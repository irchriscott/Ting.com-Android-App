package com.codepipes.ting.interfaces

import android.content.DialogInterface


interface SuccessDialogCloseListener {
    fun handleDialogClose(dialog: DialogInterface?)
}