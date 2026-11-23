package com.bunker.urltest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AppPickerAdapter(
    private val allApps: List<MainActivity.LaunchableApp>,
    private val selectedPackages: MutableSet<String>
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    private var filteredApps: List<MainActivity.LaunchableApp> = allApps

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.appCheckBox)
        val labelText: TextView = view.findViewById(R.id.appLabelText)
        val packageText: TextView = view.findViewById(R.id.appPackageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.labelText.text = app.label
        holder.packageText.text = app.packageName
        holder.checkBox.isChecked = selectedPackages.contains(app.packageName)

        val toggle = {
            if (selectedPackages.contains(app.packageName)) {
                selectedPackages.remove(app.packageName)
                holder.checkBox.isChecked = false
            } else {
                selectedPackages.add(app.packageName)
                holder.checkBox.isChecked = true
            }
        }

        holder.itemView.setOnClickListener { toggle() }
        holder.checkBox.setOnClickListener { toggle() }
    }

    override fun getItemCount(): Int = filteredApps.size

    fun filter(query: String) {
        val q = query.trim().lowercase(Locale.ROOT)
        filteredApps = if (q.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.lowercase(Locale.ROOT).contains(q) || it.packageName.lowercase(Locale.ROOT).contains(q)
            }
        }
        notifyDataSetChanged()
    }

    fun selectAll() {
        for (app in filteredApps) {
            selectedPackages.add(app.packageName)
        }
        notifyDataSetChanged()
    }

    fun clearAll() {
        for (app in filteredApps) {
            selectedPackages.remove(app.packageName)
        }
        notifyDataSetChanged()
    }
}
