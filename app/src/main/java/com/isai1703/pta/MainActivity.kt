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
import com.isai1703.pta.Device.TipoDispositivo
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val dispositivosCompatibles = mutableListOf<TipoDispositivo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        scanBluetoothDevices()
        scanWifiDevices()
        // RecyclerView productos, envío de comandos, historial y simulación
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
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
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
                val tipoDisp = when {
                    device.name.contains("esp32", true) -> TipoDispositivo.ESP32
                    device.name.contains("raspberry", true) -> TipoDispositivo.RASPBERRY
                    device.name.contains("stm32", true) -> TipoDispositivo.STM32
                    device.name.contains("minipc", true) -> TipoDispositivo.MINIPC
                    else -> TipoDispositivo.DESCONOCIDO
                }
                dispositivosCompatibles.add(tipoDisp)
            }
        }
        if (dispositivosCompatibles.isNotEmpty()) showDeviceListDialog()
    }

    private fun isCompatibleBluetooth(device: BluetoothDevice) =
        device.name?.lowercase()?.contains("esp32") == true ||
        device.name?.lowercase()?.contains("raspberry") == true ||
        device.name?.lowercase()?.contains("stm32") == true ||
        device.name?.lowercase()?.contains("minipc") == true

    private fun scanWifiDevices() {
        # Aquí puedes agregar escaneo de IPs en la red
    }

    private fun showDeviceListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_device_list, null)
        val listView: ListView = dialogView.findViewById(R.id.deviceListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dispositivosCompatibles.map { it.name })
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.device_list_title))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            connectToDevice(dispositivosCompatibles[position])
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun connectToDevice(dispositivo: TipoDispositivo) {
        Toast.makeText(this, "Conectando a ${dispositivo.name}...", Toast.LENGTH_SHORT).show()
        when(dispositivo){
            TipoDispositivo.ESP32 -> connectWifi("192.168.1.50")
            TipoDispositivo.RASPBERRY -> connectWifi("192.168.1.51")
            TipoDispositivo.STM32 -> connectBluetooth("00:11:22:33:44:55")
            TipoDispositivo.MINIPC -> connectBluetooth("66:77:88:99:AA:BB")
            TipoDispositivo.DESCONOCIDO -> Toast.makeText(this,"Dispositivo desconocido",Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectBluetooth(mac: String) = Toast.makeText(this,"Conectado vía Bluetooth: $mac", Toast.LENGTH_SHORT).show()

    private fun connectWifi(ip: String, port:Int=80){
        Thread {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip,port),2000)
                runOnUiThread { Toast.makeText(this,"Conectado a $ip",Toast.LENGTH_SHORT).show() }
                socket.close()
            }catch (e:IOException){
                runOnUiThread { Toast.makeText(this,"Error de conexión WiFi: ${e.message}",Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

}
