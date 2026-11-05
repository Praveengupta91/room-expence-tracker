package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ExpenseCategory(
    val displayName: String,
    val iconName: String,
    val color: Color
) {
    VEGETABLES("Vegetables", "eco", Color(0xFF16A34A)), // Fresh Green
    GROCERIES("Groceries", "shopping_cart", Color(0xFF10B981)), // Emerald Green
    RENT("Rent", "home", Color(0xFF6366F1)), // Indigo
    UTILITIES("Utilities", "bolt", Color(0xFFF59E0B)), // Amber
    FOOD_DINING("Food & Dining", "restaurant", Color(0xFFEC4899)), // Pink
    HOUSEHOLD("Household", "cleaning_services", Color(0xFF06B6D4)), // Cyan
    ENTERTAINMENT("Entertainment", "movie", Color(0xFF8B5CF6)), // Purple
    TRANSPORTATION("Travel", "directions_car", Color(0xFF3B82F6)), // Blue
    OTHER("Other / Custom", "category", Color(0xFF64748B)); // Slate

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}
