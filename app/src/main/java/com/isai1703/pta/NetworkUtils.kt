package com.isai1703.pta

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager

object NetworkUtils {
    fun obtenerIP(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiInfo = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
        val ip = wifiInfo.ipAddress
        return String.format("%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }
}
