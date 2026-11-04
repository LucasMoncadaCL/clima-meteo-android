# Clima Meteo ☀️

Una aplicación Android en Kotlin y Jetpack Compose que consulta Open-Meteo. Busca ciudades, muestra las condiciones actuales y un pronóstico de cinco días. Se inicia con Santiago de Chile; puedes buscar otra ciudad y elegir entre los resultados.

## Abrir y ejecutar

1. Descomprime el proyecto si descargaste el ZIP. En Android Studio elige **Open** y selecciona la carpeta `ClimaMeteo` (la que contiene `settings.gradle.kts`).
2. Deja que Gradle sincronice y acepta la instalación del SDK 35 si Android Studio la solicita. Usa JDK 17 (el JDK integrado de Android Studio suele funcionar).
3. Crea o inicia un emulador Android con API 26 o superior, o conecta un dispositivo con depuración USB. Pulsa **Run ▶**.
4. Necesitas conexión a Internet para descargar dependencias y consultar el clima. No se necesita clave de API para este uso no comercial; revisa las condiciones de Open-Meteo para otros usos.

## Cómo funciona

- `MainActivity.kt`: interfaz Compose y `WeatherViewModel`, que administra carga, búsqueda y errores.
- `WeatherRepository.kt`: consulta la API de geocodificación para convertir una ciudad en coordenadas y luego la API de pronóstico para obtener el clima. Usa corrutinas para evitar bloquear la interfaz.
- `AndroidManifest.xml`: declara el permiso de Internet.

Los datos provienen de [Open-Meteo](https://open-meteo.com/). Consulta su [documentación de geocodificación](https://open-meteo.com/en/docs/geocoding-api) y [pronóstico](https://open-meteo.com/en/docs).
