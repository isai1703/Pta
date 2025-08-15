package com.isai1703.pta

import android.app.Activity
import android.content.Intent
import android.net.wifi.WifiManager
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

    private val PICK_IMAGE = 123

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
        productos.add(Producto("Jugo", 15.0, R.drawable.ic_producto))
    }

    private fun enviarComando(producto: Producto) {
        Toast.makeText(this, "Enviando ${producto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun conectarDispositivo() {
        Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show()
    }

    private fun editarIP() {
        Toast.makeText(this, "Editar IP", Toast.LENGTH_SHORT).show()
    }

    private fun escanearDispositivos() {
        Toast.makeText(this, "Escaneando dispositivos...", Toast.LENGTH_SHORT).show()
    }

    private fun enviarATodos() {
        Toast.makeText(this, "Enviando a todos los dispositivos", Toast.LENGTH_SHORT).show()
    }
}
