package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.SimplifiedDebt
import com.example.ui.screens.BalancesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.RecordExpenseScreen
import com.example.ui.screens.RoommatesScreen
import com.example.ui.screens.SettleUpDialog
import com.example.viewmodel.RoomieViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    EXPENSES("Expenses", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BALANCES("Balances", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    ROOMMATES("Roommates", Icons.Filled.Group, Icons.Outlined.Group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: RoomieViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var isRecordingExpense by remember { mutableStateOf(false) }
    var activeSettleUpDebt by remember { mutableStateOf<SimplifiedDebt?>(null) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // System BackHandler
    BackHandler(enabled = isRecordingExpense || currentTab != NavigationTab.DASHBOARD) {
        if (isRecordingExpense) {
            isRecordingExpense = false
        } else {
            currentTab = NavigationTab.DASHBOARD
        }
    }

    if (isRecordingExpense) {
        RecordExpenseScreen(
            roommates = state.roommates,
            currencySymbol = state.currencySymbol,
            onBack = { isRecordingExpense = false },
            onSaveExpense = { title, amount, categoryName, paidById, splitBetweenIds, splitType, notes ->
                viewModel.addExpenseWithCustomCategory(
                    title = title,
                    amount = amount,
                    categoryName = categoryName,
                    paidById = paidById,
                    splitBetweenIds = splitBetweenIds,
                    splitType = splitType,
                    notes = notes
                )
                isRecordingExpense = false
            },
            onAddNewRoommate = { name, email, colorHex ->
                viewModel.createRoommateAndGetId(name, email, colorHex)
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.currencySymbol,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RoomieSplit",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Currency switcher button
                    IconButton(
                        onClick = { showCurrencyDialog = true },
                        modifier = Modifier.testTag("currency_switch_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = state.currencySymbol,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == NavigationTab.BALANCES && state.simplifiedDebts.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text("${state.simplifiedDebts.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onAddExpenseClick = { isRecordingExpense = true },
                    onChartModeChange = { viewModel.setChartMode(it) },
                    onSettleUpClick = { activeSettleUpDebt = it },
                    onViewAllExpensesClick = { currentTab = NavigationTab.EXPENSES },
                    onViewAllDebtsClick = { currentTab = NavigationTab.BALANCES },
                    onExpenseClick = { currentTab = NavigationTab.EXPENSES }
                )

                NavigationTab.EXPENSES -> ExpensesScreen(
                    state = state,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                    onAddExpenseClick = { isRecordingExpense = true },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )

                NavigationTab.BALANCES -> BalancesScreen(
                    state = state,
                    onSettleUpClick = { activeSettleUpDebt = it },
                    onDeleteSettlement = { viewModel.deleteSettlement(it) }
                )

                NavigationTab.ROOMMATES -> RoommatesScreen(
                    state = state,
                    onAddRoommate = { name, email, colorHex ->
                        viewModel.addRoommate(name, email, colorHex)
                    },
                    onUpdateRoommate = { viewModel.updateRoommate(it) },
                    onDeleteRoommate = { viewModel.deleteRoommate(it) }
                )
            }
        }
    }

    // Settle Up Dialog
    if (activeSettleUpDebt != null) {
        val debt = activeSettleUpDebt!!
        SettleUpDialog(
            debt = debt,
            currencySymbol = state.currencySymbol,
            onDismiss = { activeSettleUpDebt = null },
            onConfirmSettlement = { payerId, payeeId, amount, notes ->
                viewModel.settleUp(payerId, payeeId, amount, notes)
            }
        )
    }

    // Currency Switcher Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("₹" to "Indian Rupee (INR)", "$" to "US Dollar (USD)", "€" to "Euro (EUR)", "£" to "British Pound (GBP)", "C$" to "Canadian Dollar (CAD)")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency Symbol") },
            text = {
                androidx.compose.foundation.layout.Column {
                    currencies.forEach { (symbol, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCurrencySymbol(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.currencySymbol == symbol,
                                onClick = {
                                    viewModel.setCurrencySymbol(symbol)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$symbol  •  $label",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
