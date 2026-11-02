package com.example

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Legacy-compatible bridge to the 3D Obsidian 6-Pillar Home Screen Widget Engine
 * ([BioSyncHomeWidgetProvider] & [BioSyncWidgetAndNotificationEngine]).
 */
class BioSyncAppWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val snapshot = BioSyncWidgetAndNotificationEngine.collectSnapshot(context)
    for (widgetId in appWidgetIds) {
      val rv = BioSyncWidgetAndNotificationEngine.buildWidgetRemoteViews(
        context = context,
        appWidgetManager = appWidgetManager,
        appWidgetId = widgetId,
        snapshot = snapshot,
      )
      appWidgetManager.updateAppWidget(widgetId, rv)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH_WIDGET) {
      updateAllWidgets(context)
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.example.biosync.ACTION_REFRESH_WIDGET"

    fun updateAllWidgets(context: Context) {
      BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
    }

    fun buildRemoteViews(context: Context): RemoteViews {
      return RemoteViews(context.packageName, R.layout.biosync_home_widget)
    }
  }
}
