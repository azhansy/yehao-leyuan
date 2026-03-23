package com.yehao.leyuan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yehao.leyuan.feature.animal.AnimalIdentificationScreen
import com.yehao.leyuan.feature.letter.LetterPuzzleScreen
import com.yehao.leyuan.feature.math.SimpleMathScreen
import com.yehao.leyuan.feature.shape.ShapeMatchingScreen
import com.yehao.leyuan.navigation.AppDestinations
import com.yehao.leyuan.update.AppUpdateOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.GrapePurple
import com.yehao.leyuan.ui.theme.SkyBlue

private fun iconFor(route: String): ImageVector = when (route) {
    AppDestinations.LetterPuzzle.route -> Icons.Rounded.TextFields
    AppDestinations.Animals.route -> Icons.Rounded.Pets
    AppDestinations.Shapes.route -> Icons.Rounded.Category
    else -> Icons.Rounded.Calculate
}

@Composable
fun YehaoLeyuanApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                AppDestinations.bottomItems.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = iconFor(dest.route),
                                contentDescription = dest.label
                            )
                        },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = when (dest.route) {
                                AppDestinations.LetterPuzzle.route -> SkyBlue
                                AppDestinations.Animals.route -> GrassGreen
                                AppDestinations.Shapes.route -> GrapePurple
                                else -> CherryRed
                            },
                            selectedTextColor = when (dest.route) {
                                AppDestinations.LetterPuzzle.route -> SkyBlue
                                AppDestinations.Animals.route -> GrassGreen
                                AppDestinations.Shapes.route -> GrapePurple
                                else -> CherryRed
                            },
                            indicatorColor = Color(0xFFE3F2FD),
                            unselectedIconColor = Color(0xFF90A4AE),
                            unselectedTextColor = Color(0xFF90A4AE)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.LetterPuzzle.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.LetterPuzzle.route) { LetterPuzzleScreen() }
            composable(AppDestinations.Animals.route) { AnimalIdentificationScreen() }
            composable(AppDestinations.Shapes.route) { ShapeMatchingScreen() }
            composable(AppDestinations.Math.route) { SimpleMathScreen() }
        }
    }
    AppUpdateOverlay()
}
