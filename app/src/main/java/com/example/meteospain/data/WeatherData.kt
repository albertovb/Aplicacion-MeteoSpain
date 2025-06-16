// WeatherData.kt
package com.example.meteospain.data

data class WeatherData(
    val city: String,
    val dailyPredictions: List<DailyPrediction>
)

data class DailyPrediction(
    val date: String,  // Formato "YYYY-MM-DD"
    val hourlyPredictions: List<HourlyPrediction>
)

data class HourlyPrediction(
    val hour: String,       // Formato "08:00"
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Int,
    val description: String
)