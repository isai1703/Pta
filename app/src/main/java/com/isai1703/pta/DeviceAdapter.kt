package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val dispositivos: List<TipoDispositivo>
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreDispositivo)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccionDispositivo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dispositivo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dispositivo = dispositivos[position]
        holder.tvNombre.text = dispositivo.nombre
        holder.tvDireccion.text = dispositivo.direccion
    }

    override fun getItemCount(): Int = dispositivos.size
}
