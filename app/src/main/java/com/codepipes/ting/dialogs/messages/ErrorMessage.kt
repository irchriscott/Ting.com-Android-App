package com.codepipes.ting.dialogs.messages

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.codepipes.ting.R

class ErrorMessage(
    private val contxt: Context?,
    private val message: String
){
    public fun show(){
        val alertDialogBuilder = AlertDialog.Builder(contxt!!)
        alertDialogBuilder.setTitle("Error")
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setNegativeButton("OK", DialogInterface.OnClickListener { _, _ -> })
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
        val alertTitle = contxt.resources.getIdentifier("alertTitle", "id", "android");
        val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
        val titleView = alertDialog.findViewById<TextView>(alertTitle)
        val okButton = alertDialog.findViewById<Button>(android.R.id.button1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            messageView!!.typeface = contxt.resources.getFont(R.font.poppins_regular)
            titleView!!.typeface = contxt.resources.getFont(R.font.poppins_semi_bold)
            okButton!!.typeface = contxt.resources.getFont(R.font.poppins_semi_bold)
        }
    }
}