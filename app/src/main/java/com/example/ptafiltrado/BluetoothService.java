package com.example.ptafiltrado;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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

    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter != null ? bluetoothAdapter.getBondedDevices() : null;
    }

    public boolean connectToDevice(String macAddress) {
        if (bluetoothAdapter == null || macAddress == null) return false;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(ESP32_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Log.e("BluetoothService", "Error connecting", e);
            return false;
        }
    }

    public boolean sendCommand(String command) {
        if (outputStream == null) return false;

        try {
            outputStream.write(command.getBytes());
            return true;
        } catch (IOException e) {
            Log.e("BluetoothService", "Error sending command", e);
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("BluetoothService", "Error closing connection", e);
        }
    }
}
