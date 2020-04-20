package com.codepipes.ting.custom

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        ActionSheetRecyclerViewAdapter(data)
    }

    fun setColorTitle(title: Int): ActionSheet {
        ActionData.colorTitle = title
        return actionSheet
    }

    fun setColorTitleCancel(title: Int): ActionSheet {
        ActionData.colorCancel = title
        return actionSheet
    }

    fun setColorData(title: Int): ActionSheet {
        ActionData.colorData = title
        return actionSheet
    }

    fun setColorSelected(title: Int): ActionSheet {
        ActionData.colorSelect = title
        return actionSheet
    }

    fun setTitle(title: String): ActionSheet {
        ActionData.title = title
        return actionSheet
    }

    fun setCancelTitle(title: String): ActionSheet {
        ActionData.titleCancel = title
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
        title.text = ActionData.title
        cancle.text = ActionData.titleCancel
        title.setTextColor(ActionData.colorTitle)
        cancle.setTextColor(ActionData.colorCancel)
        actionSheetRecyclerViewAdapter.color = ActionData.colorData
        actionSheetRecyclerViewAdapter.colorSelect = ActionData.colorSelect
    }

    object ActionData {
        var title =""
        var titleCancel =""
        var colorData = Color.parseColor("#5EA1D6")
        var colorCancel = Color.parseColor("#5EA1D6")
        var colorTitle = Color.parseColor("#afafaf")
        var colorSelect = Color.parseColor("#FAFF1E1E")
    }
}