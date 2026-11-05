package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val payerId: Long,
    val payeeId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
