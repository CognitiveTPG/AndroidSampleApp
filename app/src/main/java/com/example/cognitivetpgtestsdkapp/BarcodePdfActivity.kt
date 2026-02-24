package com.example.cognitivetpgtestsdkapp

import android.R
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.io.LabelPrinterIO
import com.cognitive.printer.io.POSPrinterIO
import com.cognitive.printer.io.POSPrinterIO.CorrectionLevelOption
import com.cognitive.printer.io.PrinterIO
import com.example.cognitivetpgtestsdkapp.databinding.ActivityBarcodePdf417Binding
import com.example.cognitivetpgtestsdkapp.databinding.ActivityQrcodeBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BarcodePdfActivity : AppCompatActivity() {
    private var isProcessing = false


    private lateinit var mBinding: ActivityBarcodePdf417Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBarcodePdf417Binding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.apply {

            linearLayoutLabelOptions.visibility = View.VISIBLE


            printButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(arg0: View?) {
                    try {

                        if (isProcessing) {
                            return
                        }

                        var buffer: PrinterIO?

                        if (BluetoothActivity.printer is LabelPrinter) {
                            buffer = LabelPrinterIO()
                            buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 100, 600, 1)
                            buffer.addPDF417(
                                if (mBinding.textX.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textX.getText().toString().trim().toInt(),
                                if (mBinding.textY.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textY.getText().toString().trim().toInt(),
                                if (mBinding.widthBarcode.getText().toString().trim()
                                        .isEmpty()
                                ) 2 else mBinding.widthBarcode.getText().toString().trim().toInt(),
                                if (mBinding.heightBarcode.getText().toString().trim()
                                        .isEmpty()
                                ) 6 else mBinding.heightBarcode.getText().toString().trim().toInt(),
                                errorBarcodeValue,
                                if (mBinding.rowsBarcode.getText().toString().trim()
                                        .isEmpty()
                                ) 0 else mBinding.rowsBarcode.getText().toString().trim().toInt(),
                                if (mBinding.columnsBarcode.getText().toString().trim()
                                        .isEmpty()
                                ) 0 else mBinding.columnsBarcode.getText().toString().trim()
                                    .toInt(),
                                mBinding.barcodeData.text.trim().toString().toByteArray(
                                    Charsets.US_ASCII
                                ).size,
                                mBinding.barcodeData.text.trim().toString()
                            )
                            buffer.addEnd()
                            sendToPrinter(buffer)

                        }


                        isProcessing = true

                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to Print")
                    }
                    isProcessing = false
                }
            })

        }

    }

    private fun sendToPrinter(buffer: PrinterIO?) {
        if (buffer != null) {
            if (HomeActivity.MODE_CONNECTION == HomeActivity.MODE_BLUETOOTH) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        BluetoothActivity.printer?.sendCommand(buffer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to Print")

                    }
                }
            }
        }
    }

    private val errorBarcodeValue: Int
        get() {
            var error = 0
            val hgt = mBinding.errorBarcode.getText().toString().trim()
            try {
                error = hgt.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (error !in 0..8) {
                error = 1
                return error
            } else {
                return error
            }

        }

}
