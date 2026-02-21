package de.vvo.glassapp

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import de.vvo.glassapp.data.repository.FavoritesManager

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesWidgetItemFactory(applicationContext)
    }
}

class FavoritesWidgetItemFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var favorites = listOf<de.vvo.glassapp.ui.viewmodel.Favorite>()

    override fun onCreate() {
        loadFavorites()
    }
    override fun onDataSetChanged() {
        loadFavorites()
    }

    private fun loadFavorites() {
        val manager = FavoritesManager(context)
        favorites = manager.getFavorites()
    }
    override fun onDestroy() {}
    override fun getCount(): Int = favorites.size

    override fun getViewAt(position: Int): RemoteViews {
        val favorite = favorites[position]
        val views = RemoteViews(context.packageName, R.layout.widget_favorite_item)
        views.setTextViewText(R.id.item_text, favorite.name)

        val iconRes = when(favorite.iconName) {
            "Work" -> R.drawable.ic_work
            "Home" -> R.drawable.ic_home
            "School" -> R.drawable.ic_school
            "Train" -> R.drawable.ic_train
            "Place" -> R.drawable.ic_place
            else -> R.drawable.ic_star
        }
        views.setImageViewResource(R.id.item_icon, iconRes)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.item_text, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
