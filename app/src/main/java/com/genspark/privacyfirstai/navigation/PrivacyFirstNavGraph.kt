package com.genspark.privacyfirstai.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.genspark.privacyfirstai.ui.component.AppPill
import com.genspark.privacyfirstai.ui.component.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val currentDestination = destinations.firstOrNull { it.route == currentRoute } ?: AppDestination.Dashboard
    val navigationColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    AppScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column {
                            Text(
                                text = "Privacy First AI",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = currentDestination.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        AppPill(text = currentDestination.label, modifier = Modifier.padding(end = 16.dp))
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        windowInsets = WindowInsets.navigationBars
                    ) {
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
                                icon = { Text(destination.navSymbol, style = MaterialTheme.typography.labelMedium) },
                                label = { Text(destination.label) },
                                colors = navigationColors
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                modifier = Modifier.fillMaxSize(),
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
}
