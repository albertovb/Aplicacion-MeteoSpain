// Consulta.kt
package com.example.meteospain.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "consultas",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Consulta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val province: String,
    val municipality: String,
    val date: Long,
    val result: Int,
    val favorite: Boolean = false
) {
    // funcion para la fecha
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(date))
    }
    fun getFormattedLocation(): String {
        return "$municipality, $province"
    }
}