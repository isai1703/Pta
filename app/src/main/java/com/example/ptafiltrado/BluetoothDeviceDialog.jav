package com.example.ptafiltrado;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothDeviceDialog {

    private Context context;
    private Set<BluetoothDevice> devices;
    private OnDeviceSelectedListener listener;

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    public BluetoothDeviceDialog(Context context, Set<BluetoothDevice> devices, OnDeviceSelectedListener listener) {
        this.context = context;
        this.devices = devices;
        this.listener = listener;
    }

    public void show() {
        if (devices == null || devices.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Dispositivos Bluetooth")
                    .setMessage("No hay dispositivos emparejados disponibles.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        final ArrayList<String> deviceList = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            deviceList.add(device.getName() + " - " + device.getAddress());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Selecciona el ESP32")
                .setItems(deviceList.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothDevice selectedDevice = (BluetoothDevice) devices.toArray()[which];
                        saveSelectedDevice(selectedDevice);
                        if (listener != null) {
                            listener.onDeviceSelected(selectedDevice);
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveSelectedDevice(BluetoothDevice device) {
        SharedPreferences prefs = context.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("esp32_mac", device.getAddress())
                .apply();
    }

    public static String getSavedDeviceMac(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE);
        return prefs.getString("esp32_mac", null);
    }
}
