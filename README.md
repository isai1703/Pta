# 📱 Proyecto Pta-filtrado

Aplicación Android para controlar dispositivos como **ESP32, Raspberry Pi, STM32, Mini-PC embebidos y tarjetas personalizadas** mediante **WiFi** y **Bluetooth**.  
Compatible con detección automática de IP, lectura de `config.txt` y envío de comandos.

---

## 🚀 Características principales

- 🔎 Escaneo automático de red para detectar la IP del dispositivo.  
- 🌐 Conexión WiFi a través de `config.txt`.  
- 📡 Conexión Bluetooth directa con el dispositivo.  
- 🛠️ Compatible con múltiples dispositivos (ESP32, Raspberry Pi, STM32, Mini-PC, etc.).  
- 📊 Interfaz con catálogo de productos (RecyclerView).  
- 🔌 Envío de comandos para actuadores o sensores.  
- ✅ Estado de conexión en tiempo real (verde = conectado, rojo = desconectado).  

---

## ⚙️ Requisitos

- 📱 **Android 8.0 (Oreo)** o superior.  
- 🛠️ Android Studio **o** compilación en **GitHub Actions**.  
- 📂 Proyecto configurado con Gradle Kotlin DSL (`build.gradle.kts`).  
- 📡 Conexión WiFi o Bluetooth habilitada en el dispositivo Android.  

---

## 🛠️ Compilación

### 🔹 En Termux (local)
```bash
cd ~/Pta-filtrado
./gradlew assembleDebug
