package com.codepipes.ting.customclasses

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.adapters.others.ActionSheetRecyclerViewAdapter
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.ActionSheetOnClickListener


class ActionSheet(var context: Context, var data: MutableList<String>) {

    private val actionSheet by lazy { ActionSheet(context, data) }

    var alertDialog: AlertDialog? = null
    lateinit var title : TextView
    private lateinit var cancle : TextView
    private lateinit var myRecyclerView: RecyclerView
    private val actionSheetRecyclerViewAdapter by lazy {
        ActionSheetRecyclerViewAdapter(
            data
        )
    }

    fun setColorTitle(title: Int): ActionSheet {
        mData.colorTitle = title
        return actionSheet
    }

    fun setColorTitleCancel(title: Int): ActionSheet {
        mData.colorCancel = title
        return actionSheet
    }

    fun setColorData(title: Int): ActionSheet {
        mData.colorData = title
        return actionSheet
    }

    fun setColorSelected(title: Int): ActionSheet {
        mData.colorSelect = title
        return actionSheet
    }

    fun setTitle(title: String): ActionSheet {
        mData.title = title
        return actionSheet
    }

    fun setCancelTitle(title: String): ActionSheet {
        mData.titleCancel = title
        return actionSheet
    }

    @SuppressLint("InflateParams")
    fun create(actionSheetCallBack: ActionSheetCallBack){
        val adb = AlertDialog.Builder(context)
        val v = LayoutInflater.from(context).inflate(R.layout.action_sheet,null)
        title = v.findViewById(R.id.tvTitle)
        cancle = v.findViewById(R.id.tvCancelAction)

        setData()

        myRecyclerView = v.findViewById(R.id.myRecyclerview)
        myRecyclerView.layoutManager = LinearLayoutManager(context)
        myRecyclerView.adapter = actionSheetRecyclerViewAdapter
        actionSheetRecyclerViewAdapter.notifyDataSetChanged()

        adb.setView(v)
        alertDialog = adb.create()
        alertDialog?.window?.attributes?.windowAnimations = R.style.DialogAnimations_SmileWindow
        alertDialog?.setCancelable(true)
        alertDialog?.window?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog?.show()

        actionSheetRecyclerViewAdapter.onClickCallBack(object : ActionSheetOnClickListener {
            override fun onClick(string: String, position: Int) {
                alertDialog?.dismiss()
                actionSheetCallBack.data(string, position)
            }
        })

        cancle.setOnClickListener { alertDialog?.dismiss() }

    }

    private fun setData() {
        title.text = mData.title
        cancle.text = mData.titleCancel
        title.setTextColor(mData.colorTitle)
        cancle.setTextColor(mData.colorCancel)
        actionSheetRecyclerViewAdapter.color = mData.colorData
        actionSheetRecyclerViewAdapter.colorSelect = mData.colorSelect
    }

    object mData {
        var title =""
        var titleCancel =""
        var colorData = Color.parseColor("#5EA1D6")
        var colorCancel = Color.parseColor("#5EA1D6")
        var colorTitle = Color.parseColor("#afafaf")
        var colorSelect = Color.parseColor("#FAFF1E1E")
    }
}