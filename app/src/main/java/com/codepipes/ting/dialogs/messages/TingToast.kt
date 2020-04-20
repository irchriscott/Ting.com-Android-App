package com.codepipes.ting.dialogs.messages

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.utils.Constants
import com.codepipes.ting.utils.UtilsFunctions


enum class TingToastType {
    DEFAULT, SUCCESS, WARNING, ERROR
}

class TingToast(
    private val context: Context,
    private val message: String,
    private val toastType: TingToastType
) {

    @SuppressLint("InflateParams")
    public fun showToast(duration: Int){
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.ting_toast_layout, null, false)

        val containerView = layout.findViewById<LinearLayout>(R.id.toastContainer) as LinearLayout
        val imageView = layout.findViewById<ImageView>(R.id.toastIcon) as ImageView
        val textView = layout.findViewById<TextView>(R.id.toastText) as TextView

        when(this.toastType){

            TingToastType.DEFAULT -> {
                containerView.background = context.resources.getDrawable(R.drawable.background_toast_default)
                imageView.setImageBitmap(UtilsFunctions(context).base64ToBitmap(Constants().toastDefaultImage))
            }
            TingToastType.SUCCESS -> {
                containerView.background = context.resources.getDrawable(R.drawable.background_toast_success)
                imageView.setImageBitmap(UtilsFunctions(context).base64ToBitmap(Constants().toastSuccessImage))
            }
            TingToastType.WARNING -> {
                containerView.background = context.resources.getDrawable(R.drawable.background_toast_warning)
                imageView.setImageBitmap(UtilsFunctions(context).base64ToBitmap(Constants().toastWarningImage))
            }
            TingToastType.ERROR -> {
                containerView.background = context.resources.getDrawable(R.drawable.background_toast_error)
                imageView.setImageBitmap(UtilsFunctions(context).base64ToBitmap(Constants().toastErrorImage))
            }
        }

        textView.text = message

        val toast = Toast(context)
        toast.duration = duration
        toast.setGravity(Gravity.BOTTOM, 0, 50)
        toast.view = layout
        toast.show()
    }
}