package com.example.ptafiltrado;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    // UUID estándar para SPP (Serial Port Profile)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothService(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void enableBluetooth(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, 1);
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return bluetoothAdapter.getBondedDevices();
    }

    public void requestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    100);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
    }

    // Conectar a dispositivo por MAC (debe estar emparejado)
    public boolean connectToDevice(String macAddress) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BluetoothService", "Bluetooth no habilitado");
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Log.i("BluetoothService", "Conectado a " + macAddress);
            return true;
        } catch (IOException e) {
            Log.e("BluetoothService", "Error conectando: " + e.getMessage());
            closeConnection();
            return false;
        }
    }

    // Enviar comando (string) al dispositivo
    public boolean sendCommand(String command) {
        if (outputStream == null) {
            Log.e("BluetoothService", "No hay conexión abierta para enviar datos");
            return false;
        }

        try {
            outputStream.write(command.getBytes());
            outputStream.flush();
            Log.i("BluetoothService", "Comando enviado: " + command);
            return true;
        } catch (IOException e) {
            Log.e("BluetoothService", "Error enviando comando: " + e.getMessage());
            return false;
        }
    }

    // Cerrar conexión Bluetooth
    public void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
            Log.i("BluetoothService", "Conexión cerrada");
        } catch (IOException e) {
            Log.e("BluetoothService", "Error cerrando conexión: " + e.getMessage());
        }
    }
}
