package de.vvo.glassapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class FavoritesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_favorites)

        // Intent to launch MainActivity
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // Intent for the list
        val serviceIntent = Intent(context, FavoritesWidgetService::class.java)
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)

        // Template for item clicks
        val clickIntent = Intent(context, MainActivity::class.java)
        val clickPendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
