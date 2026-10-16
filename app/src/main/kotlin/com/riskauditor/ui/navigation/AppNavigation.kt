package com.riskauditor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.riskauditor.core.model.RiskBand
import com.riskauditor.ui.applist.AppListScreen
import com.riskauditor.ui.dashboard.DashboardScreen
import com.riskauditor.ui.detail.AppDetailScreen
import com.riskauditor.ui.diff.PermissionDiffScreen
import com.riskauditor.ui.viewmodel.ScannerViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object AppList : Screen("apps?band={band}", "Apps", Icons.Default.Apps) {
        fun createRoute(band: RiskBand? = null): String {
            return if (band != null) "apps?band=${band.name}" else "apps"
        }
    }
    object Diff : Screen("diff", "Diff Log", Icons.Default.History)
    object Detail : Screen("detail/{packageName}", "App Detail") {
        fun createRoute(packageName: String): String = "detail/$packageName"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: ScannerViewModel,
    startDestination: String = Screen.Dashboard.route
) {
    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.AppList,
        Screen.Diff
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Show bottom navigation bar on top-level screens
            val showBottomBar = bottomNavItems.any { it.route.substringBefore('?') == currentRoute?.substringBefore('?') }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute?.substringBefore('?') == screen.route.substringBefore('?'),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Detail.createRoute(pkg))
                    },
                    onNavigateToAppsList = { band ->
                        navController.navigate(Screen.AppList.createRoute(band))
                    }
                )
            }

            composable(
                route = Screen.AppList.route,
                arguments = listOf(
                    navArgument("band") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val bandArg = backStackEntry.arguments?.getString("band")
                val initialBand = bandArg?.let { runCatching { RiskBand.valueOf(it) }.getOrNull() }

                AppListScreen(
                    viewModel = viewModel,
                    initialBand = initialBand,
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Detail.createRoute(pkg))
                    }
                )
            }

            composable(Screen.Diff.route) {
                PermissionDiffScreen(
                    viewModel = viewModel,
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Detail.createRoute(pkg))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = pkg,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

