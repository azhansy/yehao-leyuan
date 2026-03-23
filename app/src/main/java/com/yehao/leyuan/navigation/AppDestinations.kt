package com.yehao.leyuan.navigation

sealed class AppDestinations(val route: String, val label: String) {
    data object Home : AppDestinations("home", "乐园")
    data object LetterPuzzle : AppDestinations("letter", "字母拼图")
    data object Animals : AppDestinations("animals", "动物王国")
    data object Shapes : AppDestinations("shapes", "形状配对")
    data object Math : AppDestinations("math", "数学题")
    data object Racing : AppDestinations("racing", "赛车")

    companion object {
        /** 首页地图上的游戏入口（顺序即展示顺序） */
        val games = listOf(LetterPuzzle, Animals, Shapes, Math, Racing)
    }
}
