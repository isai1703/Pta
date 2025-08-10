package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val listaProductos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.producto_imagen)
        val nombre: TextView = itemView.findViewById(R.id.producto_nombre)
        val precio: TextView = itemView.findViewById(R.id.producto_precio)
        val boton: Button = itemView.findViewById(R.id.boton_enviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]
        holder.imagen.setImageResource(producto.imagenResId)
        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"
        holder.boton.setOnClickListener { onProductoClick(producto) }
    }

    override fun getItemCount(): Int = listaProductos.size
}
