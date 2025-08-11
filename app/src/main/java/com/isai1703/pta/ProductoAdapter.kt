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
    private val onProductoClick: OnProductoClickListener
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
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagenProducto: ImageView = itemView.findViewById(R.id.imgProducto)
        private val nombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val precioProducto: TextView = itemView.findViewById(R.id.tvPrecioProducto)
        private val botonComprar: Button = itemView.findViewById(R.id.btnComprar)

        fun bind(producto: Producto) {
            imagenProducto.setImageResource(producto.imagenResId)
            nombreProducto.text = producto.nombre
            precioProducto.text = producto.comando  // Aquí puedes cambiar si quieres mostrar otro texto en "precio"
            botonComprar.setOnClickListener {
                onProductoClick.onProductoClick(producto.comando)
            }
        }
    }
}
