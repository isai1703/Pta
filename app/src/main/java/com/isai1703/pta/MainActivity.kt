package com.isai1703.pta

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var rvProductos: RecyclerView
    private lateinit var tvIp: TextView
    private lateinit var ivConexion: ImageView
    private lateinit var btnConectar: Button
    private lateinit var btnEditarIP: Button
    private lateinit var btnEscanearIP: Button
    private lateinit var btnEnviarTodos: Button

    private val productos = mutableListOf<Producto>()
    private val dispositivos = mutableListOf<TipoDispositivo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvProductos = findViewById(R.id.rvProductos)
        tvIp = findViewById(R.id.tvIp)
        ivConexion = findViewById(R.id.ivConexion)
        btnConectar = findViewById(R.id.btnConectar)
        btnEditarIP = findViewById(R.id.btnEditarIP)
        btnEscanearIP = findViewById(R.id.btnEscanearIP)
        btnEnviarTodos = findViewById(R.id.btnEnviarTodos)

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = ProductoAdapter(productos, this::enviarComando)

        inicializarDispositivos()
        inicializarProductos()

        btnConectar.setOnClickListener { conectarDispositivos() }
        btnEditarIP.setOnClickListener { mostrarDialogEditarIP() }
        btnEscanearIP.setOnClickListener { escanearDispositivos() }
        btnEnviarTodos.setOnClickListener { enviarATodos() }
    }

    private fun inicializarDispositivos() {
        dispositivos.add(TipoDispositivo.ESP32("ESP32-A", "192.168.1.50"))
        dispositivos.add(TipoDispositivo.RASPBERRY("Raspberry-01", "192.168.1.51"))
        dispositivos.add(TipoDispositivo.MINIPC("MiniPC-01", "192.168.1.52"))
    }

    private fun inicializarProductos() {
        productos.add(Producto("Agua", 10.0, R.drawable.ic_producto))
        productos.add(Producto("Cocacola", 15.0, R.drawable.ic_producto))
        productos.add(Producto("Paletas", 5.0, R.drawable.ic_producto))
    }

    private fun enviarComando(producto: Producto) {
        for (disp in dispositivos) {
            when (disp) {
                is TipoDispositivo.ESP32 -> {/* enviar WiFi/Bluetooth */}
                is TipoDispositivo.RASPBERRY -> {/* acción específica */}
                is TipoDispositivo.MINIPC -> {/* acción MiniPC */}
            }
        }
        Toast.makeText(this, "Comando enviado: ${producto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun conectarDispositivos() {
        for (disp in dispositivos) {
            // Aquí podrías verificar conexión real
        }
        ivConexion.setImageResource(R.drawable.ic_producto)
        tvIp.text = "Conectado a todos"
    }

    private fun mostrarDialogEditarIP() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar IP del dispositivo")

        val input = EditText(this)
        input.hint = "Nueva IP"
        builder.setView(input)

        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevaIP = input.text.toString()
            dispositivos.firstOrNull()?.let { it.direccion = nuevaIP }
            Toast.makeText(this, "IP actualizada a $nuevaIP", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun escanearDispositivos() {
        Toast.makeText(this, "Escaneando dispositivos...", Toast.LENGTH_SHORT).show()
        // Lógica de escaneo real aquí
    }

    private fun enviarATodos() {
        for (producto in productos) {
            enviarComando(producto)
        }
        Toast.makeText(this, "Comando enviado a todos los productos", Toast.LENGTH_SHORT).show()
    }
}
