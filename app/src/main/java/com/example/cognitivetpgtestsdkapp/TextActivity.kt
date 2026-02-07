package com.example.cognitivetpgtestsdkapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.io.LabelPrinterIO
import com.cognitive.printer.io.POSPrinterIO
import com.cognitive.printer.io.PrinterIO
import com.example.cognitivetpgtestsdkapp.databinding.ActivityTextBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TextActivity : AppCompatActivity() {
    private var italics = false
    private var upsidedown = false
    private var reverse = false
    private var clock = false
    private var anticlock = false
    private var DIRECTION: Int = NONE

    private lateinit var mBinding: ActivityTextBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityTextBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        if (BluetoothActivity.printer is PoSPrinter) {
            mBinding.linearLayoutPosOptions.visibility = View.VISIBLE
        } else if (BluetoothActivity.printer is LabelPrinter) {
            mBinding.linearLayoutLabelOptions.visibility = View.VISIBLE
        }


        mBinding.selectItalics.apply {
            setOnClickListener {
                if (italics) {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.italics_off, 0, 0
                    )
                    italics = false
                } else {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.italics_on, 0, 0
                    )
                    italics = true
                }
            }
        }



        mBinding.selectUpdown.apply {
            setOnClickListener {
                if (upsidedown) {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.circle_off, 0, 0
                    )
                    upsidedown = false
                } else {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.circle_on, 0, 0
                    )
                    upsidedown = true
                }
            }
        }

        mBinding.selectReverse.apply {
            setOnClickListener {
                if (reverse) {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.inverse_off, 0, 0
                    )
                    reverse = false
                } else {
                    setCompoundDrawablesWithIntrinsicBounds(
                        0, R.drawable.inverse_on, 0, 0
                    )
                    reverse = true
                }
            }
        }

        mBinding.selectClock.apply {
            setOnClickListener {
                when (DIRECTION) {
                    NONE -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.clock_on, 0, 0
                        )
                        clock = true
                        DIRECTION = CLOCK_WISE
                    }

                    CLOCK_WISE -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.clock_off, 0, 0
                        )
                        clock = false
                        DIRECTION = NONE
                    }

                    else -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.clock_on, 0, 0
                        )
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.anticlock_off, 0, 0
                        )
                        clock = true
                        anticlock = false
                        DIRECTION = CLOCK_WISE
                    }
                }
            }
        }

        mBinding.selectCounter.apply {
            setOnClickListener {
                when (DIRECTION) {
                    NONE -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.anticlock_on, 0, 0
                        )
                        anticlock = true
                        DIRECTION = ANTI_CLOCKWISE
                    }

                    ANTI_CLOCKWISE -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.anticlock_off, 0, 0
                        )
                        anticlock = false
                        DIRECTION = NONE
                    }

                    else -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.anticlock_on, 0, 0
                        )
                        setCompoundDrawablesWithIntrinsicBounds(
                            0, R.drawable.clock_off, 0, 0
                        )
                        anticlock = true
                        clock = false
                        DIRECTION = ANTI_CLOCKWISE
                    }
                }
            }
        }

        mBinding.spinnerFont.apply {
            adapter = ArrayAdapter(
                this@TextActivity,
                android.R.layout.simple_list_item_1,
                arrayOf("Size 6", "Size 8", "Size 10", "Size 16", "Size 24", "Size 36", "Size 48")
            )
        }

        mBinding.spinnerRotation.apply {
            adapter = ArrayAdapter(
                this@TextActivity,
                android.R.layout.simple_list_item_1,
                arrayOf("R0", "R90", "R180", "R270")
            )
        }

        mBinding.spinnerAlignment.apply {
            adapter = ArrayAdapter(
                this@TextActivity,
                android.R.layout.simple_list_item_1,
                arrayOf("LEFT", "CENTER", "RIGHT")
            )
        }

        mBinding.spinnerDPI.apply {
            adapter = ArrayAdapter(
                this@TextActivity,
                android.R.layout.simple_list_item_1,
                arrayOf("DPI 100", "DPI 200")
            )
        }


        mBinding.printButton.apply {
            setOnClickListener {

                try {
                    var buffer: PrinterIO?

                    if (BluetoothActivity.printer is PoSPrinter) {
                        buffer = POSPrinterIO()

                        buffer.addInitializePrinter()
                        buffer.addAlignment(POSPrinterIO.Alignment.Center)
                        buffer.addTextItalic(italics)
                        buffer.addTextUpsideDown(upsidedown)
                        buffer.addTextInvertColor(reverse)
                        buffer.addTextRotation(clock)
                        if (anticlock) buffer.addTextCounterClockwise()

                        val data: ByteArray =
                            (mBinding.textData.getText().toString() + "\r\n").toByteArray()
                        buffer.addTextData(data)

                        sendToPrinter(buffer)


                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        buffer = LabelPrinterIO()
                        buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 200, 150, 1)
                        buffer.addPitch(getPitch())
//                        buffer.addJustify(getPrintAlignment())
                        buffer.addText(
                            getFontSize(),
                            if (mBinding.textSpacing.getText().toString().trim()
                                    .isEmpty()
                            ) 0 else mBinding.textSpacing.getText().toString().trim().toInt(),
                            getPrintRotation(),
                            if (mBinding.textWidth.getText().toString().trim()
                                    .isEmpty()
                            ) 0 else mBinding.textWidth.getText().toString().trim().toInt(),
                            if (mBinding.textHeight.getText().toString().trim()
                                    .isEmpty()
                            ) 0 else mBinding.textHeight.getText().toString().trim().toInt(),
                            if (mBinding.textX.getText().toString().trim()
                                    .isEmpty()
                            ) 0 else mBinding.textX.getText().toString().trim().toInt(),
                            if (mBinding.textY.getText().toString().trim()
                                    .isEmpty()
                            ) 0 else mBinding.textY.getText().toString().trim().toInt(),
                            mBinding.textData.getText().toString()
                        )
                        buffer.addEnd()

                        sendToPrinter(buffer)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Failed to Print")
                }
            }
        }
    }

    private fun getFontSize(): LabelPrinterIO.FontID {
        val position: Int = mBinding.spinnerFont.selectedItemPosition

        return when (position) {
            0 -> LabelPrinterIO.FontID.Size_6
            1 -> LabelPrinterIO.FontID.Size_8
            2 -> LabelPrinterIO.FontID.Size_10
            3 -> LabelPrinterIO.FontID.Size_16
            4 -> LabelPrinterIO.FontID.Size_24
            5 -> LabelPrinterIO.FontID.Size_36
            6 -> LabelPrinterIO.FontID.Size_48
            else -> LabelPrinterIO.FontID.Size_6

        }
    }

    private fun getPitch(): LabelPrinterIO.Pitch {
        val position: Int = mBinding.spinnerDPI.selectedItemPosition

        return when (position) {
            0 -> LabelPrinterIO.Pitch.DPI_100
            1 -> LabelPrinterIO.Pitch.DPI_200
            else -> LabelPrinterIO.Pitch.DPI_100

        }
    }

    private fun getPrintRotation(): LabelPrinterIO.Rotation {
        val position: Int = mBinding.spinnerRotation.selectedItemPosition

        return when (position) {
            0 -> LabelPrinterIO.Rotation.R0
            1 -> LabelPrinterIO.Rotation.R90
            2 -> LabelPrinterIO.Rotation.R180
            3 -> LabelPrinterIO.Rotation.R270

            else -> LabelPrinterIO.Rotation.R0

        }
    }

    private fun getPrintAlignment(): LabelPrinterIO.Alignment {
        val position: Int = mBinding.spinnerAlignment.selectedItemPosition

        return when (position) {
            0 -> LabelPrinterIO.Alignment.Left
            1 -> LabelPrinterIO.Alignment.Center
            2 -> LabelPrinterIO.Alignment.Right

            else -> LabelPrinterIO.Alignment.Left

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


    companion object {
        private const val NONE = 0
        private const val CLOCK_WISE = 1
        private const val ANTI_CLOCKWISE = -1

    }
}
