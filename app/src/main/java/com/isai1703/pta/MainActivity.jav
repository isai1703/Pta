package com.isai1703.pta;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ptafiltrado.BluetoothService;
import com.example.ptafiltrado.BluetoothDeviceDialog;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BluetoothService bluetoothService;
    private String connectedMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSendCommand = findViewById(R.id.btnSendCommand);

        bluetoothService = new BluetoothService(this);

        if (bluetoothService.isBluetoothSupported()) {
            bluetoothService.requestPermissions(this);

            if (!bluetoothService.isBluetoothEnabled()) {
                bluetoothService.enableBluetooth(this);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothService.getPairedDevices();

            String savedMac = BluetoothDeviceDialog.getSavedDeviceMac(this);
            if (savedMac == null) {
                if (pairedDevices != null && !pairedDevices.isEmpty()) {
                    BluetoothDeviceDialog dialog = new BluetoothDeviceDialog(this, pairedDevices, new BluetoothDeviceDialog.OnDeviceSelectedListener() {
                        @Override
                        public void onDeviceSelected(BluetoothDevice device) {
                            connectedMac = device.getAddress();
                            Toast.makeText(MainActivity.this, "ESP32 seleccionado: " + device.getName(), Toast.LENGTH_SHORT).show();
                            boolean connected = bluetoothService.connectToDevice(connectedMac);
                            if (connected) {
                                Toast.makeText(MainActivity.this, "Conectado a ESP32", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Error conectando al ESP32", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    dialog.show();
                } else {
                    Toast.makeText(this, "No hay dispositivos Bluetooth emparejados.", Toast.LENGTH_LONG).show();
                }
            } else {
                connectedMac = savedMac;
                boolean connected = bluetoothService.connectToDevice(connectedMac);
                if (connected) {
                    Toast.makeText(this, "Conectado automáticamente a ESP32 guardado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error conectando automáticamente al ESP32", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth no es compatible en este dispositivo.", Toast.LENGTH_LONG).show();
        }

        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectedMac != null) {
                    boolean sent = bluetoothService.sendCommand("COMANDO_DE_EJEMPLO");
                    if (sent) {
                        Toast.makeText(MainActivity.this, "Comando enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error enviando comando", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No conectado a ningún ESP32", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.closeConnection();
    }
}
