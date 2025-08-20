package com.isai1703.pta.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.R

class ProductoAdapter(
    private val productos: List<Producto>,
    private val onSendCommandClick: (Producto) -> Unit,
    private val onEditClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun getItemCount(): Int = productos.size

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]

        holder.tvNombre.text = producto.nombre
        holder.tvPrecio.text = producto.precio
        producto.imagenPath?.let {
            // Cargar imagen desde ruta si existe
            val uri = android.net.Uri.parse(it)
            holder.ivProducto.setImageURI(uri)
        } ?: holder.ivProducto.setImageResource(R.drawable.icon_prueba)

        holder.btnEnviarComando.setOnClickListener {
            onSendCommandClick(producto)
        }

        holder.btnEditar.setOnClickListener {
            onEditClick(producto)
        }
    }

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val ivProducto: ImageView = itemView.findViewById(R.id.ivProducto)
        val btnEnviarComando: Button = itemView.findViewById(R.id.btnEnviarComando)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
    }
}
