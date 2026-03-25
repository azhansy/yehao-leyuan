package com.yehao.leyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yehao.leyuan.feature.animal.AnimalIdentificationScreen
import com.yehao.leyuan.feature.animal.PeopleRolesScreen
import com.yehao.leyuan.feature.family.FamilyEnglishReadScreen
import com.yehao.leyuan.feature.letter.LetterPuzzleScreen
import com.yehao.leyuan.feature.math.SimpleMathScreen
import com.yehao.leyuan.feature.dino.DinoRunScreen
import com.yehao.leyuan.feature.race.LaneDodgeScreen
import com.yehao.leyuan.feature.animaljigsaw.AnimalJigsawScreen
import com.yehao.leyuan.feature.shape.ShapeMatchingScreen
import com.yehao.leyuan.navigation.AppDestinations
import com.yehao.leyuan.update.AppUpdateOverlay

@Composable
fun YehaoLeyuanApp() {
    val navController = rememberNavController()

    // 沉浸式顶栏（状态栏透明，内容可铺到顶部）；仅避开底部导航条/手势区
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
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
            composable(AppDestinations.PeopleRoles.route) {
                PeopleRolesScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.FamilyEnglishRead.route) {
                FamilyEnglishReadScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Shapes.route) {
                ShapeMatchingScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.AnimalJigsaw.route) {
                AnimalJigsawScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Math.route) {
                SimpleMathScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.Racing.route) {
                LaneDodgeScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.DinoRun.route) {
                DinoRunScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
        AppUpdateOverlay()
    }
}
