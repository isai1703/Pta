package com.isai1703.pta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Button
import android.widget.Toast
import com.isai1703.pta.model.Producto
import com.isai1703.pta.model.ProductoAdapter
import com.isai1703.pta.utils.NetworkUtils

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val listaProductos = mutableListOf<Producto>()
    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductoAdapter(listaProductos) { producto ->
            sendCommand(producto)
        }
        recyclerView.adapter = adapter

        // Botón para escanear dispositivos
        scanButton = findViewById(R.id.btnScanDevices)
        scanButton.setOnClickListener {
            scanDevices()
        }

        // Agregar productos de prueba
        listaProductos.add(Producto("Producto 1", "$10", R.drawable.icon_prueba))
        listaProductos.add(Producto("Producto 2", "$15", R.drawable.icon_prueba))
        adapter.notifyDataSetChanged()
    }

    private fun sendCommand(producto: Producto) {
        Toast.makeText(this, "Enviando comando a ${producto.nombre}", Toast.LENGTH_SHORT).show()
        // Aquí se integra WiFi/Bluetooth según NetworkUtils
    }

    private fun scanDevices() {
        NetworkUtils.scanDevices(this) { device, progress ->
            runOnUiThread {
                Toast.makeText(this, "Detectado: ${device.type} en ${device.ip} ($progress%)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
