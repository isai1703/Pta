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
    private val onCommandClick: (String) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun getItemCount() = productos.size

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagen: ImageView = itemView.findViewById(R.id.imagenProducto)
        private val nombre: TextView = itemView.findViewById(R.id.nombreProducto)
        private val precio: TextView = itemView.findViewById(R.id.precioProducto)
        private val btnEnviar: Button = itemView.findViewById(R.id.btnEnviarComando)

        fun bind(producto: Producto) {
            imagen.setImageResource(producto.imagenResId)
            nombre.text = producto.nombre
            precio.text = producto.precio

            btnEnviar.setOnClickListener {
                onCommandClick(producto.comando)
            }
        }
    }
}
