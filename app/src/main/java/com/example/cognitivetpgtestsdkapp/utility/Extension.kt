package com.example.cognitivetpgtestsdkapp.utility

import android.app.Activity
import android.widget.Toast

fun Activity.showToast(msg: String?) {
    runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
}