package com.ejemplo.climameteo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class City(val name: String, val region: String, val country: String, val latitude: Double, val longitude: Double) {
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}
data class Day(val date: String, val code: Int, val max: Double, val min: Double)
data class Weather(val city: City, val temperature: Double, val feelsLike: Double, val humidity: Int, val wind: Double, val code: Int, val isDay: Boolean, val days: List<Day>)

class WeatherRepository {
    private suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (connection.responseCode !in 200..299) error("El servidor respondió con un error (${connection.responseCode}).")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
    suspend fun search(query: String): List<City> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val results = get("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=es&format=json").optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i -> results.getJSONObject(i).let {
            City(it.getString("name"), it.optString("admin1", ""), it.optString("country", ""), it.getDouble("latitude"), it.getDouble("longitude"))
        } }
    }
    suspend fun forecast(city: City): Weather {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=5"
        val json = get(url); val current = json.getJSONObject("current"); val daily = json.getJSONObject("daily")
        val dates = daily.getJSONArray("time"); val codes = daily.getJSONArray("weather_code"); val max = daily.getJSONArray("temperature_2m_max"); val min = daily.getJSONArray("temperature_2m_min")
        val days = (0 until dates.length()).map { Day(dates.getString(it), codes.getInt(it), max.getDouble(it), min.getDouble(it)) }
        return Weather(city, current.getDouble("temperature_2m"), current.getDouble("apparent_temperature"), current.getInt("relative_humidity_2m"), current.getDouble("wind_speed_10m"), current.getInt("weather_code"), current.getInt("is_day") == 1, days)
    }
}
fun weatherDescription(code: Int): String = when (code) {
    0 -> "Despejado"; 1, 2 -> "Parcialmente nublado"; 3 -> "Nublado"; 45, 48 -> "Niebla"; 51, 53, 55, 56, 57 -> "Llovizna"; 61, 63, 65, 66, 67 -> "Lluvia"; 71, 73, 75, 77 -> "Nieve"; 80, 81, 82 -> "Chubascos"; 85, 86 -> "Chubascos de nieve"; 95, 96, 99 -> "Tormenta"; else -> "Tiempo variable"
}
fun weatherEmoji(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"; 1, 2 -> if (isDay) "🌤️" else "☁️"; 3 -> "☁️"; 45, 48 -> "🌫️"; in 51..67, in 80..82 -> "🌧️"; in 71..77, 85, 86 -> "🌨️"; 95, 96, 99 -> "⛈️"; else -> "🌥️"
}
