package com.codepipes.ting.adapters.others

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.SelectItemListener
import kotlinx.android.synthetic.main.row_select.view.*


class SelectAdapter (private val items: List<String>, private val selectItemListener: SelectItemListener) : RecyclerView.Adapter<SelectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): SelectViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_select, parent, false)
        return SelectViewHolder(row)
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        holder.view.select_text.text = items[position]
        if(position == items.size - 1) { holder.view.select_separator.visibility = View.GONE }
        holder.view.setOnClickListener { selectItemListener.onSelectItem(position) }
    }
}

class SelectViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}