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

    inner class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProducto: ImageView = view.findViewById(R.id.imgProducto)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreProducto)
        val txtPrecio: TextView = view.findViewById(R.id.txtPrecioProducto)
        val btnComprar: Button = view.findViewById(R.id.btnComprar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.imgProducto.setImageResource(producto.imagenResId)
        holder.txtNombre.text = producto.nombre
        holder.txtPrecio.text = "$${producto.precio}"
        holder.btnComprar.setOnClickListener {
            listener.onProductoClick(producto.comando)
        }
    }

    override fun getItemCount() = productos.size
}
