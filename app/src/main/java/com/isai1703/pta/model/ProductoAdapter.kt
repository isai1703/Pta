package com.isai1703.pta.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.R

class ProductoAdapter(
    private val context: Context,
    private val productos: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.productName)
        val imagen: ImageView = view.findViewById(R.id.productImage)
        val btnEnviar: Button = view.findViewById(R.id.btnSendCommand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = productos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        holder.imagen.setImageResource(producto.imagen)
        holder.btnEnviar.setOnClickListener { onClick(producto) }
    }
}
