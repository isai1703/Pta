#!/bin/bash

# Carpeta base del proyecto
BASE=~/Pta-filtrado/app/src/main/java/com/isai1703/pta
LAYOUT=~/Pta-filtrado/app/src/main/res/layout

# Crear carpetas si no existen
mkdir -p $BASE
mkdir -p $LAYOUT

# MainActivity.kt
cat > $BASE/MainActivity.kt << 'EOF'
package com.isai1703.pta

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var tvIp: TextView
    private lateinit var iconoEstado: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConectar: Button
    private lateinit var btnEditarIP: Button
    private lateinit var btnEscanearIP: Button
    private lateinit var btnEnviarTodos: Button

    private val dispositivosDetectados = mutableListOf<Dispositivo>()
    private val productos = mutableListOf<Producto>()
    private lateinit var adapter: ProductoAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val scanInterval = 15000L // 15 segundos para re-scan

    private lateinit var sharedPreferences: SharedPreferences
    private var modoSimulacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvIp = findViewById(R.id.tvIp)
        iconoEstado = findViewById(R.id.iconoEstado)
        recyclerView = findViewById(R.id.recyclerView)
        btnConectar = findViewById(R.id.btnConectar)
        btnEditarIP = findViewById(R.id.btnEditarIP)
        btnEscanearIP = findViewById(R.id.btnEscanearIP)
        btnEnviarTodos = findViewById(R.id.btnEnviarTodos)

        sharedPreferences = getSharedPreferences("Historial", Context.MODE_PRIVATE)

        // Setup RecyclerView
        adapter = ProductoAdapter(productos) { producto ->
            sendCommandProducto(producto)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Botones
        btnConectar.setOnClickListener { startAutoScan() }
        btnEscanearIP.setOnClickListener { scanRed() }
        btnEnviarTodos.setOnClickListener { enviarATodos() }

        // Inicia escaneo automático
        startAutoScan()
    }

    private fun startAutoScan() {
        scanRed()
        handler.postDelayed({ startAutoScan() }, scanInterval)
    }

    private fun scanRed() {
        CoroutineScope(Dispatchers.IO).launch {
            val ips = getLocalNetworkIPs()
            val nuevosDispositivos = mutableListOf<Dispositivo>()
            for (ip in ips) {
                val tipo = identificarDispositivo(ip)
                if (tipo != null) {
                    nuevosDispositivos.add(Dispositivo(ip, tipo))
                }
            }
            // Bluetooth
            scanBluetooth()?.let { nuevosDispositivos.addAll(it) }

            withContext(Dispatchers.Main) {
                dispositivosDetectados.clear()
                dispositivosDetectados.addAll(nuevosDispositivos)
                actualizarUI()
                reconectarDispositivos()
            }
        }
    }

    private fun getLocalNetworkIPs(): List<String> {
        val ips = mutableListOf<String>()
        val base = "192.168.1."
        for (i in 1..254) ips.add(base + i)
        return ips
    }

    private fun identificarDispositivo(ip: String): TipoDispositivo? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 200)
            socket.close()
            when {
                ip.contains("192.168.1.10") -> TipoDispositivo.ESP32
                ip.contains("192.168.1.20") -> TipoDispositivo.RASPBERRY
                ip.contains("192.168.1.30") -> TipoDispositivo.MINIPC
                else -> TipoDispositivo.DESCONOCIDO
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun scanBluetooth(): List<Dispositivo>? {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val lista = mutableListOf<Dispositivo>()
        pairedDevices?.forEach { device ->
            lista.add(Dispositivo(device.name, TipoDispositivo.DESCONOCIDO))
        }
        return lista
    }

    private fun reconectarDispositivos() {
        for (disp in dispositivosDetectados) {
            when (disp.tipo) {
                TipoDispositivo.ESP32, TipoDispositivo.RASPBERRY, TipoDispositivo.MINIPC -> sendCommandWifi("PING", disp.ip)
                else -> {}
            }
        }
    }

    private fun actualizarUI() {
        if (dispositivosDetectados.isNotEmpty()) {
            tvIp.text = dispositivosDetectados.joinToString { it.ip + " (" + it.tipo.name + ")" }
            iconoEstado.setImageResource(R.drawable.ic_conectado)
        } else {
            tvIp.text = "No se detectaron dispositivos"
            iconoEstado.setImageResource(R.drawable.ic_desconectado)
        }
    }

    private fun sendCommandProducto(producto: Producto) {
        dispositivosDetectados.forEach { disp ->
            sendCommandWifi("COMANDO_${producto.id}", disp.ip)
        }
        saveHistorial(producto)
    }

    private fun enviarATodos() {
        productos.forEach { sendCommandProducto(it) }
    }

    private fun sendCommandWifi(command: String, ip: String) {
        Toast.makeText(this, "Enviando $command a $ip", Toast.LENGTH_SHORT).show()
    }

    private fun saveHistorial(producto: Producto) {
        val historial = sharedPreferences.getStringSet("historial", mutableSetOf()) ?: mutableSetOf()
        historial.add("${producto.nombre} - ${System.currentTimeMillis()}")
        sharedPreferences.edit().putStringSet("historial", historial).apply()
    }
}

data class Dispositivo(val ip: String, val tipo: TipoDispositivo)
enum class TipoDispositivo { ESP32, RASPBERRY, MINIPC, DESCONOCIDO }
EOF

# Producto.kt
cat > $BASE/Producto.kt << 'EOF'
package com.isai1703.pta

data class Producto(
    val id: Int,
    val nombre: String,
    val imagen: Int,
    var disponible: Boolean = true
)
EOF

# ProductoAdapter.kt
cat > $BASE/ProductoAdapter.kt << 'EOF'
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
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombre)
        val imagen: ImageView = itemView.findViewById(R.id.ivImagen)
        val btnEnviar: Button = itemView.findViewById(R.id.btnEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = productos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        holder.imagen.setImageResource(producto.imagen)
        holder.btnEnviar.isEnabled = producto.disponible
        holder.btnEnviar.setOnClickListener { onClick(producto) }
    }
}
EOF

# activity_main.xml
cat > $LAYOUT/activity_main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/tvIp"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="IP de dispositivos"
        android:textSize="16sp"
        android:padding="8dp"/>

    <ImageView
        android:id="@+id/iconoEstado"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:src="@drawable/ic_desconectado"
        android:layout_below="@id/tvIp"
        android:layout_marginTop="8dp"
        android:layout_centerHorizontal="true"/>

    <Button
        android:id="@+id/btnConectar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Conectar"
        android:layout_below="@id/iconoEstado"
        android:layout_marginTop="8dp"
        android:layout_alignParentStart="true"/>

    <Button
        android:id="@+id/btnEditarIP"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Editar IP"
        android:layout_alignTop="@id/btnConectar"
        android:layout_centerHorizontal="true"/>

    <Button
        android:id="@+id/btnEscanearIP"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Escanear IP"
        android:layout_alignTop="@id/btnConectar"
        android:layout_alignParentEnd="true"/>

    <Button
        android:id="@+id/btnEnviarTodos"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Enviar a todos"
        android:layout_below="@id/btnConectar"
        android:layout_marginTop="8dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/btnEnviarTodos"
        android:layout_marginTop="8dp"/>
</RelativeLayout>
EOF

# item_producto.xml
cat > $LAYOUT/item_producto.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="horizontal"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp">

    <ImageView
        android:id="@+id/ivImagen"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:src="@drawable/ic_producto"
        android:layout_marginEnd="8dp"/>

    <TextView
        android:id="@+id/tvNombre"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:text="Nombre Producto"
        android:textSize="16sp"/>

    <Button
        android:id="@+id/btnEnviar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Enviar"/>
</LinearLayout>
EOF

echo "Todos los archivos se han creado correctamente."
