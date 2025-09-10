package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductoAdapter(
    private val items: List<Producto>,
    private val onSendCommandClick: (Producto) -> Unit,
    private val onEditClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreProducto)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioProducto)
        val ivProducto: ImageView = view.findViewById(R.id.ivProducto)
        val btnSend: Button = view.findViewById(R.id.btnSendCommand)
        val btnEdit: Button = view.findViewById(R.id.btnEditProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        holder.tvNombre.text = p.nombre
        holder.tvPrecio.text = p.precio
        if (!p.imagenPath.isNullOrBlank()) {
            Glide.with(holder.itemView).load(p.imagenPath).into(holder.ivProducto)
        } else {
            holder.ivProducto.setImageResource(android.R.drawable.ic_menu_report_image)
        }
        holder.btnSend.setOnClickListener { onSendCommandClick(p) }
        holder.btnEdit.setOnClickListener { onEditClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
