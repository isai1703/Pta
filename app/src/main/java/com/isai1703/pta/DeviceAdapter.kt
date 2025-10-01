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

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    selectedPosition = pos
                    notifyDataSetChanged()
                    onDeviceSelected(devices[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(v)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val d = devices[position]
        
        // Identifica el dispositivo por IP
        val identification = if (d.ip != null) {
            com.isai1703.pta.utils.DeviceIdentifier.identifyDevice(d.ip)
        } else {
            com.isai1703.pta.utils.DeviceIdentification(
                name = d.name,
                type = d.type,
                description = "Sin IP"
            )
        }
        
        // Muestra nombre identificado + IP
        holder.tvName.text = identification.name
        holder.tvIp.text = d.ip ?: "N/A"
        holder.tvType.text = identification.description
        holder.itemView.isSelected = (position == selectedPosition)
    }

    override fun getItemCount(): Int = devices.size
}
