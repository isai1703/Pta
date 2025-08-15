package com.isai1703.pta

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val context: Context,
    private val productos: MutableList<Producto>,
    private val onCommandClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.productName)
        val imagen: ImageView = view.findViewById(R.id.productImage)
        val boton: Button = view.findViewById(R.id.sendCommandButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        if (producto.imagenPath != null) holder.imagen.setImageURI(Uri.parse(producto.imagenPath))
        holder.boton.setOnClickListener { onCommandClick(producto) }
    }

    override fun getItemCount(): Int = productos.size
}
