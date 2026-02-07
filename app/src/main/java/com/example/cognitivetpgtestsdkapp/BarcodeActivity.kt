package com.example.cognitivetpgtestsdkapp

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
import com.cognitive.printer.io.POSPrinterIO.BarCodeType
import com.cognitive.printer.io.POSPrinterIO.BarcodeWide
import com.cognitive.printer.io.POSPrinterIO.HRI
import com.cognitive.printer.io.PrinterIO
import com.example.cognitivetpgtestsdkapp.databinding.ActivityBarcodeBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BarcodeActivity : AppCompatActivity() {
    private var isProcessing = false


    private lateinit var mBinding: ActivityBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBarcodeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.apply {

            if (BluetoothActivity.printer is PoSPrinter) {
                linearLayoutPosOptions.visibility = View.VISIBLE
            } else if (BluetoothActivity.printer is LabelPrinter) {
                mBinding.linearLayoutLabelOptions.visibility = View.VISIBLE
            }

            var ad: SpinnerAdapter = ArrayAdapter<String?>(
                this@BarcodeActivity, android.R.layout.simple_list_item_1, arrayOf<String>(
                    "UPC-A",
                    "UPC-E",
                    "JAN8",
                    "JAN13",
                    "Code39",
                    "Code93",
                    "Code128",
                    "CODABAR",
                    "CodeEAN128",
                    "ITF"
                )
            )
            selectBarcode.setAdapter(ad)

            ad = ArrayAdapter<String?>(
                this@BarcodeActivity,
                android.R.layout.simple_list_item_1,
                arrayOf<String>("Left", "Center", "Right")
            )
            selectJustify.setAdapter(ad)


            ad = ArrayAdapter<String?>(
                this@BarcodeActivity,
                android.R.layout.simple_list_item_1,
                arrayOf<String>("HRI None", "HRI Above", "HRI Below", "HRI Both")
            )
            selectHri.setAdapter(ad)

            ad = ArrayAdapter<String?>(
                this@BarcodeActivity,
                android.R.layout.simple_list_item_1,
                arrayOf<String>("Wide 2", "Wide 3", "Wide 4", "Wide 5", "Wide 6")
            )
            selectWidth.setAdapter(ad)


            ad = ArrayAdapter(
                this@BarcodeActivity, android.R.layout.simple_list_item_1, arrayOf(
                    "UPCA",
                    "UPCE",
                    "UPCE1",
                    "UPCA_PLUS",
                    "EAN8",
                    "EAN13",
                    "EAN8_PLUS",
                    "EAN13_PLUS",
                    "EAN128",
                    "ADD2",
                    "ADD5",
                    "CODE39",
                    "I2OF5",
                    "S2OF5",
                    "D2OF5",
                    "CODE128A",
                    "MSI",
                    "MSI1",
                    "CODE93",
                    "POSTNET",
                    "CODE128B",
                    "CODE128C",
                    "CODABAR",
                    "PLESSEY"
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
                            buffer.addResetPrinter()
                            buffer.addAlignment(this@BarcodeActivity.justification)
                            buffer.addBarcode(
                                this@BarcodeActivity.barcodeHeight,
                                this@BarcodeActivity.barcodeWide,
                                this@BarcodeActivity.barcodeHRI,
                                this@BarcodeActivity.barcodeType,
                                this@BarcodeActivity.barcodeData
                            )
                            buffer.addFeedLines(2)

                            sendToPrinter(buffer)


                        } else if (BluetoothActivity.printer is LabelPrinter) {
                            buffer = LabelPrinterIO()
                            buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 200, 150, 1)
                            buffer.addPitch(LabelPrinterIO.Pitch.DPI_100)
                            buffer.addBarcode(
                                this@BarcodeActivity.barcodeTypeLabel,
                                null,
                                if (mBinding.textX.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textX.getText().toString().trim().toInt(),
                                if (mBinding.textY.getText().toString().trim()
                                        .isEmpty()
                                ) 10 else mBinding.textY.getText().toString().trim().toInt(),
                                this@BarcodeActivity.barcodeHeight,
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


    private val justification: POSPrinterIO.Alignment
        get() {
            val position: Int = mBinding.selectJustify.selectedItemPosition
            if (position == 0) {
                return POSPrinterIO.Alignment.Left
            }
            if (position == 1) {
                return POSPrinterIO.Alignment.Center
            }
            if (position == 2) {
                return POSPrinterIO.Alignment.Right
            }
            return POSPrinterIO.Alignment.Center
        }

    private val barcodeType: BarCodeType
        get() {
            val position: Int = mBinding.selectBarcode.selectedItemPosition
            if (position == 0) {
                return BarCodeType.UPC_A
            }
            if (position == 1) {
                return BarCodeType.UPC_E
            }
            if (position == 2) {
                return BarCodeType.JAN8
            }
            if (position == 3) {
                return BarCodeType.JAN13
            }
            if (position == 4) {
                return BarCodeType.Code_39
            }
            if (position == 5) {
                return BarCodeType.Code_93
            }
            if (position == 6) {
                return BarCodeType.Code_128
            }
            if (position == 7) {
                return BarCodeType.CODABAR
            }
            if (position == 8) {
                return BarCodeType.Code_EAN_128
            }
            if (position == 9) {
                return BarCodeType.ITF
            }
            return BarCodeType.Code_39
        }

    private val barcodeHRI: HRI
        get() {
            val position: Int = mBinding.selectHri.selectedItemPosition
            if (position == 0) {
                return HRI.HRI_None
            }
            if (position == 1) {
                return HRI.HRI_Above
            }
            if (position == 2) {
                return HRI.HRI_Below
            }
            if (position == 3) {
                return HRI.HRI_Both
            }
            return HRI.HRI_None
        }

    private val barcodeWide: BarcodeWide
        get() {
            val position: Int = mBinding.selectWidth.selectedItemPosition
            if (position == 0) {
                return BarcodeWide.Wide_2
            }
            if (position == 1) {
                return BarcodeWide.Wide_3
            }
            if (position == 2) {
                return BarcodeWide.Wide_4
            }
            if (position == 3) {
                return BarcodeWide.Wide_5
            }
            if (position == 4) {
                return BarcodeWide.Wide_6
            }
            return BarcodeWide.Wide_2
        }

    private val barcodeHeight: Int
        get() {
            var height = 0
            val hgt = mBinding.barcodeHeight.getText().toString().trim()
            try {
                height = hgt.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (height in 1..255) {
                return height
            } else {
                height = 80
                return height
            }
        }

    private val barcodeData: ByteArray
        get() = mBinding.barcodeData.getText().toString().trim().toByteArray()


    private val barcodeTypeLabel: LabelPrinterIO.BarcodeType
        get() {
            return when (mBinding.selectBarcode.selectedItemPosition) {
                0 -> LabelPrinterIO.BarcodeType.UPCA
                1 -> LabelPrinterIO.BarcodeType.UPCE
                2 -> LabelPrinterIO.BarcodeType.UPCE1
                3 -> LabelPrinterIO.BarcodeType.UPCA_PLUS
                4 -> LabelPrinterIO.BarcodeType.EAN8
                5 -> LabelPrinterIO.BarcodeType.EAN13
                6 -> LabelPrinterIO.BarcodeType.EAN8_PLUS
                7 -> LabelPrinterIO.BarcodeType.EAN13_PLUS
                8 -> LabelPrinterIO.BarcodeType.EAN128
                9 -> LabelPrinterIO.BarcodeType.ADD2
                10 -> LabelPrinterIO.BarcodeType.ADD5
                11 -> LabelPrinterIO.BarcodeType.CODE39
                12 -> LabelPrinterIO.BarcodeType.I2OF5
                13 -> LabelPrinterIO.BarcodeType.S2OF5
                14 -> LabelPrinterIO.BarcodeType.D2OF5
                15 -> LabelPrinterIO.BarcodeType.CODE128A
                16 -> LabelPrinterIO.BarcodeType.MSI
                17 -> LabelPrinterIO.BarcodeType.MSI1
                18 -> LabelPrinterIO.BarcodeType.CODE93
                19 -> LabelPrinterIO.BarcodeType.POSTNET
                20 -> LabelPrinterIO.BarcodeType.CODE128B
                21 -> LabelPrinterIO.BarcodeType.CODE128C
                22 -> LabelPrinterIO.BarcodeType.CODABAR
                23 -> LabelPrinterIO.BarcodeType.PLESSEY
                else -> LabelPrinterIO.BarcodeType.EAN13
            }
        }

}
