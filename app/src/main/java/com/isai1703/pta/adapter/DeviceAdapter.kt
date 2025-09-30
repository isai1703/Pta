package com.isai1703.pta.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.R
import com.isai1703.pta.model.DeviceInfo

/**
 * Adapter para mostrar la lista de dispositivos detectados
 * (WiFi / Bluetooth) en un RecyclerView.
 *
 * Compatible con ESP32, Raspberry Pi y sistemas Android embebidos.
 */
class DeviceAdapter(
    private val devices: List<DeviceInfo>,
    private val onClick: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        private val tvIp: TextView = view.findViewById(R.id.tvDeviceIp)

        fun bind(device: DeviceInfo) {
            tvName.text = device.name
            tvIp.text = device.ip ?: "Dirección no disponible"

            itemView.setOnClickListener { onClick(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size
}
