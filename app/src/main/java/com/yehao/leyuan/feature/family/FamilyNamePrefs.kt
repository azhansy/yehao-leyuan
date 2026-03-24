package com.yehao.leyuan.feature.family

import android.content.Context
import androidx.core.content.edit

private const val PREFS = "family_english_names"
private const val KEY_ME = "me"
private const val KEY_FATHER = "father"
private const val KEY_MOTHER = "mother"
private const val KEY_BROTHER = "brother"
private const val KEY_OLDER_SISTER = "older_sister"
private const val KEY_YOUNGER_SISTER = "younger_sister"
private const val KEY_TEMPLATE_INDEX = "template_index"

data class FamilyNameSlots(
    val myName: String = "",
    val father: String = "",
    val mother: String = "",
    val youngerBrother: String = "",
    val olderSister: String = "",
    val youngerSister: String = "",
)

class FamilyNamePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): FamilyNameSlots = FamilyNameSlots(
        myName = sp.getString(KEY_ME, "").orEmpty(),
        father = sp.getString(KEY_FATHER, "").orEmpty(),
        mother = sp.getString(KEY_MOTHER, "").orEmpty(),
        youngerBrother = sp.getString(KEY_BROTHER, "").orEmpty(),
        olderSister = sp.getString(KEY_OLDER_SISTER, "").orEmpty(),
        youngerSister = sp.getString(KEY_YOUNGER_SISTER, "").orEmpty(),
    )

    fun loadTemplateIndex(): Int = sp.getInt(KEY_TEMPLATE_INDEX, 0).coerceIn(0, 99)

    fun save(slots: FamilyNameSlots, templateIndex: Int) {
        sp.edit {
            putString(KEY_ME, slots.myName)
            putString(KEY_FATHER, slots.father)
            putString(KEY_MOTHER, slots.mother)
            putString(KEY_BROTHER, slots.youngerBrother)
            putString(KEY_OLDER_SISTER, slots.olderSister)
            putString(KEY_YOUNGER_SISTER, slots.youngerSister)
            putInt(KEY_TEMPLATE_INDEX, templateIndex.coerceIn(0, 99))
        }
    }
}
