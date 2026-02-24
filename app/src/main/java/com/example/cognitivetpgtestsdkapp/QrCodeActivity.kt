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
import com.example.cognitivetpgtestsdkapp.databinding.ActivityQrcodeBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class QrCodeActivity : AppCompatActivity() {
    private var isProcessing = false


    private lateinit var mBinding: ActivityQrcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.apply {

            if (BluetoothActivity.printer is PoSPrinter) {
                linearLayoutPosOptions.visibility = View.VISIBLE
            } else if (BluetoothActivity.printer is LabelPrinter) {
                linearLayoutLabelOptions.visibility = View.VISIBLE
            }

            var ad: SpinnerAdapter = ArrayAdapter<String?>(
                this@QrCodeActivity, R.layout.simple_list_item_1, arrayOf<String>(
                    "Model1",
                    "Model2",
                )
            )
            selectQrcodeModel.setAdapter(ad)

            ad = ArrayAdapter<Int>(
                this@QrCodeActivity,
                R.layout.simple_list_item_1,
                arrayOf<Int>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
            selectSizeQrCode.setAdapter(ad)


            ad = ArrayAdapter<String?>(
                this@QrCodeActivity,
                R.layout.simple_list_item_1,
                arrayOf<String>("Low", "Middle", "Q", "High")
            )
            selectQrCodeOption.setAdapter(ad)




            ad = ArrayAdapter(
                this@QrCodeActivity, R.layout.simple_list_item_1, arrayOf(
                    1,
                    2,
                )
            )
            mBinding.selectBarcodeLabel.adapter = ad


            printButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(arg0: View?) {
                    try {

                        if (isProcessing) {
                            return
                        }

                        var buffer: PrinterIO?

                        if (BluetoothActivity.printer is PoSPrinter) {
                            buffer = POSPrinterIO()
                            buffer.addInitializePrinter()
                            buffer.addAlignment(POSPrinterIO.Alignment.Center)
                            buffer.addQRCode(
                                selectQrcodeModelValue,
                                selectSizeQrCodeValue,
                                selectQrCodeOptionValue,
                                mBinding.barcodeData.text.trim().toString().toByteArray()
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
                            buffer = LabelPrinterIO()
                            buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 100, 250, 1)
                            buffer.addQRCode(
                                if (mBinding.textX.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textX.getText().toString().trim().toInt(),
                                if (mBinding.textY.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textY.getText().toString().trim().toInt(),
                                this@QrCodeActivity.barcodeCellSize,
                                this@QrCodeActivity.barcodeTypeLabel,
                                mBinding.barcodeData.text.trim().toString()
                            )
                            buffer.addIndex()
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


    private val selectQrcodeModelValue: POSPrinterIO.Model
        get() {
            val position: Int = mBinding.selectQrcodeModel.selectedItemPosition
            if (position == 0) {
                return POSPrinterIO.Model.Model1
            }
            if (position == 1) {
                return POSPrinterIO.Model.Model2
            }

            return POSPrinterIO.Model.Model1
        }

    private val selectSizeQrCodeValue: Int
        get() {
            val position: Int = mBinding.selectSizeQrCode.selectedItemPosition
            if (position == 0) {
                return 1
            }
            if (position == 1) {
                return 2
            }
            if (position == 2) {
                return 3
            }
            if (position == 3) {
                return 4
            }
            if (position == 4) {
                return 5
            }
            if (position == 5) {
                return 6
            }
            if (position == 6) {
                return 7
            }
            if (position == 7) {
                return 8
            }
            if (position == 8) {
                return 9
            }
            if (position == 9) {
                return 10
            }
            return 1
        }

    private val selectQrCodeOptionValue: CorrectionLevelOption
        get() {
            val position: Int = mBinding.selectQrCodeOption.selectedItemPosition
            if (position == 0) {
                return CorrectionLevelOption.Low
            }
            if (position == 1) {
                return CorrectionLevelOption.Middle
            }
            if (position == 2) {
                return CorrectionLevelOption.Q
            }
            if (position == 3) {
                return CorrectionLevelOption.High
            }
            return CorrectionLevelOption.Low
        }


    private val barcodeCellSize: Int
        get() {
            var height = 0
            val hgt = mBinding.barcodeCellSize.getText().toString().trim()
            try {
                height = hgt.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (height <= 0) {
                height = 3
                return height
            } else {
                return height
            }

        }

    private val barcodeData: ByteArray
        get() = mBinding.barcodeData.getText().toString().trim().toByteArray()


    private val barcodeTypeLabel: Int
        get() {
            return when (mBinding.selectBarcodeLabel.selectedItemPosition) {
                0 -> 1
                1 -> 2
                else -> 1
            }
        }

}
