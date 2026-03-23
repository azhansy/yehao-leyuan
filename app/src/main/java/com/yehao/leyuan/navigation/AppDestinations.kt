package com.yehao.leyuan.navigation

sealed class AppDestinations(val route: String, val label: String) {
    data object LetterPuzzle : AppDestinations("letter", "字母拼图")
    data object Animals : AppDestinations("animals", "动物王国")
    data object Shapes : AppDestinations("shapes", "形状配对")
    data object Math : AppDestinations("math", "数学题")

    companion object {
        val bottomItems = listOf(LetterPuzzle, Animals, Shapes, Math)
    }
}
