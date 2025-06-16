// ConsultaDao.kt
package com.example.meteospain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultaDao {
    @Insert
    suspend fun insert(consulta: Consulta): Long

    @Update
    suspend fun update(consulta: Consulta)

    @Query("SELECT * FROM consultas WHERE userId = :userId ORDER BY date DESC")
    fun getConsultasByUser(userId: Int): Flow<List<Consulta>>

    @Query("SELECT * FROM consultas WHERE id = :id")
    suspend fun getConsultaById(id: Long): Consulta?

    @Query("SELECT * FROM consultas WHERE userId = :userId AND favorite = 1")
    fun getFavoriteConsultas(userId: Int): Flow<List<Consulta>>

    @Query("SELECT * FROM consultas WHERE userId = :userId ORDER BY date DESC LIMIT 5")
    fun getRecentConsultas(userId: Int): Flow<List<Consulta>>

    @Query("SELECT * FROM consultas WHERE userId = :userId ORDER BY date DESC")
    fun getAllConsultasByUser(userId: Int): Flow<List<Consulta>>
}