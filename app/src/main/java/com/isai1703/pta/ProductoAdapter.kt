package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val productos: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombre)
        val imagen: ImageView = itemView.findViewById(R.id.ivImagen)
        val btnEnviar: Button = itemView.findViewById(R.id.btnEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = productos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        holder.imagen.setImageResource(producto.imagen)
        holder.btnEnviar.isEnabled = producto.disponible
        holder.btnEnviar.setOnClickListener { onClick(producto) }
    }
}
