package de.vvo.glassapp

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesWidgetItemFactory(applicationContext)
    }
}

class FavoritesWidgetItemFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val favorites = listOf("Arbeit", "Zuhause", "Hauptbahnhof")

    override fun onCreate() {}
    override fun onDataSetChanged() {}
    override fun onDestroy() {}
    override fun getCount(): Int = favorites.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        views.setTextViewText(android.R.id.text1, favorites[position])
        views.setTextColor(android.R.id.text1, android.graphics.Color.WHITE)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(android.R.id.text1, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
