package com.isai1703.pta

import android.os.Bundle
import android.widget.*
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

        btnConectar.setOnClickListener { conectarDispositivo() }
        btnEditarIP.setOnClickListener { editarIP() }
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
        for (disp in dispositivos) { /* Lógica WiFi/Bluetooth por tipo */ }
        Toast.makeText(this, "Comando enviado: ${producto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun conectarDispositivo() {
        ivConexion.setImageResource(R.drawable.ic_producto) // ejemplo icono conectado
        tvIp.text = "Conectado"
    }

    private fun editarIP() { /* Funcionalidad editar IP */ }
    private fun escanearDispositivos() { /* Escaneo todos dispositivos */ }
    private fun enviarATodos() { /* Enviar comando a todos */ }
}
