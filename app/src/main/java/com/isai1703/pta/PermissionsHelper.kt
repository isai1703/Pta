package com.isai1703.pta

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsHelper {
    private const val REQ = 101

    fun ensure(activity: Activity) {
        val needs = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needs += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needs += Manifest.permission.BLUETOOTH_SCAN
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needs += Manifest.permission.BLUETOOTH_CONNECT
            }
        }
        if (needs.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, needs.toTypedArray(), REQ)
        }
    }
}
