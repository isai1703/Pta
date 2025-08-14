package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val items: MutableList<Device>,
    private val onConnectClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
        val tvNombre: TextView = v.findViewById(R.id.tvNombreDispositivo)
        val tvDireccion: TextView = v.findViewById(R.id.tvDireccionDispositivo)
        val tvTipo: TextView = v.findViewById(R.id.tvTipoDispositivo)
        val btnConectar: Button = v.findViewById(R.id.btnConectar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val d = items[pos]
        h.tvNombre.text = d.nombre
        h.tvDireccion.text = d.address
        h.tvTipo.text = d.tipo.name
        h.ivStatus.setImageResource(if (d.conectado) R.drawable.ic_connected else R.drawable.ic_disconnected)
        h.btnConectar.setOnClickListener { onConnectClick(d) }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(nuevos: List<Device>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    fun markConnected(address: String, connected: Boolean) {
        val i = items.indexOfFirst { it.address == address }
        if (i >= 0) {
            items[i] = items[i].copy(conectado = connected)
            notifyItemChanged(i)
        }
    }
}
