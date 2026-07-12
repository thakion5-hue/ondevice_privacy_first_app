package com.genspark.privacyfirstai.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.genspark.privacyfirstai.di.AppContainer
import com.genspark.privacyfirstai.feature.assistant.AssistantRoute
import com.genspark.privacyfirstai.feature.dashboard.DashboardRoute
import com.genspark.privacyfirstai.feature.gallery.GalleryCleanerRoute
import com.genspark.privacyfirstai.feature.journal.JournalDraftRoute
import com.genspark.privacyfirstai.feature.security.CleanGuardRoute
import com.genspark.privacyfirstai.feature.settings.SettingsRoute

@Composable
fun PrivacyFirstNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val destinations = listOf(
        AppDestination.Dashboard,
        AppDestination.Assistant,
        AppDestination.Gallery,
        AppDestination.Journal,
        AppDestination.Security,
        AppDestination.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.label.take(2)) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route
        ) {
            composable(AppDestination.Dashboard.route) {
                DashboardRoute(container = container, paddingValues = paddingValues)
            }
            composable(AppDestination.Assistant.route) {
                AssistantRoute(container = container, paddingValues = paddingValues)
            }
            composable(AppDestination.Gallery.route) {
                GalleryCleanerRoute(container = container, paddingValues = paddingValues)
            }
            composable(AppDestination.Journal.route) {
                JournalDraftRoute(container = container, paddingValues = paddingValues)
            }
            composable(AppDestination.Security.route) {
                CleanGuardRoute(container = container, paddingValues = paddingValues)
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(container = container, paddingValues = paddingValues)
            }
        }
    }
}
