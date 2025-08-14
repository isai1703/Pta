package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val items: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.VH>() {

    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvDisponible: TextView = v.findViewById(R.id.tvDisponible)
        val ivImagen: ImageView = v.findViewById(R.id.ivImagen)
        val btnEnviar: Button = v.findViewById(R.id.btnEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.tvNombre.text = p.nombre
        h.tvDisponible.text = if (p.disponible) "Disponible" else "Agotado"
        h.ivImagen.setImageResource(p.imagenRes)
        h.btnEnviar.isEnabled = p.disponible
        h.btnEnviar.setOnClickListener { onClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
