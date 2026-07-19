package com.biehuale.app.ui.nav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

/**
 * 别花乐 (BieHuaLe) - Navigation 框架
 *
 * 详见 docs/PRD.md §4.1, §4.2
 *
 * Tab 顺序（从左到右）：账单 / 记账 / 设置
 *
 * 路由层级：
 *  - 顶层 3 Tab
 *  - 设置 Tab 子页（账户管理 / 类别管理 / 全部流水 / 回收站）
 *  - 弹出路由：流水详情 / 编辑（账单 Tab 触发）
 *
 * 关键设计（PRD §4.2）：
 *  - 切 Tab 状态保留
 *  - **底部 Tab 永远可见**——三主 Tab 内始终显示；进入详情/子页时仍保留
 *    底部 Tab（1 tap 回主流程）
 *  - 子页 Tap Tab 不会清子页栈，但顶层 Tab 状态保留逻辑优先
 */
@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
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
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
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
        modifier = modifier
    ) {
        // 3 Tab
        composable(Destinations.BILL) {
            BillScreen(
                onItemClick = { txId ->
                    navController.navigate(Destinations.transactionDetail(txId))
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
                    navController.navigate(Destinations.ALL_TRANSACTIONS)
                }
            )
        }
        composable(Destinations.RECORD) {
            RecordScreen(
                transactionId = null,
                onNavigateToBill = {
                    navController.navigate(Destinations.BILL) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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

        // 设置 Tab 子页
        composable(Destinations.ACCOUNT_MANAGE) {
            AccountManageScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.CATEGORY_MANAGE) {
            CategoryManageScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.RECYCLE_BIN) {
            RecycleBinScreen(onBack = { navController.popBackStack() })
        }

        // 全部流水页（从账单 Tab 「查看全部」进入）
        composable(Destinations.ALL_TRANSACTIONS) {
            AllTransactionsScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { txId ->
                    navController.navigate(Destinations.transactionDetail(txId))
                }
            )
        }

        // 流水详情
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

        // 编辑模式
        composable(
            route = Destinations.RECORD_EDIT,
            arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getLong(Destinations.ARG_TRANSACTION_ID) ?: 0L
            RecordScreen(
                transactionId = txId,
                onSavedAndExit = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
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

@Composable
private fun RowScope.bhlTabItem(
    route: String,
    label: String,
    icon: ImageVector,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val selected = isTabSelected(route, currentRoute)
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(route) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}

/** 子路由仍高亮所属主 Tab（详情 / 全部流水 / 设置子页等） */
private fun isTabSelected(tabRoute: String, currentRoute: String?): Boolean {
    if (currentRoute == null) return false
    return when (tabRoute) {
        Destinations.BILL ->
            currentRoute == Destinations.BILL ||
                currentRoute == Destinations.ALL_TRANSACTIONS ||
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
