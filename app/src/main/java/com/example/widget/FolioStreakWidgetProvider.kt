package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class FolioStreakWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(8_000L) {
                    FolioWidgetUpdater.updateStreakWidgetsBlocking(
                        context = context.applicationContext,
                        manager = appWidgetManager,
                        appWidgetIds = appWidgetIds
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
