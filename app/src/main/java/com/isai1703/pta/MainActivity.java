package com.isai1703.pta;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private BluetoothService bluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothService = new BluetoothService(this);

        if (!bluetoothService.isBluetoothSupported()) {
            Toast.makeText(this, "Bluetooth no es compatible", Toast.LENGTH_LONG).show();
            return;
        }

        bluetoothService.requestPermissions(this);

        if (!bluetoothService.isBluetoothEnabled()) {
            bluetoothService.enableBluetooth(this);
        }
    }
}
