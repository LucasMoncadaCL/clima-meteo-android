package com.ejemplo.climameteo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val Ink = Color(0xFF172554)
private val Muted = Color(0xFF64748B)
private val Blue = Color(0xFF315EEA)

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<City>>(emptyList()); private set
    var weather by mutableStateOf<Weather?>(null); private set
    var loading by mutableStateOf(false); private set
    var searching by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var searched by mutableStateOf(false); private set
    private var job: Job? = null
    init { select(City("Santiago", "Región Metropolitana", "Chile", -33.4569, -70.6483)) }
    fun updateQuery(value: String) { job?.cancel(); searching = false; loading = false; query = value; results = emptyList(); searched = false }
    fun search() {
        if (query.trim().length < 2) { error = "Escribe al menos dos letras para buscar."; return }
        job?.cancel(); job = viewModelScope.launch {
            searching = true; loading = false; error = null; searched = false
            try { results = repository.search(query.trim()); searched = true }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo buscar la ciudad. Comprueba tu conexión e inténtalo de nuevo." }
            finally { searching = false }
        }
    }
    fun select(city: City) {
        job?.cancel(); job = viewModelScope.launch {
            loading = true; searching = false; error = null; results = emptyList(); searched = false
            try { weather = repository.forecast(city); query = "" }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo cargar el clima. Comprueba tu conexión e inténtalo de nuevo." }
            finally { loading = false }
        }
    }
    fun refresh() { weather?.city?.let { select(it) } ?: select(City("Santiago", "Región Metropolitana", "Chile", -33.4569, -70.6483)) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { WeatherScreen() } } }
}
@Composable
fun WeatherScreen(vm: WeatherViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F7FE)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp)) {
        Text("CLIMA METEO", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text("Tu día, en un vistazo.", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = vm.query, onValueChange = vm::updateQuery, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Buscar ciudad") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(18.dp))
            Button(onClick = vm::search, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Buscar") }
        }
        if (vm.searching) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp)) }
        if (vm.results.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                vm.results.forEach { city -> Text(city.label, Modifier.fillMaxWidth().clickable { vm.select(city) }.padding(14.dp), color = Ink) }
            }
        } else if (vm.searched) Text("No encontramos ciudades con ese nombre.", Modifier.padding(top = 12.dp), color = Muted)
        vm.error?.let { message ->
            Text(message, Modifier.padding(top = 14.dp), color = Color(0xFFB42318))
            TextButton(onClick = vm::refresh) { Text("Recargar clima") }
        }
        if (vm.loading) { Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else vm.weather?.let { weather ->
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF315EEA), Color(0xFF728FF7))), RoundedCornerShape(28.dp)).padding(24.dp)) {
                Text("AHORA EN", color = Color(0xFFDEE8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(weather.city.name, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(listOf(weather.city.region, weather.city.country).filter { it.isNotBlank() }.distinct().joinToString(", "), color = Color(0xFFDEE8FF), fontSize = 14.sp)
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("${weather.temperature.roundToInt()}°", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, lineHeight = 76.sp); Text(weatherDescription(weather.code), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                    Text(weatherEmoji(weather.code, weather.isDay), fontSize = 58.sp)
                }
                Spacer(Modifier.height(22.dp))
                Text("Sensación ${weather.feelsLike.roundToInt()}°  •  Humedad ${weather.humidity}%  •  Viento ${weather.wind.roundToInt()} km/h", color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Próximos días", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("5 DÍAS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                weather.days.forEachIndexed { index, day ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        val label = if (index == 0) "Hoy" else LocalDate.parse(day.date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es" )).replaceFirstChar { it.uppercase() }
                        Text(label, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Medium)
                        Text(weatherEmoji(day.code), fontSize = 23.sp)
                        Text("${day.max.roundToInt()}°  /  ${day.min.roundToInt()}°", Modifier.width(105.dp), textAlign = TextAlign.End, color = Muted)
                    }
                    if (index < weather.days.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Color(0xFFF0F3F8))
                }
            }
            TextButton(onClick = vm::refresh, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Actualizar") }
            Text("Datos meteorológicos: Open-Meteo", Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
        }
    }
}

