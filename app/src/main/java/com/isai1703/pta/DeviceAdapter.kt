package com.isai1703.pta.model.DeviceInfo

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
        holder.tvName.text = d.name
        holder.tvIp.text = d.ip
        holder.tvType.text = d.type
        holder.itemView.isSelected = (position == selectedPosition)
    }

    override fun getItemCount(): Int = devices.size
}
