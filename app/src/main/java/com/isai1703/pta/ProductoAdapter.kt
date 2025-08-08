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
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreTextView: TextView = itemView.findViewById(R.id.nombreProducto)
        val precioTextView: TextView = itemView.findViewById(R.id.precioProducto)
        val imagenImageView: ImageView = itemView.findViewById(R.id.imagenProducto)
        val botonEnviar: Button = itemView.findViewById(R.id.botonEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombreTextView.text = producto.nombre
        holder.precioTextView.text = "$${producto.precio}"
        holder.imagenImageView.setImageResource(producto.imagenResId)

        holder.botonEnviar.setOnClickListener {
            onClick(producto)
        }
    }

    override fun getItemCount(): Int = productos.size
}
