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
    private val listener: OnProductoClickListener
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    interface OnProductoClickListener {
        fun onProductoClick(comando: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"
        holder.imagen.setImageResource(producto.imagenResId)
        holder.btnEnviar.setOnClickListener {
            listener.onProductoClick(producto.comando)
        }
    }

    override fun getItemCount(): Int = productos.size

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.textNombre)
        val precio: TextView = itemView.findViewById(R.id.textPrecio)
        val imagen: ImageView = itemView.findViewById(R.id.imgProducto)
        val btnEnviar: Button = itemView.findViewById(R.id.btnEnviar)
    }
}
