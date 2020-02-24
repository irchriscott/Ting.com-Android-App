package com.codepipes.ting.dialogs

import android.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import android.content.DialogInterface


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class SuccessOverlay : DialogFragment(){

    private lateinit var mCheckOverlayImage: ImageView
    private lateinit var mMessageOverlayText: TextView

    private lateinit var mCloseListener: SuccessDialogCloseListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        this.isCancelable = true
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val mArgs = arguments
        val message = mArgs.getString("message")
        val messageType = mArgs.getString("type")

        val view = inflater!!.inflate(R.layout.include_success_overlay, container, false)

        mCheckOverlayImage = view.findViewById(R.id.successOverlayImage) as ImageView
        mMessageOverlayText = view.findViewById(R.id.successOverlayText) as TextView

        if (messageType == "info") {
            mCheckOverlayImage.setImageDrawable(activity.resources.getDrawable(R.drawable.ic_info_white_64dp))
        } else { mCheckOverlayImage.setImageDrawable(activity.resources.getDrawable(R.drawable.ic_check_circle_primary_64dp)) }

        mMessageOverlayText.text = mArgs.getString("message")

        val mPopInAnimation = AnimationUtils.loadAnimation(activity!!, R.anim.pop_in)
        mCheckOverlayImage.startAnimation(mPopInAnimation)

        view.setOnClickListener { mCloseListener.handleDialogClose(null) }

        return view
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
        }
    }

    fun dismissListener(closeListener: SuccessDialogCloseListener) {
        this.mCloseListener = closeListener
    }

    override fun onDismiss(dialog: DialogInterface?) {
        mCloseListener.handleDialogClose(null)
        super.onDismiss(dialog)
    }
}