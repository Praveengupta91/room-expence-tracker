package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = ExpenseCategory.OTHER.name,
    val paidByRoommateId: Long,
    val splitBetweenRoommateIds: List<Long>,
    val splitType: String = "EQUAL",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    /**
     * Share amount per person in split
     */
    val perPersonShare: Double
        get() = if (splitBetweenRoommateIds.isNotEmpty()) amount / splitBetweenRoommateIds.size else 0.0
}
