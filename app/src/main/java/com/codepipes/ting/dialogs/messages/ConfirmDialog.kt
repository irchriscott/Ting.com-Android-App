package com.codepipes.ting.dialogs.messages

import android.annotation.SuppressLint
import android.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.interfaces.ConfirmDialogListener
import com.codepipes.ting.interfaces.SubmitPeoplePlacementListener
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_confirm_dialog.view.*

class ConfirmDialog : DialogFragment() {

    private lateinit var confirmDialogListener: ConfirmDialogListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = inflater.inflate(R.layout.fragment_confirm_dialog, null, false)

        val title = arguments?.getString(CurrentRestaurant.CONFIRM_TITLE_KEY)
        val message = arguments?.getString(CurrentRestaurant.CONFIRM_MESSAGE_KEY)

        view.dialog_title.text = title
        if(message != null) { view.dialog_text.text = message } else { view.dialog_text.visibility = View.GONE }
        view.dialog_yes.setOnClickListener { confirmDialogListener.onAccept() }
        view.dialog_cancel.setOnClickListener { confirmDialogListener.onCancel() }

        return view
    }

    public fun onDialogListener(listener: ConfirmDialogListener) {
        this.confirmDialogListener = listener
    }
}