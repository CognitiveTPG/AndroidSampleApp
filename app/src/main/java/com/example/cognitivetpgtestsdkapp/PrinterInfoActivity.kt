package com.example.cognitivetpgtestsdkapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.example.cognitivetpgtestsdkapp.databinding.ActivityMainBinding
import com.example.cognitivetpgtestsdkapp.databinding.ActivityPrinterInfoBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrinterInfoActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityPrinterInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPrinterInfoBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        getPrinterDetails()
    }

    private fun getPrinterDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var data: String?

                if (HomeActivity.MODE_CONNECTION == HomeActivity.MODE_BLUETOOTH) {

                    data = "Printer Model" + "\r\n"
                    if (BluetoothActivity.printer is PoSPrinter) {
                        data += ((BluetoothActivity.printer as PoSPrinter).printerModel ?: "")
                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        data += ((BluetoothActivity.printer as LabelPrinter).modelNumber ?: "")
                    }
                    showInfo(mBinding.model, data)

                    data = "Serial Number" + "\r\n"
                    if (BluetoothActivity.printer is PoSPrinter) {
                        data += ((BluetoothActivity.printer as PoSPrinter).serialNumber ?: "")
                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        data += ((BluetoothActivity.printer as LabelPrinter).serialNumber ?: "")
                    }
                    showInfo(mBinding.serialNumber, data)

                    data = "Software Version" + "\r\n"
                    if (BluetoothActivity.printer is PoSPrinter) {
                        data += ((BluetoothActivity.printer as PoSPrinter).softwareVersion
                            ?: "")
                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        data += ""
                    }
                    showInfo(mBinding.softwareVersion, data)

                    data = "Loader Version" + "\r\n"
                    if (BluetoothActivity.printer is PoSPrinter) {
                        data += ((BluetoothActivity.printer as PoSPrinter).bootFirmwareVersion
                            ?: "")
                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        data += ""
                    }
                    showInfo(mBinding.bootVersion, data)

                    data = "Flash Version" + "\r\n"
                    if (BluetoothActivity.printer is PoSPrinter) {
                        data += ((BluetoothActivity.printer as PoSPrinter).flashFirmwareVersion
                            ?: "")
                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        data += ""
                    }
                    showInfo(mBinding.flashVersion, data)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Communication Error")
            }
        }
    }


    private suspend fun showInfo(view: TextView, data: String?) {
        withContext(Dispatchers.Main) {
            view.text = data
        }
    }
}
