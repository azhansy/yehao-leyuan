package com.yehao.leyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yehao.leyuan.feature.animal.AnimalIdentificationScreen
import com.yehao.leyuan.feature.letter.LetterPuzzleScreen
import com.yehao.leyuan.feature.math.SimpleMathScreen
import com.yehao.leyuan.feature.race.LaneDodgeScreen
import com.yehao.leyuan.feature.shape.ShapeMatchingScreen
import com.yehao.leyuan.navigation.AppDestinations
import com.yehao.leyuan.update.AppUpdateOverlay

@Composable
fun YehaoLeyuanApp() {
    val navController = rememberNavController()

    // enableEdgeToEdge()：为状态栏 + 三键/手势导航条预留内边距，避免底部按钮被挡住
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Home.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppDestinations.Home.route) {
                GameMapHomeScreen(navController = navController)
            }
            composable(AppDestinations.LetterPuzzle.route) {
                LetterPuzzleScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Animals.route) {
                AnimalIdentificationScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Shapes.route) {
                ShapeMatchingScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Math.route) {
                SimpleMathScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Racing.route) {
                LaneDodgeScreen(onBack = { navController.popBackStack() })
            }
        }
        AppUpdateOverlay()
    }
}
