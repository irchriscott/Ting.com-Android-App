package com.codepipes.ting.dialogs.messages

import androidx.fragment.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R

class ProgressOverlay : DialogFragment(){

    override fun onCreate(savedInstanceState: Bundle?) {
        this.isCancelable = false
        super.onCreate(savedInstanceState)
    }

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.include_progress_overlay, container, false)
    }
}