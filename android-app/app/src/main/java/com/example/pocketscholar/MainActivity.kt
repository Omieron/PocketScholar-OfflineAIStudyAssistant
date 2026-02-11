package com.example.pocketscholar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/*
 * Paul Rand: "Design is the method of putting form and content together."
 */

private val RandBlack = Color(0xFF1A1A1A)
private val RandWhite = Color(0xFFF8F7F4)
private val RandTeal = Color(0xFF2D9D94)
private val RandGrey = Color(0xFF9E9E9E)
private val RandLightGrey = Color(0xFFE8E6E1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LlamaEngine.init(applicationContext)
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
                    containerColor = RandWhite,
                    bottomBar = {
                        // Rand navigasyon: ince üst çizgi, düz arka plan, minimal
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(RandLightGrey)
                            )
                            NavigationBar(
                                containerColor = RandWhite,
                                tonalElevation = 0.dp
                            ) {
                                AppScreen.entries.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.name } == true
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.height(20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                screen.title.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                                    letterSpacing = 1.sp,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        },
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
                                            selectedIconColor = RandBlack,
                                            selectedTextColor = RandBlack,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = RandGrey,
                                            unselectedTextColor = RandGrey
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.Documents.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(AppScreen.Documents.name) { DocumentsScreen() }
                        composable(AppScreen.Chat.name) { ChatScreen() }
                        composable(AppScreen.Models.name) { ModelManagerScreen() }
                        composable(AppScreen.Stats.name) { StatsScreen() }
                    }
                }
            }
        }
    }
}
