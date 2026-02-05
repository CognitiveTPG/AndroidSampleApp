package com.example.cognitivetpgtestsdkapp

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cognitive.connection.ConnectionListener
import com.cognitive.connection.ConnectionManager
import com.cognitive.connection.DevType
import com.cognitive.connection.Device
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.PrinterFactory
import com.cognitive.printer.PrinterModel
import com.example.cognitivetpgtestsdkapp.databinding.ActivityPrinterSetupBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import java.io.InputStream
import java.io.OutputStream
import com.cognitive.printer.Printer
import kotlinx.coroutines.Runnable
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep

class BluetoothActivity : AppCompatActivity(), ConnectionListener {

    private var adapter: ListAdapter? = null

    private lateinit var mBinding: ActivityPrinterSetupBinding

    private var list: ArrayList<Device>? = null

    private val mBluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var socket: BluetoothSocket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPrinterSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        // Check device Bluetooth support
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Request permissions first
        checkAndRequestBluetoothPermissions()

        mBinding.printerList.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                if (mannual_connect != "") {
                    showToast("Disconnect Active Connection")
                    return
                }
                val position = arg2
                val dev = list?.get(position)
                val address = dev?.address

                if (connectedAddress.equals("", ignoreCase = true)) {
                    displayDialog { dialog, which ->
                        lifecycleScope.launch {
                            val isConnected = connect(address, which)
                            connectedAddress = if (isConnected) address
                            else ""
                        }

                    }
                } else if (address.equals(connectedAddress, ignoreCase = true)) {
                    try {
                        if (connection != null) {
                            connection?.closeConnection()
                            connection = null
                            printer = null
                            connectedAddress = ""
                            adapter?.notifyDataSetChanged()
                            return
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    showToast("Disconnect Active Connection")
                }
            }
        }

        mBinding.btnRefresh.setOnClickListener {
            findPrinters()
        }

        mBinding.fieldAddress.setText(mannual_connect)
        mBinding.btnConnect.setOnClickListener(object : View.OnClickListener {
            override fun onClick(arg0: View?) {
                if (!connectedAddress.equals("", ignoreCase = true)) {
                    showToast("Disconnect Active Connection")
                    return
                } else if (mBinding.btnConnect.text.contentEquals("Connect", ignoreCase = true)) {
                    val address = mBinding.fieldAddress.text.toString().trim { it <= ' ' }
                    if (address.isEmpty()) {
                        showToast("Enter Mac Address")
                        return
                    }

                    displayDialog { dialog, which ->
                        lifecycleScope.launch {
                            val isConnected = connect(address, which)
                            if (isConnected) {
                                mannual_connect = address
                                mBinding.btnConnect.text = "Disconnect"
                            }
                        }
                    }
                    return
                } else if (mBinding.btnConnect.text.contentEquals(
                        "Disconnect", ignoreCase = true
                    )
                ) {
                    try {
                        if (connection != null) {
                            connection?.closeConnection()
                            mBinding.btnConnect.text = "Connect"
                            connection = null
                            printer = null
                            mannual_connect = ""
                            return
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return
                }
            }
        })

        mBinding.btnRefresh.performClick()
        if (mannual_connect.equals("", ignoreCase = true)) {
            mBinding.btnConnect.text = "Connect"
        } else {
            mBinding.btnConnect.text = "Disconnect"
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (mBinding.layoutProgressBar.isGone) {
                    finish()
                }
            }
        })
    }

    private fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31 and above)
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
            } else {
                enableBluetoothIfNeeded()
            }
        } else {
            // Android 11 and below
            enableBluetoothIfNeeded()
        }
    }

    private fun enableBluetoothIfNeeded() {
        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            showToast("Bluetooth already enabled")

            findPrinters()
        }
    }

    private fun findPrinters() {
        try {
            list = ConnectionManager.searchPrinters(DevType.BLUETOOTH, this@BluetoothActivity)
            if (list != null && list?.isNotEmpty() == true) {
                adapter = ListAdapter()
                mBinding.printerList.adapter = adapter
            } else {
                showToast("No Printer Found!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (mBluetoothAdapter?.isEnabled == true) {

                showToast("Bluetooth enabled")

                findPrinters()


            } else {
                showToast("Bluetooth not enabled")
            }
        }


    private inner class ListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return list?.size ?: 0
        }

        override fun getItem(arg0: Int): Any {
            return arg0
        }

        override fun getItemId(arg0: Int): Long {
            return arg0.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var convertView = convertView
            var holder: ViewHolder
            if (convertView == null) {
                val li = layoutInflater
                convertView = li.inflate(R.layout.item_list, null)
                holder = ViewHolder()
                holder.image = convertView?.findViewById<View?>(R.id.icon) as ImageView
                holder.text = convertView.findViewById<View?>(R.id.text) as TextView
                holder.check = convertView.findViewById<View?>(R.id.check) as CheckBox
                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }

            val dev = list?.get(position)
            val name = dev?.name ?: ""
            val address = dev?.address ?: ""
            holder.text?.text = name
            holder.check?.id = position
//            holder.check?.setOnCheckedChangeListener(onCheck)

            if (connectedAddress.equals(address, ignoreCase = true)) {
                holder.image?.setImageResource(R.drawable.ic_bluetooth_active)
            } else {
                holder.image?.setImageResource(R.drawable.ic_blueooth_inactive)
            }
            return convertView
        }
    }

//    var onCheck: CompoundButton.OnCheckedChangeListener =
//        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) {
//                val position = buttonView.id
//                val dev = list?.get(position)
//                val address = dev?.address ?: ""
//
//                val handler = Handler()
//                handler.post {
//
//                    displayDialog { dialog, which ->
//                        val isConnected = connect(address, which)
//                        if (isConnected) connectedAddress = address
//                        else buttonView.isChecked = false
//                    }
//
//
//                }
//            }
//        }


    private suspend fun connect(address: String?, index: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {

                withContext(Dispatchers.Main) {
                    mBinding.layoutProgressBar.visibility = View.VISIBLE
                }

                connection = ConnectionManager.getConnection(DevType.BLUETOOTH)
                connection?.setConnectionListener(this@BluetoothActivity)
                connection?.openConnection(address, null)

                if (index == 0) {
                    printer = PrinterFactory.getPrinter(PrinterModel.DLXi) as LabelPrinter?
                } else {
                    printer = PrinterFactory.getPrinter(PrinterModel.A798) as PoSPrinter?
                }

                printer?.setConnection(connection)

                sleep(2000)

                withContext(Dispatchers.Main) {
                    mBinding.layoutProgressBar.visibility = View.GONE
                }
            }
            true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                mBinding.layoutProgressBar.visibility = View.GONE
                e.printStackTrace()
                showToast("Connection Error: " + e.message)
                connection = null
                printer = null
            }
            false
        }
    }

    private fun displayDialog(onClick: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connection Type").setItems(
            arrayOf("Label Printer", "POS Printer")
        ) { dialog, which ->
            dialog.dismiss()
            onClick.onClick(dialog, which)
        }
        builder.create().show()


    }

    internal class ViewHolder {
        var image: ImageView? = null
        var text: TextView? = null
        var check: CheckBox? = null
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConnected() {
        showToast("Connected")

        lifecycleScope.launch(Dispatchers.Main) {
            if (adapter != null) adapter?.notifyDataSetChanged()
        }
    }

    override fun onDisconnected() {
        showToast("Disconnected")
        lifecycleScope.launch(Dispatchers.Main) {
            connectedAddress = ""
            if (adapter != null) adapter?.notifyDataSetChanged()
        }
    }

    override fun onError(arg0: String?) {
        showToast("Printer Error: $arg0")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableBluetoothIfNeeded()
            } else {
                showToast("Bluetooth permissions denied")
            }
        }
    }

    companion object {
        private var connectedAddress: String? = ""

        var connection: ConnectionManager? = null
        var printer: Printer? = null

        private var mannual_connect = ""
    }
}
