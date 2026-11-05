package com.example

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Roommate
import com.example.data.model.Settlement
import com.example.domain.DebtCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testDebtSimplificationAndSettlement() {
    val rahul = Roommate(id = 1L, name = "Rahul", colorHex = "#3B82F6")
    val amit = Roommate(id = 2L, name = "Amit", colorHex = "#10B981")
    val priya = Roommate(id = 3L, name = "Priya", colorHex = "#F59E0B")

    val roommates = listOf(rahul, amit, priya)

    // Rahul pays ₹600 for Groceries split between Rahul, Amit, Priya (₹200 each)
    val expense1 = Expense(
      id = 101L,
      title = "Groceries",
      amount = 600.0,
      category = ExpenseCategory.GROCERIES.name,
      paidByRoommateId = 1L,
      splitBetweenRoommateIds = listOf(1L, 2L, 3L)
    )

    val balances1 = DebtCalculator.calculateRoommateBalances(roommates, listOf(expense1), emptyList())
    val rahulBal = balances1.first { it.roommate.id == 1L }
    val amitBal = balances1.first { it.roommate.id == 2L }
    val priyaBal = balances1.first { it.roommate.id == 3L }

    // Rahul paid 600, share is 200 => +400
    assertEquals(400.0, rahulBal.netBalance, 0.01)
    // Amit paid 0, share is 200 => -200
    assertEquals(-200.0, amitBal.netBalance, 0.01)
    // Priya paid 0, share is 200 => -200
    assertEquals(-200.0, priyaBal.netBalance, 0.01)

    // Simplified debts: Amit owes Rahul ₹200, Priya owes Rahul ₹200
    val debts1 = DebtCalculator.simplifyDebts(roommates, listOf(expense1), emptyList())
    assertEquals(2, debts1.size)
    assertTrue(debts1.all { it.payee.id == 1L && it.amount == 200.0 })

    // Now Amit settles ₹200 with Rahul
    val settlement1 = Settlement(
      id = 1L,
      payerId = 2L, // Amit
      payeeId = 1L, // Rahul
      amount = 200.0
    )

    val balances2 = DebtCalculator.calculateRoommateBalances(roommates, listOf(expense1), listOf(settlement1))
    val amitBalAfter = balances2.first { it.roommate.id == 2L }
    val rahulBalAfter = balances2.first { it.roommate.id == 1L }

    // Amit is now fully settled (0 balance)
    assertEquals(0.0, amitBalAfter.netBalance, 0.01)
    // Rahul now gets back +200 (still owed by Priya)
    assertEquals(200.0, rahulBalAfter.netBalance, 0.01)

    // Simplified debts now should have ONLY 1 debt: Priya owes Rahul ₹200
    val debts2 = DebtCalculator.simplifyDebts(roommates, listOf(expense1), listOf(settlement1))
    assertEquals(1, debts2.size)
    assertEquals(3L, debts2[0].payer.id)
    assertEquals(1L, debts2[0].payee.id)
    assertEquals(200.0, debts2[0].amount, 0.01)
  }
}
