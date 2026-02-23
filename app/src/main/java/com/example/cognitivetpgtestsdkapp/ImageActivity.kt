package com.example.cognitivetpgtestsdkapp

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.io.LabelPrinterIO
import com.cognitive.printer.io.POSPrinterIO
import com.cognitive.printer.io.PrinterIO
import com.cognitive.util.BitmapConvertor
import com.example.cognitivetpgtestsdkapp.databinding.ActivityImageBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        mBinding.apply {

            if (BluetoothActivity.printer is LabelPrinter) {
                linearLayoutLabelOptions.visibility = View.VISIBLE
            }

            printButton.setOnClickListener {
                try {

                    var buffer: PrinterIO?
                    val image: ByteArray =
                        readByteArrayFromRaw(this@ImageActivity, R.raw.test_simple)
                    val base64 = byteArrayToBase64(image)

                    if (BluetoothActivity.printer is PoSPrinter) {
                        buffer = POSPrinterIO()
                        buffer.addInitializePrinter()
                        buffer.addAlignment(POSPrinterIO.Alignment.Center)
                        buffer.addLogo(image)

                        sendToPrinter(buffer)

                        Thread.sleep(100L)

                        buffer.clearBuffer()
                        buffer.printLogo(POSPrinterIO.PrintMode.Normal)

                        sendToPrinter(buffer)
                    } else if (BluetoothActivity.printer is LabelPrinter) {

                        lifecycleScope.launch {

                            withContext(Dispatchers.Main) {
                                mBinding.rellayout.visibility = View.VISIBLE
                            }

                            var buffer = LabelPrinterIO()
                            buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 100, 600, 0)
                            buffer.addGraphic(
                                LabelPrinterIO.GraphicType.PCX,
                                if (mBinding.textX.getText().toString().trim()
                                        .isEmpty()
                                ) 0 else mBinding.textX.getText().toString().trim().toInt(),
                                if (mBinding.textY.getText().toString().trim()
                                        .isEmpty()
                                ) 0 else mBinding.textY.getText().toString().trim().toInt(),
                            )
                            sendToPrinter(buffer)

                            delay(500)

                            buffer = LabelPrinterIO()
                            buffer.addGraphicData(image)
                            sendToPrinter(buffer)

                            showToast("File Sent To Print")

                            delay(500)


                            buffer = LabelPrinterIO()
                            buffer.addHeader(LabelPrinterIO.Mode.REUSE, 0, 100, 590, 1)
                            buffer.addEnd()
                            sendToPrinter(buffer)

                            delay(1000)

                            withContext(Dispatchers.Main) {
                                mBinding.rellayout.visibility = View.GONE
                            }
                        }
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Failed to Print")
                    mBinding.rellayout.visibility = View.VISIBLE
                }
            }
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


    private fun readByteArrayFromRaw(context: Context, resId: Int): ByteArray {
        return context.resources.openRawResource(resId).use { input ->
            input.readBytes()
        }
    }

    fun byteArrayToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
