#!/bin/bash
echo "📂 Usando proyecto en: $PWD"

# 1. Actualizar repos
git fetch origin
git pull origin main

# 2. Limpiar proyecto
./gradlew clean

# 3. Compilar APK debug
./gradlew assembleDebug

# 4. Subir cambios a GitHub
git add .
git commit -m "Actualización completa multi-dispositivo + agregar productos + imágenes desde galería + escaneo con progreso + NetworkUtils actualizado + workflow v5"
git push origin main

echo "✅ Script finalizado. APK generado en: app/build/outputs/apk/debug/app-debug.apk"
