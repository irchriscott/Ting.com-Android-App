package com.codepipes.ting.adapters.others


import android.graphics.Color
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.ActionSheetOnClickListener


class ActionSheetRecyclerViewAdapter(var data: MutableList<String>) : RecyclerView.Adapter<ActionSheetRecyclerViewAdapter.MyViewHolder>() {

    lateinit var onClick: ActionSheetOnClickListener

    var color = Color.parseColor("#5EA1D6")
    var colorSelect = Color.parseColor("#FAFF1E1E")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.action_sheet_recyclerview_layout, parent, false)

        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val string = data[position]

        if (position == data.size - 1){
            holder.linebottom.visibility = View.GONE
        }

        holder.textView.text = string
        holder.itemView.setOnClickListener {
            holder.textView.setTextColor(colorSelect)
            Handler().postDelayed({
                onClick.onClick(string,position)
            }, 10)

        }
        holder.textView.setTextColor(color)
    }

    inner class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val textView = v.findViewById(R.id.tvName) as TextView
        val linebottom = v.findViewById(R.id.linebottom) as LinearLayout
    }

    fun onClickCallBack(actionSheetOnClickListener: ActionSheetOnClickListener){
        this.onClick =  actionSheetOnClickListener
    }

}