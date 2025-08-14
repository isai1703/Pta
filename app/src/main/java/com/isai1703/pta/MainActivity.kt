package com.isai1703.pta

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

data class TipoDispositivo(
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val conexion: String
)

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val dispositivosCompatibles = mutableListOf<TipoDispositivo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        scanBluetoothDevices()
        scanWifiDevices()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanBluetoothDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) bluetoothAdapter.enable()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            if (isCompatibleBluetooth(device)) {
                dispositivosCompatibles.add(
                    TipoDispositivo(
                        nombre = device.name ?: device.address,
                        tipo = "Bluetooth",
                        direccion = device.address,
                        conexion = "Bluetooth"
                    )
                )
            }
        }
        if (dispositivosCompatibles.isNotEmpty()) showDeviceListDialog()
    }

    private fun isCompatibleBluetooth(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("esp32") || name.contains("raspberry") ||
               name.contains("stm32") || name.contains("pta-device")
    }

    private fun scanWifiDevices() {
        # Ejemplo placeholder, agrega tus IPs escaneadas
        # dispositivosCompatibles.add(TipoDispositivo("ESP32 WiFi", "WiFi", "192.168.1.50", "WiFi"))
    }

    private fun showDeviceListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_device_list, null)
        val listView: ListView = dialogView.findViewById(R.id.deviceListView)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            dispositivosCompatibles.map { "${it.nombre} (${it.tipo})" }
        )
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.device_list_title))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val dispositivo = dispositivosCompatibles[position]
            connectToDevice(dispositivo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun connectToDevice(dispositivo: TipoDispositivo) {
        Toast.makeText(this, "Conectando a ${dispositivo.nombre}...", Toast.LENGTH_SHORT).show()
        when (dispositivo.conexion) {
            "Bluetooth" -> connectBluetooth(dispositivo.direccion)
            "WiFi" -> connectWifi(dispositivo.direccion)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetooth(macAddress: String) {
        Toast.makeText(this, "Conectado vía Bluetooth: $macAddress", Toast.LENGTH_SHORT).show()
    }

    private fun connectWifi(ip: String, port: Int = 80) {
        Thread {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 2000)
                runOnUiThread {
                    Toast.makeText(this, "Conectado a $ip", Toast.LENGTH_SHORT).show()
                }
                socket.close()
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión WiFi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
