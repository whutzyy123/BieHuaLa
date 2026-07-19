package com.biehuale.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.biehuale.app.R
import com.biehuale.app.ui.bill.BillScreen
import com.biehuale.app.ui.detail.TransactionDetailScreen
import com.biehuale.app.ui.list.AllTransactionsScreen
import com.biehuale.app.ui.record.RecordScreen
import com.biehuale.app.ui.settings.AccountManageScreen
import com.biehuale.app.ui.settings.CategoryManageScreen
import com.biehuale.app.ui.settings.RecycleBinScreen
import com.biehuale.app.ui.settings.SettingsScreen
import com.biehuale.app.ui.theme.AppScaffoldBackground

/**
 * 别花乐 (BieHuaLe) - Navigation 框架
 *
 * Tab 顺序：账单 / 记账 / 设置；底部 Tab 永远可见。
 */
@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    AppScaffoldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { dest ->
                        navController.navigate(dest) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AppNavHost(navController = navController)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.BILL,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Destinations.BILL) {
            BillScreen(
                onItemClick = { txId ->
                    navController.navigate(Destinations.transactionDetail(txId))
                },
                onEdit = { txId ->
                    navController.navigate(Destinations.recordEdit(txId))
                },
                onGoToRecord = {
                    navController.navigate(Destinations.RECORD) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onViewAll = {
                    navController.navigate(Destinations.allTransactions())
                },
                onCategoryFlow = { categoryId ->
                    navController.navigate(Destinations.allTransactions(categoryId))
                }
            )
        }
        composable(Destinations.RECORD) {
            RecordScreen(
                transactionId = null,
                onManageAccounts = {
                    navController.navigate(Destinations.ACCOUNT_MANAGE)
                }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onNavigateToAccountManage = {
                    navController.navigate(Destinations.ACCOUNT_MANAGE)
                },
                onNavigateToCategoryManage = {
                    navController.navigate(Destinations.CATEGORY_MANAGE)
                },
                onNavigateToRecycleBin = {
                    navController.navigate(Destinations.RECYCLE_BIN)
                }
            )
        }

        composable(Destinations.ACCOUNT_MANAGE) {
            AccountManageScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.CATEGORY_MANAGE) {
            CategoryManageScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.RECYCLE_BIN) {
            RecycleBinScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destinations.ALL_TRANSACTIONS,
            arguments = listOf(
                navArgument(Destinations.ARG_CATEGORY_ID) {
                    type = NavType.LongType
                    defaultValue = Destinations.NO_CATEGORY
                }
            )
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getLong(Destinations.ARG_CATEGORY_ID)
                ?: Destinations.NO_CATEGORY
            val initialCategoryId = raw.takeIf { it != Destinations.NO_CATEGORY }
            AllTransactionsScreen(
                initialCategoryId = initialCategoryId,
                onBack = { navController.popBackStack() },
                onItemClick = { txId ->
                    navController.navigate(Destinations.transactionDetail(txId))
                },
                onEdit = { txId ->
                    navController.navigate(Destinations.recordEdit(txId))
                }
            )
        }

        composable(
            route = Destinations.TRANSACTION_DETAIL,
            arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) {
                type = NavType.LongType
            })
        ) {
            TransactionDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { txId ->
                    navController.navigate(Destinations.recordEdit(txId))
                }
            )
        }

        composable(
            route = Destinations.RECORD_EDIT,
            arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getLong(Destinations.ARG_TRANSACTION_ID) ?: 0L
            RecordScreen(
                transactionId = txId,
                onSavedAndExit = { navController.popBackStack() },
                onManageAccounts = {
                    navController.navigate(Destinations.ACCOUNT_MANAGE)
                }
            )
        }
    }
}

@Composable
private fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            bhlTabItem(
                route = Destinations.BILL,
                label = stringResource(R.string.tab_bill),
                icon = Icons.Filled.Home,
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
            bhlTabItem(
                route = Destinations.RECORD,
                label = stringResource(R.string.tab_record),
                icon = Icons.Filled.Add,
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
            bhlTabItem(
                route = Destinations.SETTINGS,
                label = stringResource(R.string.tab_settings),
                icon = Icons.Filled.Settings,
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun RowScope.bhlTabItem(
    route: String,
    label: String,
    icon: ImageVector,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val selected = isTabSelected(route, currentRoute)
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(route) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = colors
    )
}

private fun isTabSelected(tabRoute: String, currentRoute: String?): Boolean {
    if (currentRoute == null) return false
    return when (tabRoute) {
        Destinations.BILL ->
            currentRoute == Destinations.BILL ||
                currentRoute.startsWith("all-transactions") ||
                currentRoute.startsWith("transaction-detail")
        Destinations.RECORD ->
            currentRoute == Destinations.RECORD ||
                currentRoute.startsWith("record-edit")
        Destinations.SETTINGS ->
            currentRoute == Destinations.SETTINGS ||
                currentRoute == Destinations.ACCOUNT_MANAGE ||
                currentRoute == Destinations.CATEGORY_MANAGE ||
                currentRoute == Destinations.RECYCLE_BIN
        else -> currentRoute == tabRoute
    }
}
