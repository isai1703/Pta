package com.isai1703.pta

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var imgEstado: ImageView
    private lateinit var tvIp: TextView
    private lateinit var btnConectar: Button
    private var conectado = false
    private var ipEsp32 = "0.0.0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgEstado = findViewById(R.id.imgEstadoConexion)
        tvIp = findViewById(R.id.tvIp)
        btnConectar = findViewById(R.id.btnConectar)

        // Leer IP desde config.txt
        ipEsp32 = leerIpDesdeConfig()
        tvIp.text = "IP: $ipEsp32"

        btnConectar.setOnClickListener {
            conectado = !conectado
            actualizarEstadoConexion()
        }

        actualizarEstadoConexion()
    }

    private fun leerIpDesdeConfig(): String {
        return try {
            val file = File(filesDir, "config.txt")
            if (file.exists()) {
                file.readText().trim()
            } else {
                "0.0.0.0"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "0.0.0.0"
        }
    }

    private fun actualizarEstadoConexion() {
        if (conectado) {
            imgEstado.setImageResource(R.drawable.ic_connected)
            btnConectar.text = "Desconectar"
        } else {
            imgEstado.setImageResource(R.drawable.ic_disconnected)
            btnConectar.text = "Conectar"
        }
    }
}
