package com.leadflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One saved business lead (one row in the "leads" table). */
@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessName: String,
    val address: String,
    val phone: String,          // 10-digit Indian mobile, without +91
    val notes: String,
    val status: String = "New",
    val createdAt: Long = System.currentTimeMillis()
)
