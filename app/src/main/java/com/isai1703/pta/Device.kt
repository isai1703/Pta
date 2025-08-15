package com.isai1703.pta

sealed class TipoDispositivo(val nombre: String, val direccion: String) {
    class ESP32(nombre: String, direccion: String) : TipoDispositivo(nombre, direccion)
    class RASPBERRY(nombre: String, direccion: String) : TipoDispositivo(nombre, direccion)
    class MINIPC(nombre: String, direccion: String) : TipoDispositivo(nombre, direccion)
}
