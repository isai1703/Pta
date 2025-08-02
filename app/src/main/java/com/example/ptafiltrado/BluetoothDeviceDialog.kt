package com.example.ptafiltrado

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences

class BluetoothDeviceDialog(
    private val context: Context,
    private val devices: Set<BluetoothDevice>,
    private val onDeviceSelected: (BluetoothDevice) -> Unit
) {
    fun show() {
        val deviceList = devices.map { "${it.name} - ${it.address}" }
        val deviceArray = deviceList.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Selecciona el ESP32")
            .setItems(deviceArray) { _, which ->
                val selectedDevice = devices.elementAt(which)
                saveSelectedDevice(selectedDevice)
                onDeviceSelected(selectedDevice)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveSelectedDevice(device: BluetoothDevice) {
        val prefs: SharedPreferences =
            context.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("esp32_mac", device.address)
            .apply()
    }

    companion object {
        fun getSavedDeviceMac(context: Context): String? {
            val prefs = context.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE)
            return prefs.getString("esp32_mac", null)
        }
    }
}
