package com.biehuale.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.list.AllTransactionsScreen
import com.biehuale.app.ui.record.RecordScreen
import com.biehuale.app.ui.settings.AccountManageScreen
import com.biehuale.app.ui.settings.CategoryManageScreen
import com.biehuale.app.ui.settings.QuickRecordManageScreen
import com.biehuale.app.ui.settings.RecycleBinScreen
import com.biehuale.app.ui.settings.SettingsScreen
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppScaffoldBackground

/**
 * 别花乐 (BieHuaLe) - Navigation 框架
 *
 * Tab 顺序：账单 / 记账 / 设置；冷启动默认记账；底部 Tab 永远可见。
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
                    navController = navController,
                    currentRoute = currentRoute,
                    onNavigate = { dest ->
                        navController.navigateToMainTab(dest)
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
        startDestination = Destinations.RECORD,
        modifier = modifier.fillMaxSize(),
        enterTransition = { bhlEnterTransition() },
        exitTransition = { bhlExitTransition() },
        popEnterTransition = { bhlPopEnterTransition() },
        popExitTransition = { bhlPopExitTransition() }
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
                    // 与底栏同一路径，避免 launchSingleTop 空操作
                    navController.navigateToMainTab(Destinations.RECORD)
                },
                onViewMonthFlow = { start, endExclusive ->
                    navController.navigate(
                        Destinations.allTransactions(
                            rangeStart = start,
                            rangeEndExclusive = endExclusive
                        )
                    )
                },
                onViewAll = {
                    navController.navigate(Destinations.allTransactions())
                },
                onCategoryFlow = { categoryId, start, endExclusive ->
                    navController.navigate(
                        Destinations.allTransactions(
                            categoryId = categoryId,
                            rangeStart = start,
                            rangeEndExclusive = endExclusive
                        )
                    )
                }
            )
        }
        composable(Destinations.RECORD) {
            RecordScreen(
                transactionId = null,
                onManageAccounts = {
                    navController.navigate(Destinations.ACCOUNT_MANAGE)
                },
                onManageCategories = {
                    navController.navigate(Destinations.CATEGORY_MANAGE)
                },
                onManageQuickRecords = {
                    navController.navigate(Destinations.QUICK_RECORD_MANAGE)
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
                onNavigateToQuickRecordManage = {
                    navController.navigate(Destinations.QUICK_RECORD_MANAGE)
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
        composable(Destinations.QUICK_RECORD_MANAGE) {
            QuickRecordManageScreen(onBack = { navController.popBackStack() })
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
                },
                navArgument(Destinations.ARG_RANGE_START) {
                    type = NavType.LongType
                    defaultValue = Destinations.NO_RANGE
                },
                navArgument(Destinations.ARG_RANGE_END) {
                    type = NavType.LongType
                    defaultValue = Destinations.NO_RANGE
                }
            )
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getLong(Destinations.ARG_CATEGORY_ID)
                ?: Destinations.NO_CATEGORY
            val initialCategoryId = raw.takeIf { it != Destinations.NO_CATEGORY }
            val rangeStart = backStackEntry.arguments?.getLong(Destinations.ARG_RANGE_START)
                ?: Destinations.NO_RANGE
            val rangeEnd = backStackEntry.arguments?.getLong(Destinations.ARG_RANGE_END)
                ?: Destinations.NO_RANGE
            val initialRangeStart = rangeStart.takeIf { it != Destinations.NO_RANGE }
            val initialRangeEnd = rangeEnd.takeIf { it != Destinations.NO_RANGE }
            AllTransactionsScreen(
                initialCategoryId = initialCategoryId,
                initialRangeStart = initialRangeStart,
                initialRangeEndExclusive = initialRangeEnd,
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
                },
                onManageCategories = {
                    navController.navigate(Destinations.CATEGORY_MANAGE)
                },
                onManageQuickRecords = {
                    navController.navigate(Destinations.QUICK_RECORD_MANAGE)
                }
            )
        }
    }
}

@Composable
private fun BottomNav(
    navController: NavHostController,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val backStack by navController.currentBackStack.collectAsStateWithLifecycle()
    val selectedTab = remember(currentRoute, backStack) {
        owningMainTab(currentRoute, backStack)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            modifier = Modifier.height(80.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            bhlTabItem(
                route = Destinations.BILL,
                label = stringResource(R.string.tab_bill),
                icon = BhlIcons.Home,
                selected = selectedTab == Destinations.BILL,
                onNavigate = onNavigate
            )
            bhlTabItem(
                route = Destinations.RECORD,
                label = stringResource(R.string.tab_record),
                icon = BhlIcons.Add,
                selected = selectedTab == Destinations.RECORD,
                onNavigate = onNavigate
            )
            bhlTabItem(
                route = Destinations.SETTINGS,
                label = stringResource(R.string.tab_settings),
                icon = BhlIcons.Settings,
                selected = selectedTab == Destinations.SETTINGS,
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
    selected: Boolean,
    onNavigate: (String) -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Tab,
                onClick = { onNavigate(route) }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private val MAIN_TABS = setOf(
    Destinations.BILL,
    Destinations.RECORD,
    Destinations.SETTINGS
)

/** 账户/类别/快速记账：可从记账或设置进入 */
private val SHARED_SECONDARY_ROUTES = setOf(
    Destinations.ACCOUNT_MANAGE,
    Destinations.CATEGORY_MANAGE,
    Destinations.QUICK_RECORD_MANAGE
)

/**
 * 底栏切 Tab：单次 navigate + popUpTo(start) 清掉二级页，
 * 避免 launchSingleTop 空操作，也避免 while 同步 popBackStack。
 */
private fun NavHostController.navigateToMainTab(route: String) {
    if (route !in MAIN_TABS) return
    val current = currentBackStackEntry?.destination?.route
    if (current == route) return

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun routeIsMainTab(route: String?): Boolean =
    route != null && route in MAIN_TABS

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bhlEnterTransition(): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return if (routeIsMainTab(from) && routeIsMainTab(to)) {
        fadeIn(animationSpec = tween(AppMotion.NavTabMs, easing = AppMotion.Easing))
    } else {
        fadeIn(animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)
            )
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bhlExitTransition(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return if (routeIsMainTab(from) && routeIsMainTab(to)) {
        fadeOut(animationSpec = tween(AppMotion.NavTabMs, easing = AppMotion.Easing))
    } else {
        fadeOut(animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)
            )
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bhlPopEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)) +
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)
        )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bhlPopExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)) +
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(AppMotion.NavPushMs, easing = AppMotion.Easing)
        )

/**
 * 当前页应高亮的主 Tab。共享二级页按返回栈中最近的主 Tab 归属，
 * 避免「从记账进快速记账却高亮设置」。
 */
private fun owningMainTab(
    currentRoute: String?,
    backStack: List<NavBackStackEntry>
): String? {
    if (currentRoute == null) return null
    classifyRoute(currentRoute)?.let { return it }

    if (currentRoute in SHARED_SECONDARY_ROUTES) {
        for (i in backStack.size - 2 downTo 0) {
            val r = backStack[i].destination.route ?: continue
            classifyRoute(r)?.let { return it }
            if (r in MAIN_TABS) return r
        }
        return Destinations.SETTINGS
    }
    return null
}

private fun classifyRoute(route: String): String? = when {
    route == Destinations.BILL ||
        route.startsWith("all-transactions") ||
        route.startsWith("transaction-detail") -> Destinations.BILL
    route == Destinations.RECORD ||
        route.startsWith("record-edit") -> Destinations.RECORD
    route == Destinations.SETTINGS ||
        route == Destinations.RECYCLE_BIN -> Destinations.SETTINGS
    else -> null
}
