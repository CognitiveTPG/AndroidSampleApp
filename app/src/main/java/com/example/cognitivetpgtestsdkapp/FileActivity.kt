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

            btnBack.setOnClickListener {
                finish()
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
        return lower.endsWith(".bin") || lower.endsWith(".txt")
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
            printerResponse.text = ""
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
                buffer = POSPrinterIO()
                buffer.addResetPrinter()
                buffer.addBinaryData(bytes)
                sendToPrinter(buffer)


            } else if (BluetoothActivity.printer is LabelPrinter) {

                lifecycleScope.launch {

                    withContext(Dispatchers.Main) {
                        showProgress()
                    }

                    var data = bytes
                    BluetoothActivity.connection?.writeData(data, 0, data?.size ?: 0)
                    delay(2000)

                    data = ByteArray(1024)
                    val size: Int? =
                        BluetoothActivity.connection?.readData(data, 0, data.size)

                    withContext(Dispatchers.Main) {
                        mBinding.printerResponse.text = String(data, 0, size ?: 0)
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
