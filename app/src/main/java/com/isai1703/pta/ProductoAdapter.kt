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
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(v)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.producto_imagen)
        private val tvNombre: TextView = itemView.findViewById(R.id.producto_nombre)
        private val tvPrecio: TextView = itemView.findViewById(R.id.producto_precio)
        private val btnEnviar: Button = itemView.findViewById(R.id.boton_enviar)

        fun bind(p: Producto) {
            img.setImageResource(p.imagenResId)
            tvNombre.text = p.nombre
            tvPrecio.text = p.precio
            btnEnviar.setOnClickListener { onClick(p.comando) }
        }
    }
}
