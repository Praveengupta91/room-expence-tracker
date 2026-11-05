package com.example.domain

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Roommate
import com.example.data.model.Settlement
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

data class RoommateBalance(
    val roommate: Roommate,
    val totalPaid: Double,
    val totalShare: Double,
    val netSettlementAdjust: Double,
    val netBalance: Double
) {
    val isSettled: Boolean get() = abs(netBalance) < 0.01
    val isOwed: Boolean get() = netBalance >= 0.01
    val owes: Boolean get() = netBalance <= -0.01
}

data class SimplifiedDebt(
    val payer: Roommate,
    val payee: Roommate,
    val amount: Double
)

data class CategorySpend(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class RoommateSpend(
    val roommate: Roommate,
    val totalAmount: Double,
    val percentage: Float
)

object DebtCalculator {

    /**
     * Calculates per-roommate net balances.
     * netBalance = (totalPaid - totalShare) + (settlementsPaid - settlementsReceived)
     * Positive -> Is owed money
     * Negative -> Owes money
     */
    fun calculateRoommateBalances(
        roommates: List<Roommate>,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<RoommateBalance> {
        val roommateMap = roommates.associateBy { it.id }
        val totalPaidMap = mutableMapOf<Long, Double>()
        val totalShareMap = mutableMapOf<Long, Double>()
        val settlementsPaidMap = mutableMapOf<Long, Double>()
        val settlementsReceivedMap = mutableMapOf<Long, Double>()

        for (roommate in roommates) {
            totalPaidMap[roommate.id] = 0.0
            totalShareMap[roommate.id] = 0.0
            settlementsPaidMap[roommate.id] = 0.0
            settlementsReceivedMap[roommate.id] = 0.0
        }

        // Aggregate expenses
        for (expense in expenses) {
            val payerId = expense.paidByRoommateId
            totalPaidMap[payerId] = (totalPaidMap[payerId] ?: 0.0) + expense.amount

            val splitCount = expense.splitBetweenRoommateIds.size
            if (splitCount > 0) {
                val perPerson = expense.amount / splitCount
                for (participantId in expense.splitBetweenRoommateIds) {
                    totalShareMap[participantId] = (totalShareMap[participantId] ?: 0.0) + perPerson
                }
            }
        }

        // Aggregate settlements
        for (settlement in settlements) {
            settlementsPaidMap[settlement.payerId] =
                (settlementsPaidMap[settlement.payerId] ?: 0.0) + settlement.amount
            settlementsReceivedMap[settlement.payeeId] =
                (settlementsReceivedMap[settlement.payeeId] ?: 0.0) + settlement.amount
        }

        return roommates.map { roommate ->
            val paid = totalPaidMap[roommate.id] ?: 0.0
            val share = totalShareMap[roommate.id] ?: 0.0
            val setPaid = settlementsPaidMap[roommate.id] ?: 0.0
            val setRec = settlementsReceivedMap[roommate.id] ?: 0.0
            val netAdjust = setPaid - setRec
            val net = (paid - share) + netAdjust

            RoommateBalance(
                roommate = roommate,
                totalPaid = round2(paid),
                totalShare = round2(share),
                netSettlementAdjust = round2(netAdjust),
                netBalance = round2(net)
            )
        }
    }

    /**
     * Greedy Min-Transfers algorithm to simplify debts.
     * Returns minimal list of "A owes B $X" transactions.
     */
    fun simplifyDebts(
        roommates: List<Roommate>,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<SimplifiedDebt> {
        val balances = calculateRoommateBalances(roommates, expenses, settlements)
        val roommateMap = roommates.associateBy { it.id }

        class PersonBalance(val id: Long, var amount: Double)

        val debtors = mutableListOf<PersonBalance>()
        val creditors = mutableListOf<PersonBalance>()

        for (b in balances) {
            if (b.netBalance < -0.01) {
                debtors.add(PersonBalance(b.roommate.id, -b.netBalance))
            } else if (b.netBalance > 0.01) {
                creditors.add(PersonBalance(b.roommate.id, b.netBalance))
            }
        }

        val simplified = mutableListOf<SimplifiedDebt>()

        // Greedily match largest debtor and largest creditor
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.amount }
            creditors.sortByDescending { it.amount }

            val debtor = debtors.first()
            val creditor = creditors.first()

            val settleAmount = min(debtor.amount, creditor.amount)
            val roundedSettle = round2(settleAmount)

            if (roundedSettle > 0.0) {
                val payer = roommateMap[debtor.id]
                val payee = roommateMap[creditor.id]
                if (payer != null && payee != null && payer.id != payee.id) {
                    simplified.add(
                        SimplifiedDebt(
                            payer = payer,
                            payee = payee,
                            amount = roundedSettle
                        )
                    )
                }
            }

            debtor.amount -= settleAmount
            creditor.amount -= settleAmount

            if (debtor.amount < 0.01) {
                debtors.removeAt(0)
            }
            if (creditor.amount < 0.01) {
                creditors.removeAt(0)
            }
        }

        return simplified
    }

    /**
     * Filters expenses to only those in the current calendar month.
     */
    fun filterCurrentMonthExpenses(expenses: List<Expense>): List<Expense> {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val cal = Calendar.getInstance()
        return expenses.filter { expense ->
            cal.timeInMillis = expense.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }

    /**
     * Aggregates expenses by Category with percentage.
     */
    fun calculateCategoryBreakdown(expenses: List<Expense>): List<CategorySpend> {
        val total = expenses.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val grouped = expenses.groupBy { ExpenseCategory.fromString(it.category) }
        return grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            CategorySpend(
                category = cat,
                totalAmount = round2(sum),
                percentage = ((sum / total) * 100).toFloat(),
                count = list.size
            )
        }.sortedByDescending { it.totalAmount }
    }

    /**
     * Aggregates expenses by who paid with percentage.
     */
    fun calculateRoommateSpendBreakdown(
        roommates: List<Roommate>,
        expenses: List<Expense>
    ): List<RoommateSpend> {
        val total = expenses.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val roommateMap = roommates.associateBy { it.id }
        val grouped = expenses.groupBy { it.paidByRoommateId }

        return grouped.mapNotNull { (roommateId, list) ->
            val roommate = roommateMap[roommateId] ?: return@mapNotNull null
            val sum = list.sumOf { it.amount }
            RoommateSpend(
                roommate = roommate,
                totalAmount = round2(sum),
                percentage = ((sum / total) * 100).toFloat()
            )
        }.sortedByDescending { it.totalAmount }
    }

    private fun round2(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}
