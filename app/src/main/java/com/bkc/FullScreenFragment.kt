package com.bkc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextClock
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView

class FullScreenFragment : Fragment() {

    private var textClock : TextClock?= null
    private var tv : AppCompatTextView?=  null
    private var iv : AppCompatImageView?= null
    private var title : String ?= null
    private var image : Int ?= null
    private var bundle : Bundle ?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view : View =  inflater.inflate(R.layout.fragment_full_screen, container, false)
        initViews(view)
        return  view
    }

    /**
     * initializing initViews
     * */
    private fun initViews(view: View) {
        textClock = view.findViewById(R.id.textClock)
        tv = view.findViewById(R.id.tv)
        iv = view.findViewById(R.id.iv)
        bundle = activity?.intent?.extras
        title = bundle?.getString("text")
        image = bundle?.getInt("image")
        tv!!.text = title
//        iv!!.setImageResource(image!!)
        tv?.isSelected = true

    }

}