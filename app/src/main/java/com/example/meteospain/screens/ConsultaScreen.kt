// ConsultaScreen.kt
package com.example.meteospain.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.meteospain.api.WeatherService
import com.example.meteospain.data.AppDatabase
import com.example.meteospain.data.Consulta
import com.example.meteospain.data.DailyPrediction
import com.example.meteospain.data.HourlyPrediction
import com.example.meteospain.data.Municipality
import com.example.meteospain.data.Province
import com.example.meteospain.data.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConsultaScreen(currentUserId: Int,
                   initialProvince: String? = null,
                   initialCMUN: Int? = null) {
    val context = LocalContext.current
    if (currentUserId == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Usuario no autenticado")
        }
        return
    }
    val db = remember {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java, "user-database"
        ).build()
    }
    val weatherService = remember { WeatherService(context, db) }
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedMunicipality by remember { mutableStateOf<Municipality?>(null) }
    var showProvinces by remember { mutableStateOf(false) }
    var showMunicipalities by remember { mutableStateOf(false) }
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var latestConsulta by remember { mutableStateOf<Consulta?>(null) }
    var isViewingExistingConsulta by remember { mutableStateOf(false) }
    var existingConsulta by remember { mutableStateOf<Consulta?>(null) }

    LaunchedEffect(initialProvince, initialCMUN) {
        if (initialProvince != null && initialCMUN != null) {
            withContext(Dispatchers.IO) {

                val consultas = db.consultaDao()
                    .getAllConsultasByUser(currentUserId)
                    .firstOrNull() ?: emptyList()

                existingConsulta = consultas.find { cons: Consulta ->
                    cons.province == initialProvince && cons.result == initialCMUN
                }
            }

            existingConsulta?.let { cons ->
                latestConsulta = cons

                selectedProvince = weatherService.getProvinces().find { it.Provincia == cons.province }

                selectedProvince?.let { province ->
                    selectedMunicipality = weatherService.getMunicipalitiesForProvince(province.CPRO)
                        .find { mun: Municipality ->
                            mun.CMUN == cons.result
                        }
                }

                // cargar data
                selectedProvince?.let { prov ->
                    selectedMunicipality?.let { mun ->
                        weatherService.getWeatherData(
                            weatherService.getTownCode(prov, mun)
                        ).onSuccess { weatherData = it }
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val targetConsulta = existingConsulta ?: latestConsulta
        targetConsulta?.let { cons ->
            coroutineScope.launch {
                val updated = cons.copy(favorite = !cons.favorite)
                withContext(Dispatchers.IO) {
                    db.consultaDao().update(updated)
                }
                latestConsulta = updated
                existingConsulta = updated
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Seleccion de la provincia
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            val provinceText = selectedProvince?.Provincia ?: "Selecciona una provincia"

            OutlinedButton(
                onClick = { showProvinces = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(provinceText)
            }

            DropdownMenu(
                expanded = showProvinces,
                onDismissRequest = { showProvinces = false },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
            ) {
                weatherService.getProvinces().forEach { province ->
                    DropdownMenuItem(
                        text = { Text(province.Provincia) },
                        onClick = {
                            selectedProvince = province
                            selectedMunicipality = null
                            showProvinces = false
                            showMunicipalities = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Seleccion del pueblo/municipio
        Box(modifier = Modifier.fillMaxWidth()) {
            val municipalityText = selectedMunicipality?.NOMBRE ?: "Selecciona un municipio"

            OutlinedButton(
                onClick = { showMunicipalities = true },
                enabled = selectedProvince != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(municipalityText)
            }

            DropdownMenu(
                expanded = showMunicipalities,
                onDismissRequest = { showMunicipalities = false },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
            ) {
                selectedProvince?.let { province ->
                    weatherService.getMunicipalitiesForProvince(province.CPRO).forEach { mun ->
                        DropdownMenuItem(
                            text = { Text(mun.NOMBRE) },
                            onClick = {
                                selectedMunicipality = mun
                                showMunicipalities = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (isViewingExistingConsulta) return@Button
                selectedProvince?.let { province ->
                    selectedMunicipality?.let { mun ->
                        coroutineScope.launch {
                            isLoading = true
                            error = null
                            weatherService.getWeatherData(
                                weatherService.getTownCode(province, mun)
                            ).onSuccess { weatherDataResult ->
                                weatherData = weatherDataResult

                                // nueva consulta
                                val newConsulta = Consulta(
                                    userId = currentUserId,
                                    province = province.Provincia,
                                    municipality = mun.NOMBRE,
                                    date = System.currentTimeMillis(),
                                    result = mun.CMUN
                                )

                                val insertedId = withContext(Dispatchers.IO) {
                                    db.consultaDao().insert(newConsulta)
                                }

                                val fullConsulta = withContext(Dispatchers.IO) {
                                    db.consultaDao().getConsultaById(insertedId)
                                } ?: throw Exception("error al recibir la consulta")

                                latestConsulta = fullConsulta
                                existingConsulta = fullConsulta
                            }.onFailure {
                                error = it.message
                            }
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = if (isViewingExistingConsulta) false
            else (selectedProvince != null && selectedMunicipality != null)
        ) {
            Text(if (isViewingExistingConsulta) "Viendo consulta" else "Consultar")
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        weatherData?.let { data ->
            latestConsulta?.let { cons ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Consulta: ${cons.province}", modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { toggleFavorite() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (latestConsulta?.favorite == true) Icons.Filled.Star
                            else Icons.Outlined.Star,
                            contentDescription = "Favorito",
                            tint = if (latestConsulta?.favorite == true)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(data.dailyPredictions) { daily ->
                    DailyForecastSection(dailyPrediction = daily)
                }
            }
        }
    }
}

@Composable
private fun DailyForecastSection(dailyPrediction: DailyPrediction) {
    val validPredictions = dailyPrediction.hourlyPredictions
        .filter { it.hour.isNotBlank() && it.description.isNotBlank() }

    if (validPredictions.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Pronóstico para el ${dailyPrediction.date}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(validPredictions) { prediction ->
                WeatherCard(prediction = prediction)
            }
        }
    }
}

@Composable
fun WeatherCard(prediction: HourlyPrediction) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prediction.hour,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${prediction.temperature}°C", style = MaterialTheme.typography.bodyLarge)
            Text("Humedad: ${prediction.humidity}%")
            Text("Viento: ${"%.1f".format(prediction.windSpeed)} km/h")
            Text("Lluvia: ${prediction.precipitation}%")
            Text(
                text = prediction.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}