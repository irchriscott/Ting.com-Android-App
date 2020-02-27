package com.codepipes.ting.dialogs.placement

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.SubmitOrderListener
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_order_form.view.*

class OrderFormDialog : DialogFragment() {

    private lateinit var onSubmitOrderListener: SubmitOrderListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_order_form, null, false)
        view.dialog_quantity_input.setText(arguments?.getString("quantity"))
        view.dialog_conditions_input.setText(arguments?.getString("conditions"))
        view.dialog_success.setOnClickListener {
            onSubmitOrderListener.onSubmitOrder(view.dialog_quantity_input.text.toString(), view.dialog_conditions_input.text.toString())
        }
        view.dialog_close.setOnClickListener { dialog.dismiss() }

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

    fun onSubmitOrder(listener: SubmitOrderListener) {
        this.onSubmitOrderListener = listener
    }
}