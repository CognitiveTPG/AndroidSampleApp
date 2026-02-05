package com.example.cognitivetpgtestsdkapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cognitivetpgtestsdkapp.databinding.ActivityHomeBinding
import kotlin.jvm.java


class HomeActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        mBinding.menu1.setOnClickListener {
            if (MODE_CONNECTION == MODE_WIFI) {
//                if (WiFiActivity.connection != null) {
//                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
//                    startActivity(intent)
//                } else {
//                    showToast("No Printer Connected!")
//                }
            } else if (MODE_CONNECTION == MODE_BLUETOOTH) {
                if (BluetoothActivity.connection != null) {
                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    showToast("No Printer Connected!")
                }
            } else {
                showToast("No Printer Connected!")
            }
        }

        mBinding.menu2.setOnClickListener {
//            if (WiFiActivity.isConnectedToWifi) {
//                MODE_CONNECTION = MODE_WIFI
//                val intent: Intent = Intent(this@HomeActivity, WiFiActivity::class.java)
//                startActivity(intent)
//            } else showDialog()

//            showDialog()

            MODE_CONNECTION = MODE_BLUETOOTH
            val intent: Intent =
                Intent(this@HomeActivity, BluetoothActivity::class.java)
            startActivity(intent)
        }

        mBinding.menu3.setOnClickListener {
            val intent: Intent = Intent(this@HomeActivity, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(msg: String?) {
        this.runOnUiThread { Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_LONG).show() }
    }

    fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connection Type")
            .setItems(
                arrayOf<String>("WiFi", "Bluetooth")
            ) { dialog, which ->
                dialog.dismiss()
                if (which == 0) {
                    MODE_CONNECTION = MODE_WIFI
//                    val intent: Intent = Intent(this@HomeActivity, WiFiActivity::class.java)
//                    startActivity(intent)
                } else if (which == 1) {
                    MODE_CONNECTION = MODE_BLUETOOTH
                    val intent: Intent =
                        Intent(this@HomeActivity, BluetoothActivity::class.java)
                    startActivity(intent)
                }
            }
        builder.create().show()
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        var MODE_CONNECTION: Int = 0
        var MODE_WIFI: Int = 1
        var MODE_BLUETOOTH: Int = 2
    }
}
