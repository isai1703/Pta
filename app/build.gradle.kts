package com.isai1703.pta.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isai1703.pta.Producto
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object ProductStorage {

    private const val FILE_NAME = "products.json"

    fun saveProducts(context: Context, products: List<Producto>) {
        val gson = Gson()
        val json = gson.toJson(products)
        val file = File(context.filesDir, FILE_NAME)
        FileWriter(file).use {
            it.write(json)
        }
    }

    fun loadProducts(context: Context): List<Producto> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            return emptyList()
        }

        val gson = Gson()
        FileReader(file).use { reader ->
            val type = object : TypeToken<List<Producto>>() {}.type
            return gson.fromJson(reader, type)
        }
    }
}
