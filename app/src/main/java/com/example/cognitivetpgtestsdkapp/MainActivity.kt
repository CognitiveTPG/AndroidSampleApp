package com.example.cognitivetpgtestsdkapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.io.LabelPrinterIO
import com.cognitive.printer.io.POSPrinterIO
import com.cognitive.printer.io.POSPrinterIO.BarCodeType
import com.cognitive.printer.io.POSPrinterIO.BarcodeWide
import com.cognitive.printer.io.POSPrinterIO.CorrectionLevelOption
import com.cognitive.printer.io.POSPrinterIO.CutType
import com.cognitive.printer.io.POSPrinterIO.HRI
import com.cognitive.printer.io.PrinterIO
import com.cognitive.status.PrinterStatus
import com.example.cognitivetpgtestsdkapp.databinding.ActivityMainBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val label = arrayOf<String?>(
        "Text",
        "Image",
        "Barcode",
        "QR Code",
        "PDF",
        "Print Sample",
        "Raw I/O",
        "Printer Info",
        "File Print"
    )
    private val icon = intArrayOf(
        R.drawable.icon_text,
        R.drawable.icon_image,
        R.drawable.icon_barcode,
        R.drawable.icon_qrcode,
        R.drawable.icon_pdf417,
        R.drawable.icon_receipt,
        R.drawable.icon_raw,
        R.drawable.icon_info,
        R.drawable.icon_file
    )

    private var adapter: GridAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.rellayout.visibility = View.GONE

        adapter = GridAdapter()
        mBinding.actionList.setAdapter(adapter)
        mBinding.actionList.onItemClickListener =
            OnItemClickListener { arg0, arg1, arg2, arg3 ->


                var intent: Intent? = null
                when (arg2) {
                    0 -> {
                        intent = Intent(this@MainActivity, TextActivity::class.java)
                        startActivity(intent)
                    }

                    1 -> {
//                        intent = Intent(this@MainActivity, ImageActivity::class.java)
//                        startActivity(intent)
                    }

                    2 -> {
                        intent = Intent(this@MainActivity, BarcodeActivity::class.java)
                        startActivity(intent)
                    }

                    3 -> printQRCode()

                    4 -> printPDF417()

                    5 -> {
//                        intent = Intent(this@MainActivity, ReceiptActivity::class.java)
//                        startActivity(intent)
                    }

                    6 -> {
//                        intent = Intent(this@MainActivity, CommandActivity::class.java)
//                        startActivity(intent)
                    }

                    7 -> {
                        intent = Intent(this@MainActivity, PrinterInfoActivity::class.java)
                        startActivity(intent)
                    }

                    8 -> {
//                        intent = Intent(this@MainActivity, FileActivity::class.java)
//                        startActivity(intent)
                    }
                }
            }


        mBinding.status.setOnClickListener {

            lifecycleScope.launch {
                try {
                    var ps: PrinterStatus?
                    if (HomeActivity.MODE_CONNECTION == HomeActivity.MODE_BLUETOOTH) {

                        if (BluetoothActivity.printer is PoSPrinter) {
                            ps = (BluetoothActivity.printer as PoSPrinter).status

                            withContext(Dispatchers.Main) {
                                if (ps?.isPaperLow ?: false) {
                                    showToast("Receipt Paper: Low")
                                }
                                if (ps?.isCoverOpen ?: false) {
                                    showToast("Receipt Cover: Open")
                                }
                                if (!(ps?.isKnifeHomePosition ?: false)) {
                                    showToast("Knife Position: Not Home Position")
                                }
                                if (!(ps?.isPaperPresent ?: false)) {
                                    showToast("Receipt Paper: Out")
                                }
                                if (ps?.isTemperatureOK ?: false) {
                                    showToast("Temperature: In Valid Range")
                                }
                                if (ps?.isVoltageOK ?: false) {
                                    showToast("Voltage: In Valid Range")
                                }
                            }
                        } else if (BluetoothActivity.printer is LabelPrinter) {

                            withContext(Dispatchers.Main) {
                                showToast("Status is: ${(BluetoothActivity.printer as LabelPrinter).printerStatus.statusMessage}")
                            }
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        e.printStackTrace()
                        showToast("Printer Error ${e.message}")
                    }
                }
            }
        }


        mBinding.tone.setOnClickListener {

            try {

                var buffer: PrinterIO? = null

                if (BluetoothActivity.printer is PoSPrinter) {
                    buffer = POSPrinterIO()
                    buffer.addResetPrinter()
                    buffer.addTone()


                } else if (BluetoothActivity.printer is LabelPrinter) {
                    buffer = LabelPrinterIO()
                    buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 0, 0, 0);
                    buffer.addBeep(2);
                    buffer.addEnd();
                }

                showToast("Beep sent for 2 second")

                sendToPrinter(buffer)


            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to Beep")
            }
        }

        mBinding.testPrint.setOnClickListener {

            try {

                var buffer: PrinterIO? = null

                if (BluetoothActivity.printer is PoSPrinter) {
                    buffer = POSPrinterIO()
                    buffer.addInitializePrinter()
                    buffer.addTestForm()


                } else if (BluetoothActivity.printer is LabelPrinter) {
                    buffer = LabelPrinterIO()
                    buffer.addPrintTestLabel()
                }


                sendToPrinter(buffer)


            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to Print")
            }
        }

        mBinding.fullCut.setOnClickListener {

            try {
                var buffer: PrinterIO?

                if (BluetoothActivity.printer is PoSPrinter) {
                    buffer = POSPrinterIO()
                    buffer.addInitializePrinter()
                    buffer.addFeedLines(50)
                    buffer.addCut(CutType.FULL_CUT)
                    sendToPrinter(buffer)


                } else if (BluetoothActivity.printer is LabelPrinter) {
                    showToast("Full Cut only supported in POS printers")
                }


            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to Cut")
            }
        }

        mBinding.partialCut.setOnClickListener {

            try {
                var buffer: PrinterIO?

                if (BluetoothActivity.printer is PoSPrinter) {
                    buffer = POSPrinterIO()
                    buffer.addInitializePrinter()
                    buffer.addFeedLines(50)
                    buffer.addCut(CutType.PARTIAL_CUT)
                    sendToPrinter(buffer)


                } else if (BluetoothActivity.printer is LabelPrinter) {
                    showToast("Partial Cut only supported in POS printers")
                }


            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to Print")
            }
        }
    }

    private fun printQRCode() {
        try {
            var buffer: PrinterIO?

            if (BluetoothActivity.printer is PoSPrinter) {
                buffer = POSPrinterIO()
                buffer.addInitializePrinter()
                buffer.addAlignment(POSPrinterIO.Alignment.Center)
                buffer.addQRCode(
                    POSPrinterIO.Model.Model2,
                    5,
                    CorrectionLevelOption.Low,
                    "QR Code by CognitiveTPG".toByteArray()
                )
                val cmd = arrayOf<Byte?>(0x0D, 0x0A)
                buffer.addCommand(cmd)
                buffer.addData("For more information please visit us at:".toByteArray())
                buffer.addCommand(cmd)
                buffer.addData("http://www.cognitivetpg.com/gettheinsidestory".toByteArray())
                buffer.printQRCode()
                buffer.addFeedLine()
                buffer.addFeedLine()

                sendToPrinter(buffer)


            } else if (BluetoothActivity.printer is LabelPrinter) {
                showToast("QR code only supported in POS printers")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printPDF417() {
        showToast("Feature under implementation")
    }

    private fun sendToPrinter(buffer: PrinterIO?) {
        if (buffer != null) {
            if (HomeActivity.MODE_CONNECTION == HomeActivity.MODE_BLUETOOTH) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) {
                            mBinding.rellayout.visibility = View.VISIBLE
                        }

                        BluetoothActivity.printer?.sendCommand(buffer)

                        Thread.sleep(3000)

                        withContext(Dispatchers.Main) {
                            mBinding.rellayout.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to Print")
                        withContext(Dispatchers.Main) {
                            mBinding.rellayout.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private inner class GridAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return label.size
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
                holder = ViewHolder()
                val li = layoutInflater
                convertView = li.inflate(R.layout.item_grid, null)
                holder = ViewHolder()
                holder.imageView = convertView?.findViewById<View?>(R.id.icon) as ImageView
                holder.textView = convertView.findViewById<View?>(R.id.label) as TextView
                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }
            holder.imageView?.setImageResource(icon[position])
            holder.textView?.text = label[position]
            return convertView
        }
    }

    internal class ViewHolder {
        var imageView: ImageView? = null
        var textView: TextView? = null
    }
}
