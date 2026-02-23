package com.example.cognitivetpgtestsdkapp

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cognitive.printer.LabelPrinter
import com.cognitive.printer.PoSPrinter
import com.cognitive.printer.io.LabelPrinterIO
import com.cognitive.printer.io.POSPrinterIO
import com.cognitive.printer.io.PrinterIO
import com.example.cognitivetpgtestsdkapp.databinding.ActivityFilePrintingBinding
import com.example.cognitivetpgtestsdkapp.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

class FileActivity : AppCompatActivity() {
    private var barProgressDialog: ProgressDialog? = null

    private var selectedUri: Uri? = null
    private lateinit var mBinding: ActivityFilePrintingBinding

    private var mStringFileType = ""


    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { handleSelectedFile(it) }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityFilePrintingBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.apply {
            btnSelectFile.setOnClickListener {
                openFilePicker()
            }

            btnClose.setOnClickListener {
                clearSelection()
            }

            printButton.setOnClickListener {
                sendFile()
            }
        }


    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    private fun handleSelectedFile(uri: Uri) {
        val fileName = getFileName(uri) ?: return

        if (!isValidFile(fileName)) {
            showToast("Invalid file type!")
            return
        }

        mStringFileType = getFileType(fileName)

        selectedUri = uri

        // Persist permission (important)
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        showSelectedUI(fileName)

        val fileBytes = uriToByteArray(uri)

        if (fileBytes != null) {
            showToast("File size: ${fileBytes.size} bytes")
        } else {
            showToast("Failed to read file")
        }
    }

    private fun isValidFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".bin") || lower.endsWith(".bmp") || lower.endsWith(".pcx")
    }

    private fun getFileType(name: String): String {
        val lower = name.lowercase()

        if (lower.endsWith(".bin")) {
            return "bin"
        } else if (lower.endsWith(".bmp")) {
            return "bmp"
        } else if (lower.endsWith(".pcx")) {
            return "pcx"
        }
        return ""
    }

    private fun getFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }

    private fun showSelectedUI(fileName: String) {
        mBinding.apply {
            btnSelectFile.visibility = View.GONE
            layoutSelected.visibility = View.VISIBLE
            txtFileName.text = fileName
            printButton.visibility = View.VISIBLE
        }

    }

    private fun clearSelection() {
        mBinding.apply {
            selectedUri = null
            layoutSelected.visibility = View.GONE
            btnSelectFile.visibility = View.VISIBLE
            printButton.visibility = View.GONE
        }

    }

    private fun showProgress() {
        mBinding.apply {
            barProgressDialog = ProgressDialog.show(this@FileActivity, "File Browser", "Loading...")
            barProgressDialog?.setCancelable(false)
        }

    }

    private fun hideProgress() {
        barProgressDialog?.dismiss()
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun sendFile() {
        try {
            var buffer: PrinterIO?
            val bytes = selectedUri?.let { uriToByteArray(it) }



            if (BluetoothActivity.printer is PoSPrinter) {

                if (mStringFileType == "bin") {
                    buffer = POSPrinterIO()
                    buffer.addResetPrinter()
                    buffer.addBinaryData(bytes)
                    sendToPrinter(buffer)
                } else if (mStringFileType == "pcx" || mStringFileType == "bmp") {
                    buffer = POSPrinterIO()
                    buffer.addInitializePrinter()
                    buffer.addAlignment(POSPrinterIO.Alignment.Center)
                    buffer.addLogo(bytes)

                    sendToPrinter(buffer)

                    Thread.sleep(100L)

                    buffer.clearBuffer()
                    buffer.printLogo(POSPrinterIO.PrintMode.Normal)
                    sendToPrinter(buffer)
                }


            } else if (BluetoothActivity.printer is LabelPrinter) {

                lifecycleScope.launch {

                    withContext(Dispatchers.Main) {
                        showProgress()
                    }

                    var buffer = LabelPrinterIO()

                    if (mStringFileType == "bin") {
                        buffer.addBinaryData(bytes)
                        sendToPrinter(buffer)
                    } else {
                        buffer.addHeader(LabelPrinterIO.Mode.ASCII, 0, 100, 600, 0)
                        buffer.addGraphic(
                            if (mStringFileType == "pcx") LabelPrinterIO.GraphicType.PCX else LabelPrinterIO.GraphicType.BMP,
                            0,
                            0,
                        )
                        sendToPrinter(buffer)

                        delay(500)

                        buffer = LabelPrinterIO()
                        buffer.addGraphicData(bytes)
                        sendToPrinter(buffer)

                        delay(500)


                        buffer = LabelPrinterIO()
                        buffer.addHeader(LabelPrinterIO.Mode.REUSE, 0, 100, 590, 1)
                        buffer.addEnd()
                        sendToPrinter(buffer)
                    }


                    delay(1000)

                    withContext(Dispatchers.Main) {
                        hideProgress()
                    }
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to Print")
            hideProgress()
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

}
