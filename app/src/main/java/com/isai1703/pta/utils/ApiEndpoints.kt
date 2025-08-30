package com.isai1703.pta.utils

object ApiEndpoints {
    const val PING = "/ping"
    const val STATUS = "/status"
    const val COMMAND = "/command"
    const val HISTORY = "/history"
    const val CONFIG = "/config"

    fun build(ip: String, endpoint: String): String {
        return "http://$ip$endpoint"
    }
}
