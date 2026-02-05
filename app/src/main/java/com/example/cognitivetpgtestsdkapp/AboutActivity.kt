package com.example.cognitivetpgtestsdkapp

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cognitivetpgtestsdkapp.databinding.ActivityAboutBinding
import com.example.cognitivetpgtestsdkapp.databinding.ActivityHomeBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }
}
