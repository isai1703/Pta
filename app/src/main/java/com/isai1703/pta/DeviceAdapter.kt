package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.model.DeviceInfo

class DeviceAdapter(
    private val devices: List<DeviceInfo>,
    private val onDeviceSelected: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var selectedPosition = -1

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvIp: TextView = view.findViewById(R.id.tvDeviceIp)
        val tvType: TextView = view.findViewById(R.id.tvDeviceType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        holder.tvName.text = device.name ?: "Desconocido"
        holder.tvIp.text = device.ip
        // <- FIX: DeviceType (o cualquier otro tipo) como texto
        holder.tvType.text = device.type?.toString() ?: "—"

        holder.itemView.isSelected = (position == selectedPosition)
        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            if (previous != -1) notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onDeviceSelected(device)
        }
    }

    override fun getItemCount(): Int = devices.size
}
