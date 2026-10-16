package com.riskauditor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.riskauditor.ui.navigation.AppNavigation
import com.riskauditor.ui.navigation.Screen
import com.riskauditor.ui.theme.RiskAuditorTheme
import com.riskauditor.ui.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent?.getStringExtra("EXTRA_PACKAGE_NAME")

        setContent {
            RiskAuditorTheme {
                val navController = rememberNavController()
                val startRoute = if (targetPackage != null) {
                    Screen.Detail.createRoute(targetPackage)
                } else {
                    Screen.Dashboard.route
                }

                AppNavigation(
                    navController = navController,
                    viewModel = viewModel,
                    startDestination = startRoute
                )
            }
        }
    }
}

