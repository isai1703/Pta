package com.isai1703.pta.utils

// Extensiones de conversión
fun DeviceInfo.asNetDevice(): NetDevice = NetDevice(ip = this.ip, name = this.name, type = this.type)
fun NetDevice.asDeviceInfo(): DeviceInfo = DeviceInfo(ip = this.ip, type = this.type, name = this.name)
