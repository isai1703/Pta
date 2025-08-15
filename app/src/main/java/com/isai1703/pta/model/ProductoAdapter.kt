package com.isai1703.pta.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.R
import com.isai1703.pta.Producto

class ProductoAdapter(
    private val productos: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.tvNombre)
        val productPrice: TextView = view.findViewById(R.id.tvPrecio)
        val productImage: ImageView = view.findViewById(R.id.ivProducto)
        val btnSendCommand: Button = view.findViewById(R.id.btnEnviarComando)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = productos[position]
        holder.productName.text = producto.nombre
        holder.productPrice.text = "$${producto.precio}"
        holder.productImage.setImageResource(producto.imagen)
        holder.btnSendCommand.setOnClickListener { onClick(producto) }
    }

    override fun getItemCount(): Int = productos.size
}
