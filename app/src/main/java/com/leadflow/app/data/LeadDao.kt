package com.leadflow.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    /** Empty query matches everything; otherwise matches business name OR address (area). */
    @Query(
        "SELECT * FROM leads " +
        "WHERE businessName LIKE '%' || :q || '%' OR address LIKE '%' || :q || '%' " +
        "ORDER BY createdAt DESC"
    )
    fun search(q: String): Flow<List<Lead>>

    @Query("SELECT COUNT(*) FROM leads")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM leads ORDER BY createdAt DESC")
    suspend fun getAll(): List<Lead>

    @Insert
    suspend fun insert(lead: Lead)

    @Delete
    suspend fun delete(lead: Lead)
}
