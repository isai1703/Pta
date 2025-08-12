package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private val productos = mutableListOf<Producto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inicializarBluetooth()
        inicializarRecyclerView()
        cargarConfigIP()
    }

    private fun inicializarBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado en este dispositivo", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    private val bluetoothEnableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth no activado", Toast.LENGTH_SHORT).show()
            }
        }

    private fun inicializarRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productoAdapter = ProductoAdapter(productos) { producto ->
            enviarComando(producto.comando)
        }
        recyclerView.adapter = productoAdapter
    }

    private fun cargarConfigIP() {
        val configFile = File(filesDir, "config.txt")
        if (!configFile.exists()) {
            configFile.writeText("192.168.0.100") // valor por defecto
        }
        val ip = configFile.readText().trim()
        Toast.makeText(this, "IP cargada: $ip", Toast.LENGTH_SHORT).show()
    }

    private fun enviarComando(comando: String) {
        if (estaConectadoWifi()) {
            Toast.makeText(this, "Enviando por WiFi: $comando", Toast.LENGTH_SHORT).show()
            // Lógica WiFi
        } else if (bluetoothAdapter?.isEnabled == true) {
            Toast.makeText(this, "Enviando por Bluetooth: $comando", Toast.LENGTH_SHORT).show()
            // Lógica Bluetooth
        } else {
            Toast.makeText(this, "No hay conexión disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun estaConectadoWifi(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun escanearDispositivosBluetooth() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            return
        }

        val adapter = bluetoothAdapter
        if (adapter != null && adapter.isEnabled) {
            adapter.startDiscovery()
            val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices
            for (device in pairedDevices) {
                println("Dispositivo emparejado: ${device.name} - ${device.address}")
            }
        }
    }
}
