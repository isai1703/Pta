package com.isai1703.pta

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private var ipESP32: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipESP32 = leerIPDesdeConfig()

        val productos = listOf(
            Producto("Coca Cola", 15.0, R.drawable.coca_cola, "CMD_COCA"),
            Producto("Pepsi", 14.0, R.drawable.pepsi, "CMD_PEPSI"),
            Producto("Agua", 12.0, R.drawable.agua, "CMD_AGUA")
        )

        recyclerView = findViewById(R.id.recycler_productos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductoAdapter(productos) { producto ->
            enviarComando(producto.comando)
        }

        recyclerView.adapter = adapter
    }

    private fun leerIPDesdeConfig(): String {
        return try {
            val archivo = File(filesDir, "config.txt")
            if (archivo.exists()) {
                archivo.readText().trim()
            } else {
                "192.168.0.100"
            }
        } catch (e: Exception) {
            "192.168.0.100"
        }
    }

    private fun enviarComando(comando: String) {
        Toast.makeText(this, "Enviando $comando a $ipESP32", Toast.LENGTH_SHORT).show()
        // Aquí irá la lógica de conexión WiFi o Bluetooth
    }
}
