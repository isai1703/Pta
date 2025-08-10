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
    private val onEnviarComando: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.productoImagen)
        val nombre: TextView = view.findViewById(R.id.productoNombre)
        val precio: TextView = view.findViewById(R.id.productoPrecio)
        val btnEnviar: Button = view.findViewById(R.id.btnEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.imagen.setImageResource(producto.imagenResId)
        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"
        holder.btnEnviar.setOnClickListener { onEnviarComando(producto) }
    }

    override fun getItemCount(): Int = productos.size
}
