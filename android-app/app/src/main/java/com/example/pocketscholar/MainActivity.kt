package com.example.pocketscholar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.pocketscholar.ui.screens.ModelManagerScreen
import com.example.pocketscholar.ui.screens.StatsScreen
import com.example.pocketscholar.ui.theme.PocketScholarTheme
import com.example.pocketscholar.engine.LlamaEngine
import com.example.pocketscholar.data.ModelRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LlamaEngine.init(applicationContext)
        // Aktif modeli yükle (ModelRepository üzerinden)
        val modelRepo = ModelRepository(applicationContext)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val modelPath = modelRepo.findAnyAvailableModelPath()
                if (modelPath != null) {
                    LlamaEngine.loadModel(modelPath)
                }
            }
        }
        setContent {
            PocketScholarTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            AppScreen.entries.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.name } == true
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.name) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                        composable(AppScreen.Models.name) {
                            ModelManagerScreen()
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
