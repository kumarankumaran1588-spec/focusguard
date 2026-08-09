package com.focusguard.blocker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.focusguard.blocker.databinding.RowAppBinding

class AppListAdapter(val items: List<AppItem>) :
    RecyclerView.Adapter<AppListAdapter.VH>() {

    inner class VH(val b: RowAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RowAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.appName.text = item.label
        holder.b.appPkg.text = item.pkg
        holder.b.checkbox.setOnCheckedChangeListener(null)
        holder.b.checkbox.isChecked = item.checked
        holder.b.checkbox.setOnCheckedChangeListener { _, isChecked -> item.checked = isChecked }
        holder.b.root.setOnClickListener { holder.b.checkbox.toggle() }
    }

    override fun getItemCount() = items.size
}
