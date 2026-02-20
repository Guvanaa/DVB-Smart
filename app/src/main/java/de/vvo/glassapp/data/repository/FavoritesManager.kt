package de.vvo.glassapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.vvo.glassapp.ui.viewmodel.Favorite

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getFavorites(): List<Favorite> {
        val json = prefs.getString("favorites_list", null) ?: return listOf(
            Favorite("Arbeit", "33000028"),
            Favorite("Zuhause", "33000037"),
            Favorite("Hbf", "33000028")
        )
        val type = object : TypeToken<List<Favorite>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveFavorites(favorites: List<Favorite>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString("favorites_list", json).apply()
    }
}
