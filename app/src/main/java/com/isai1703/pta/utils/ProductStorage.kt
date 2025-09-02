package com.isai1703.pta.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isai1703.pta.Producto
import java.io.File

object ProductStorage {
    private const val FILE_NAME = "products.json"

    fun saveProducts(context: Context, products: List<Producto>) {
        try {
            val json = Gson().toJson(products)
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json)
        } catch (_: Exception) { }
    }

    fun loadProducts(context: Context): MutableList<Producto> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return mutableListOf()
            val json = file.readText()
            val type = object : TypeToken<List<Producto>>() {}.type
            val list: List<Producto> = Gson().fromJson(json, type)
            list.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }
}
