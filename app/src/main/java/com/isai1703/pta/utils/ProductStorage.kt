package com.isai1703.pta.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isai1703.pta.Producto
import java.io.File

object ProductStorage {
    private const val FILE_NAME = "products.json"

    fun save(ctx: Context, productos: List<Producto>) {
        try {
            val json = Gson().toJson(productos)
            File(ctx.filesDir, FILE_NAME).writeText(json)
        } catch (_: Exception) { /* opcional: log */ }
    }

    fun load(ctx: Context): List<Producto> {
        return try {
            val f = File(ctx.filesDir, FILE_NAME)
            if (!f.exists()) return emptyList()
            val json = f.readText()
            val type = object : TypeToken<List<Producto>>() {}.type
            Gson().fromJson<List<Producto>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
