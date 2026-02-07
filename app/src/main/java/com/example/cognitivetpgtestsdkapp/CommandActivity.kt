package com.example.cognitivetpgtestsdkapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.example.cognitivetpgtestsdkapp.databinding.ActivityCommandBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CommandActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityCommandBinding

    private var isProcess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityCommandBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.apply {

            mBinding.commandData.hint =
                if (BluetoothActivity.printer is PoSPrinter) "Enter Printer Command (Hex)" else "Enter Printer Command (ASCII)"

            sendButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(arg0: View?) {
                    if (mBinding.commandData.text.trim().toString().isEmpty()) {
                        showToast("Command cannot be empty")
                        return
                    }

                    if (isProcess) {
                        return
                    }
                    isProcess = true

                    var cmd = ByteArray(0)

                    if (BluetoothActivity.printer is PoSPrinter) {
                        val hex = commandData.getText().toString().trim()
                        if (!isHexCommand(hex)) {
                            showToast("Enter Hex Command")
                            isProcess = false
                            return
                        }

                        val command = hex.replace(" ", "")

                        if (command.isEmpty()) {
                            showToast("Enter Printer Command.")
                            isProcess = false
                            return
                        }

                        cmd = hexStringToByteArray(command)

                    } else if (BluetoothActivity.printer is LabelPrinter) {
                        cmd = "${commandData.getText().toString().trim()}\r\n".toByteArray()
                    }


                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            if (HomeActivity.MODE_CONNECTION == HomeActivity.MODE_BLUETOOTH) {

                                showToast("Command Send")

                                var data = cmd
                                BluetoothActivity.connection?.writeData(data, 0, data.size)
                                Thread.sleep(200L)

                                data = ByteArray(1024)
                                val size: Int? =
                                    BluetoothActivity.connection?.readData(data, 0, data.size)
                                showToast(String(data, 0, size ?: 0))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast("Failed to Send")
                        }
                        isProcess = false
                    }


                }
            })
        }


    }


    private fun isHexCommand(cmd: String): Boolean {
        val pattern = Pattern.compile("([0-9A-Fa-f]{2}){0,31}([0-9A-Fa-f]{2})")
        val match = pattern.matcher(cmd)
        return match.matches()
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace("\\s".toRegex(), "")
        require(cleanHex.length % 2 == 0) { "Invalid hex string length" }

        return ByteArray(cleanHex.length / 2) { i ->
            val index = i * 2
            cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
    }
}
