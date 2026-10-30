package com.leadflow.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE leads SET status = :status WHERE id = :leadId")
    suspend fun updateStatus(leadId: Long, status: String)

    @Query("UPDATE leads SET followUpDate = :date WHERE id = :leadId")
    suspend fun updateFollowUpDate(leadId: Long, date: String)

    @Query("SELECT COUNT(*) FROM leads")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM leads ORDER BY createdAt DESC")
    suspend fun getAll(): List<Lead>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM leads " +
        "WHERE LOWER(TRIM(businessName)) = LOWER(TRIM(:name)) " +
        "AND LOWER(TRIM(address)) = LOWER(TRIM(:address)) LIMIT 1)"
    )
    suspend fun existsByNameAndAddress(name: String, address: String): Boolean

    @Insert
    suspend fun insert(lead: Lead)

    @Transaction
    suspend fun insertIfAbsent(lead: Lead): Boolean {
        if (existsByNameAndAddress(lead.businessName, lead.address)) return false
        insert(lead)
        return true
    }

    @Delete
    suspend fun delete(lead: Lead)
}
