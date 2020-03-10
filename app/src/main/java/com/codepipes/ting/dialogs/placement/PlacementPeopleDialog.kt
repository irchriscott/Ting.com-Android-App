package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.SubmitPeoplePlacementListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_placement_people.view.*

class PlacementPeopleDialog : DialogFragment() {

    private lateinit var submitPeoplePlacementListener: SubmitPeoplePlacementListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = inflater.inflate(R.layout.fragment_placement_people, null, false)

        val people = arguments.getString("people")
        view.dialog_input.setText(people ?: "1")
        view.dialog_success.setOnClickListener { submitPeoplePlacementListener.onSubmit(view.dialog_input.text.toString()?:"1") }
        view.dialog_close.setOnClickListener { dialog.dismiss() }

        return view
    }

    fun onSubmitPeople(listener: SubmitPeoplePlacementListener) {
        this.submitPeoplePlacementListener = listener
    }
}