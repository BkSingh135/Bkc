package com.bkc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextClock
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView

class FullScreenActivity : BaseActivity() {

    var textClock : TextClock?= null
    var tv : AppCompatTextView?=  null
    var iv : AppCompatImageView?= null
    var title : String ?= null
    var image : Int ?= null
    var bundle : Bundle ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)
        removeStatusBarWithWhiteIcon()
        initViews()
    }
    /**
     * Initializing InitViews
     * */
    private fun initViews() {
        textClock = findViewById(R.id.textClock)
        tv = findViewById(R.id.tv)
        iv = findViewById(R.id.iv)
        bundle = intent.extras
        title = bundle?.getString("text")
        image = bundle?.getInt("image")
        tv!!.text = title
        iv!!.setImageResource(image!!)
        tv?.isSelected = true
    }
}