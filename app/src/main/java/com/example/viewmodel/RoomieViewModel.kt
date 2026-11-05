package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Roommate
import com.example.data.model.Settlement
import com.example.data.repository.ExpenseRepository
import com.example.domain.CategorySpend
import com.example.domain.DebtCalculator
import com.example.domain.RoommateBalance
import com.example.domain.RoommateSpend
import com.example.domain.SimplifiedDebt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChartMode {
    BY_CATEGORY,
    BY_ROOMMATE
}

data class RoomieUiState(
    val isLoading: Boolean = true,
    val roommates: List<Roommate> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val balances: List<RoommateBalance> = emptyList(),
    val simplifiedDebts: List<SimplifiedDebt> = emptyList(),
    val totalExpensesAmount: Double = 0.0,
    val monthlyExpensesAmount: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val roommateBreakdown: List<RoommateSpend> = emptyList(),
    val chartMode: ChartMode = ChartMode.BY_CATEGORY,
    val searchQuery: String = "",
    val categoryFilter: ExpenseCategory? = null,
    val currencySymbol: String = "₹"
)

class RoomieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    private val _chartMode = MutableStateFlow(ChartMode.BY_CATEGORY)
    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    private val _currencySymbol = MutableStateFlow("₹")

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(
            roommateDao = db.roommateDao(),
            expenseDao = db.expenseDao(),
            settlementDao = db.settlementDao()
        )
        viewModelScope.launch {
            seedSampleDataIfEmpty()
        }
    }

    val uiState: StateFlow<RoomieUiState> = combine(
        repository.allRoommates,
        repository.allExpenses,
        repository.allSettlements,
        _chartMode,
        combine(_searchQuery, _categoryFilter, _currencySymbol) { query, cat, cur ->
            Triple(query, cat, cur)
        }
    ) { roommates, expenses, settlements, chartMode, (query, catFilter, currency) ->
        val balances = DebtCalculator.calculateRoommateBalances(roommates, expenses, settlements)
        val debts = DebtCalculator.simplifyDebts(roommates, expenses, settlements)
        val monthlyExpenses = DebtCalculator.filterCurrentMonthExpenses(expenses)
        val totalMonthlyAmount = monthlyExpenses.sumOf { it.amount }
        val totalAllAmount = expenses.sumOf { it.amount }
        val categoryBreakdown = DebtCalculator.calculateCategoryBreakdown(expenses)
        val roommateBreakdown = DebtCalculator.calculateRoommateSpendBreakdown(roommates, expenses)

        RoomieUiState(
            isLoading = false,
            roommates = roommates,
            expenses = expenses,
            settlements = settlements,
            balances = balances,
            simplifiedDebts = debts,
            totalExpensesAmount = totalAllAmount,
            monthlyExpensesAmount = totalMonthlyAmount,
            categoryBreakdown = categoryBreakdown,
            roommateBreakdown = roommateBreakdown,
            chartMode = chartMode,
            searchQuery = query,
            categoryFilter = catFilter,
            currencySymbol = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoomieUiState()
    )

    fun setChartMode(mode: ChartMode) {
        _chartMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: ExpenseCategory?) {
        _categoryFilter.value = category
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        paidById: Long,
        splitBetweenIds: List<Long>,
        date: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        addExpenseWithCustomCategory(
            title = title,
            amount = amount,
            categoryName = category.name,
            paidById = paidById,
            splitBetweenIds = splitBetweenIds,
            splitType = "EQUAL",
            date = date,
            notes = notes
        )
    }

    fun addExpenseWithCustomCategory(
        title: String,
        amount: Double,
        categoryName: String,
        paidById: Long,
        splitBetweenIds: List<Long>,
        splitType: String = "EQUAL",
        date: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                category = categoryName.trim().ifEmpty { ExpenseCategory.OTHER.name },
                paidByRoommateId = paidById,
                splitBetweenRoommateIds = if (splitBetweenIds.isEmpty()) listOf(paidById) else splitBetweenIds,
                splitType = splitType,
                date = date,
                notes = notes.trim()
            )
            repository.insertExpense(expense)
        }
    }

    suspend fun createRoommateAndGetId(
        name: String,
        email: String = "",
        colorHex: String = "#3B82F6"
    ): Long {
        val roommate = Roommate(
            name = name.trim(),
            email = email.trim(),
            colorHex = colorHex
        )
        return repository.insertRoommate(roommate)
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addRoommate(name: String, email: String = "", colorHex: String = "#3B82F6") {
        viewModelScope.launch {
            val roommate = Roommate(
                name = name.trim(),
                email = email.trim(),
                colorHex = colorHex
            )
            repository.insertRoommate(roommate)
        }
    }

    fun updateRoommate(roommate: Roommate) {
        viewModelScope.launch {
            repository.updateRoommate(roommate)
        }
    }

    fun deleteRoommate(roommate: Roommate) {
        viewModelScope.launch {
            repository.deleteRoommate(roommate)
        }
    }

    fun settleUp(
        payerId: Long,
        payeeId: Long,
        amount: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val settlement = Settlement(
                payerId = payerId,
                payeeId = payeeId,
                amount = amount,
                date = System.currentTimeMillis(),
                notes = notes.trim()
            )
            repository.insertSettlement(settlement)
        }
    }

    fun deleteSettlement(settlement: Settlement) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
        }
    }

    private suspend fun seedSampleDataIfEmpty() {
        if (repository.getRoommateCount() == 0) {
            val rahul = Roommate(name = "Rahul Sharma", email = "rahul@example.com", colorHex = "#3B82F6")
            val amit = Roommate(name = "Amit Patel", email = "amit@example.com", colorHex = "#10B981")
            val priya = Roommate(name = "Priya Rao", email = "priya@example.com", colorHex = "#F59E0B")

            val id1 = repository.insertRoommate(rahul)
            val id2 = repository.insertRoommate(amit)
            val id3 = repository.insertRoommate(priya)

            val now = System.currentTimeMillis()
            val day = 86400000L

            // Seed initial realistic expenses
            repository.insertExpense(
                Expense(
                    title = "Monthly Apartment Groceries",
                    amount = 3600.0,
                    category = ExpenseCategory.GROCERIES.name,
                    paidByRoommateId = id1, // Rahul paid
                    splitBetweenRoommateIds = listOf(id1, id2, id3),
                    date = now - day * 2,
                    notes = "Veggies, milk, spices, and cooking oil"
                )
            )

            repository.insertExpense(
                Expense(
                    title = "High-Speed Wi-Fi Bill",
                    amount = 1200.0,
                    category = ExpenseCategory.UTILITIES.name,
                    paidByRoommateId = id2, // Amit paid
                    splitBetweenRoommateIds = listOf(id1, id2, id3),
                    date = now - day * 5,
                    notes = "Monthly fiber internet plan"
                )
            )

            repository.insertExpense(
                Expense(
                    title = "Weekend Pizza & Drinks Night",
                    amount = 1500.0,
                    category = ExpenseCategory.FOOD_DINING.name,
                    paidByRoommateId = id3, // Priya paid
                    splitBetweenRoommateIds = listOf(id1, id2, id3),
                    date = now - day * 1,
                    notes = "Shared dinner order"
                )
            )
        }
    }
}
