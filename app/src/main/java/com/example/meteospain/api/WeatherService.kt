// WeatherService.kt
package com.example.meteospain.api

import android.content.Context
import android.util.Log
import com.example.meteospain.data.AppDatabase
import com.example.meteospain.data.Consulta
import com.example.meteospain.data.WeatherData
import com.example.meteospain.data.HourlyPrediction
import com.example.meteospain.data.DailyPrediction
import com.example.meteospain.data.Province
import com.example.meteospain.data.Municipality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class WeatherService(private val context: Context, private val db: AppDatabase) {
    private val apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGJlcnRvdmIwMEBnbWFpbC5jb20iLCJqdGkiOiI2MDBlYjYyMC1kY2EyLTQxMzItOTg4Mi1jYzdkMTI5NTM2ZWEiLCJpc3MiOiJBRU1FVCIsImlhdCI6MTc0MzA3Mjg1OSwidXNlcklkIjoiNjAwZWI2MjAtZGNhMi00MTMyLTk4ODItY2M3ZDEyOTUzNmVhIiwicm9sZSI6IiJ9.mfkwqTSijTKh4aAXSCLlSibzgSWYgMaaT3YBNLDzYdw"
    private val baseUrl = "https://opendata.aemet.es/opendata/api"

    private val _loadedProvinces by lazy { loadProvinces() }
    private val _loadedMunicipalities by lazy { loadMunicipalities() }

    private fun loadProvinces(): List<Province> {
        return try {
            val json = context.assets.open("provinces.json").bufferedReader().use { it.readText() }
            JSONArray(json).let { array ->
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    Province(
                        CPRO = obj.getInt("CPRO"),
                        Provincia = obj.getString("Provincia")
                    )
                }
            }.sortedBy { it.Provincia }
        } catch (e: Exception) {
            Log.e("WeatherService", "Error al cargar las provincias", e)
            emptyList()
        }
    }

    private fun loadMunicipalities(): List<Municipality> {
        return try {
            val json = context.assets
                .open("diccionario24_modificado.json")
                .bufferedReader()
                .use { it.readText() }
            JSONArray(json).let { array ->
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    Municipality(
                        CPRO = obj.getInt("CPRO"),
                        CMUN = obj.getInt("CMUN"),
                        NOMBRE = obj.getString("NOMBRE")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Error cargando municipios", e)
            emptyList()
        }
    }

    fun getMunicipalitiesForProvince(cpro: Int): List<Municipality> {
        return _loadedMunicipalities
            .filter { it.CPRO == cpro }
            .sortedBy { it.NOMBRE }
            .ifEmpty {
                Log.w("WeatherService", "Ningun municipio encontrado en $cpro")
                emptyList()
            }
    }

    fun getTownCode(province: Province, municipality: Municipality): String {
        return "${province.CPRO.toString().padStart(2, '0')}" +
                "${municipality.CMUN.toString().padStart(3, '0')}"
    }

    fun getProvinces(): List<Province> = _loadedProvinces.ifEmpty {
        Log.w("WeatherService", "Provincias vacias")
        emptyList()
    }

    suspend fun getWeatherData(townCode: String): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val initialUrl = "$baseUrl/prediccion/especifica/municipio/horaria/$townCode?api_key=$apiKey"
            Log.d("WeatherService", "Primera solicitud: $initialUrl")

            val (status1, response1) = makeApiRequest(initialUrl)

            if (status1 != 200) {
                return@withContext Result.failure(Exception("Error API inicial: $status1"))
            }

            val dataUrl = try {
                JSONObject(response1).getString("datos").also {
                    Log.d("WeatherService", "URL de los datos procesados: $it")
                }
            } catch (e: Exception) {
                Log.e("WeatherService", "Error al analizar datos de la URL", e)
                return@withContext Result.failure(Exception("Formato de respuesta inválido"))
            }

            Log.d("WeatherService", "Obteniendo datos de: $dataUrl")
            val (status2, response2) = makeApiRequest(dataUrl)

            if (status2 != 200) {
                return@withContext Result.failure(Exception("Error en datos: $status2"))
            }

            parseWeatherData(townCode, response2)
        } catch (e: Exception) {
            Log.e("WeatherService", "Error general: ${e.message}", e)
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
        }
    }

    private fun makeApiRequest(urlString: String): Pair<Int, String> {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            val content = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }

            Log.d("WeatherService", "Respuesta de la API [$responseCode] desde $urlString")
            connection.disconnect()

            responseCode to content
        } catch (e: Exception) {
            Log.e("WeatherService", "Error de conexion: ${e.javaClass.simpleName} - ${e.message}")
            0 to "Connection failed: ${e.localizedMessage}"
        }
    }

    private fun parseWeatherData(townCode: String, rawData: String): Result<WeatherData> {
        return try {
            val allDailyPredictions = mutableListOf<DailyPrediction>()
            val jsonArray = JSONArray(rawData)

            val daysArray = jsonArray.getJSONObject(0)
                .getJSONObject("prediccion")
                .getJSONArray("dia")

            for (d in 0 until daysArray.length()) {
                val day = daysArray.getJSONObject(d)
                val apiDate = day.getString("fecha")

                val hourlyPredictions = mutableListOf<HourlyPrediction>().apply {
                    // guardamos todos los arrays
                    val skyStates = day.optJSONArray("estadoCielo") ?: JSONArray()
                    val temperatures = day.optJSONArray("temperatura") ?: JSONArray()
                    val humidity = day.optJSONArray("humedadRelativa") ?: JSONArray()
                    val wind = day.optJSONArray("vientoAndRachaMax") ?: JSONArray()
                    val precipitation = day.optJSONArray("precipitacion") ?: JSONArray()

                    // index
                    val maxIndex = listOf(
                        skyStates.length() - 1,
                        temperatures.length() - 1,
                        humidity.length() - 1,
                        wind.length() - 1,
                        precipitation.length() - 1
                    ).minOrNull() ?: 0

                    for (i in 0..maxIndex) {
                        try {
                            val sky = skyStates.optJSONObject(i) ?: continue
                            val temp = temperatures.optJSONObject(i) ?: continue
                            val hum = humidity.optJSONObject(i) ?: continue
                            val windData = wind.optJSONObject(i) ?: continue
                            val precip = precipitation.optJSONObject(i) ?: continue

                            val rawHour = sky.optString("periodo", "")
                            val formattedHour = when {
                                rawHour.length >= 2 -> "${rawHour.substring(0, 2)}:00"
                                else -> "00:00"
                            }

                            val windSpeed = try {
                                windData.optJSONArray("velocidad")?.getDouble(0) ?: 0.0
                            } catch (e: Exception) {
                                0.0
                            }

                            add(
                                HourlyPrediction(
                                    hour = formattedHour,
                                    temperature = temp.optDouble("value", 0.0),
                                    humidity = hum.optInt("value", 0),
                                    windSpeed = windSpeed,
                                    precipitation = precip.optInt("value", 0),
                                    description = sky.optString("descripcion", "")
                                        .replaceFirstChar { it.titlecase() }
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("WeatherService", "Skipping hour $i", e)
                        }
                    }
                }

                if (hourlyPredictions.isNotEmpty()) {
                    allDailyPredictions.add(
                        DailyPrediction(
                            date = formatDateLegacy(apiDate),
                            hourlyPredictions = hourlyPredictions
                        )
                    )
                }
            }

            if (allDailyPredictions.isEmpty()) {
                Result.failure(Exception("No se encontraron datos meteorológicos válidos"))
            } else {
                Result.success(WeatherData(townCode, allDailyPredictions))
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Parsing error", e)
            Result.failure(Exception("Error en formato de datos de la API"))
        }
    }

    private fun formatDateLegacy(apiDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
            val parsedDate = inputFormat.parse(apiDate)
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                apiDate
            }
        } catch (e: Exception) {
            apiDate
        }
    }
    suspend fun saveConsulta(
        userId: Int,
        province: String,
        municipality: String,
        cmun: Int
    ) {
        val consulta = Consulta(
            userId = userId,
            province = province,
            municipality = municipality,
            date = System.currentTimeMillis(),
            result = cmun
        )

        withContext(Dispatchers.IO) {
            db.consultaDao().insert(consulta)
        }
    }
}