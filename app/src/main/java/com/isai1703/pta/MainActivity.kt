package com.isai1703.pta

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var estadoConexion: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        estadoConexion = findViewById(R.id.estadoConexion)
        recyclerView = findViewById(R.id.recyclerProductos)

        val listaProductos = listOf(
            Producto("Coca-Cola", 15.0, R.drawable.coca_cola, "CMD_COCA"),
            Producto("Pepsi", 14.0, R.drawable.pepsi, "CMD_PEPSI"),
            Producto("Agua", 10.0, R.drawable.agua, "CMD_AGUA")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(listaProductos) { producto ->
            enviarComando(producto)
        }

        actualizarEstadoConexion(false) // Por ahora falso hasta implementar conexión
    }

    private fun enviarComando(producto: Producto) {
        Toast.makeText(this, "Enviando comando: ${producto.comando}", Toast.LENGTH_SHORT).show()
        // Aquí luego integramos la lógica WiFi/Bluetooth
    }

    private fun actualizarEstadoConexion(conectado: Boolean) {
        if (conectado) {
            estadoConexion.setImageResource(R.drawable.ic_online)
        } else {
            estadoConexion.setImageResource(R.drawable.ic_offline)
        }
    }
}
