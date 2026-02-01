package com.example.pocketscholar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketscholar.ui.navigation.AppScreen
import com.example.pocketscholar.ui.screens.ChatScreen
import com.example.pocketscholar.ui.screens.DocumentsScreen
import com.example.pocketscholar.ui.screens.StatsScreen
import com.example.pocketscholar.ui.theme.PocketScholarTheme
import com.example.pocketscholar.engine.LlamaEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LlamaEngine.init(applicationContext)
        setContent {
            PocketScholarTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            AppScreen.entries.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.name } == true,
                                    onClick = {
                                        navController.navigate(screen.name) {
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
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.Documents.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(AppScreen.Documents.name) {
                            DocumentsScreen()
                        }
                        composable(AppScreen.Chat.name) {
                            ChatScreen()
                        }
                        composable(AppScreen.Stats.name) {
                            StatsScreen()
                        }
                    }
                }
            }
        }
    }
}
