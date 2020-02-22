package com.codepipes.ting.dialogs

import android.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_info_dialog.view.*

class InfoDialog : DialogFragment() {

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = inflater.inflate(R.layout.fragment_info_dialog, null, false)

        val image = arguments?.getString("image")
        val title = arguments?.getString("title")
        val message = arguments?.getString("message")

        if(image != null) { Picasso.get().load("${Routes().HOST_END_POINT}$image").into(view.dialog_image) }
        view.dialog_title.text = title
        if(message != null) { view.dialog_text.text = message } else { view.dialog_text.visibility = View.GONE }
        view.dialog_close.setOnClickListener { dialog.dismiss() }

        return view
    }
}