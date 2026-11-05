package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.local.RoommateDao
import com.example.data.local.SettlementDao
import com.example.data.model.Expense
import com.example.data.model.Roommate
import com.example.data.model.Settlement
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val roommateDao: RoommateDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao
) {
    // Roommates
    val allRoommates: Flow<List<Roommate>> = roommateDao.getAllRoommates()
    suspend fun getRoommateById(id: Long): Roommate? = roommateDao.getRoommateById(id)
    suspend fun getRoommateCount(): Int = roommateDao.getRoommateCount()
    suspend fun insertRoommate(roommate: Roommate): Long = roommateDao.insertRoommate(roommate)
    suspend fun insertRoommates(roommates: List<Roommate>) = roommateDao.insertAll(roommates)
    suspend fun updateRoommate(roommate: Roommate) = roommateDao.updateRoommate(roommate)
    suspend fun deleteRoommate(roommate: Roommate) = roommateDao.deleteRoommate(roommate)

    // Expenses
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertAll(expenses)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Settlements
    val allSettlements: Flow<List<Settlement>> = settlementDao.getAllSettlements()
    suspend fun insertSettlement(settlement: Settlement): Long = settlementDao.insertSettlement(settlement)
    suspend fun deleteSettlement(settlement: Settlement) = settlementDao.deleteSettlement(settlement)
}
