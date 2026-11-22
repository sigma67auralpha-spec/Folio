package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.FolioDatabase
import com.example.data.model.BookEntity
import com.example.data.model.ReadingSessionEntity
import com.example.ui.screens.calculateStreaks
import com.example.ui.screens.currentLocalEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class WidgetStreakSummary(
    val currentStreak: Int,
    val bestStreak: Int,
    val todaySeconds: Long,
    val totalSeconds: Long,
    val bookCount: Int,
    val dailyTotals: Map<Long, Long>
)

object FolioWidgetUpdater {
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val TAB_STATS = "stats"
    const val EXTRA_OPEN_BOOK_ID = "extra_open_book_id"

    private const val THROTTLE_MS = 30_000L
    @Volatile
    private var lastUpdateAtMs: Long = 0L

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun computeWidgetSummary(
        sessions: List<ReadingSessionEntity>,
        books: List<BookEntity>,
        todayEpoch: Long = currentLocalEpochDay()
    ): WidgetStreakSummary {
        val dailyTotals = sessions.groupBy { it.epochDay }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
        val (currentStreak, bestStreak) = calculateStreaks(dailyTotals, todayEpoch)
        val todaySeconds = dailyTotals[todayEpoch] ?: 0L
        val sessionTotal = sessions.sumOf { it.durationSeconds }
        val bookTotal = books.sumOf { it.totalReadingSeconds }
        val totalSeconds = if (sessionTotal > 0L) sessionTotal else bookTotal
        return WidgetStreakSummary(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            todaySeconds = todaySeconds,
            totalSeconds = totalSeconds,
            bookCount = books.size,
            dailyTotals = dailyTotals
        )
    }

    fun requestUpdate(context: Context, throttled: Boolean = false) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return
        val streakIds = manager.getAppWidgetIds(ComponentName(appContext, FolioStreakWidgetProvider::class.java))
        val continueIds = manager.getAppWidgetIds(ComponentName(appContext, FolioContinueWidgetProvider::class.java))
        if (streakIds.isEmpty() && continueIds.isEmpty()) return

        val now = System.currentTimeMillis()
        if (throttled && now - lastUpdateAtMs < THROTTLE_MS) return
        lastUpdateAtMs = now

        updateScope.launch {
            withTimeoutOrNull(8_000L) {
                if (streakIds.isNotEmpty()) {
                    updateStreakWidgetsBlocking(appContext, manager, streakIds)
                }
                if (continueIds.isNotEmpty()) {
                    updateContinueWidgetsBlocking(appContext, manager, continueIds)
                }
            }
        }
    }

    suspend fun updateStreakWidgetsBlocking(
        context: Context,
        manager: AppWidgetManager = AppWidgetManager.getInstance(context),
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val dao = FolioDatabase.getInstance(context).folioDao()
        val sessions = dao.getAllSessionsSnapshot()
        val books = dao.getAllBooksSnapshot()
        val todayEpoch = currentLocalEpochDay()
        val summary = computeWidgetSummary(sessions, books, todayEpoch)
        val isDark = resolveWidgetDarkMode(context)

        val heatmapBitmap = renderHeatmapBitmap(
            dailyTotals = summary.dailyTotals,
            todayEpoch = todayEpoch,
            isDark = isDark,
            weekCount = 18
        )

        val streakLabel = if (summary.currentStreak > 0) {
            "${summary.currentStreak}d streak"
        } else {
            "0d streak"
        }
        val bestLabel = "Best ${summary.bestStreak}d"
        val todayLabel = if (summary.todaySeconds > 0L) {
            "Today ${formatShortDuration(summary.todaySeconds)}"
        } else {
            "Today 0m"
        }
        val totalLabel = "Total ${formatShortDuration(summary.totalSeconds)} • ${summary.bookCount} book${if (summary.bookCount == 1) "" else "s"}"

        val openStatsIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.widget.OPEN_STATS"
            putExtra(EXTRA_OPEN_TAB, TAB_STATS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openStatsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak).apply {
                setTextViewText(R.id.widget_streak_badge, streakLabel)
                setTextViewText(R.id.widget_best_streak, bestLabel)
                setTextViewText(R.id.widget_today_pill, todayLabel)
                setTextViewText(R.id.widget_total_time, totalLabel)
                setTextViewText(
                    R.id.widget_streak_status_hint,
                    if (summary.todaySeconds > 0L) "Active today" else "Stats →"
                )
                setImageViewBitmap(R.id.widget_heatmap_image, heatmapBitmap)
                setOnClickPendingIntent(R.id.widget_streak_root, pendingIntent)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }

    suspend fun updateContinueWidgetsBlocking(
        context: Context,
        manager: AppWidgetManager = AppWidgetManager.getInstance(context),
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val dao = FolioDatabase.getInstance(context).folioDao()
        val sessions = dao.getAllSessionsSnapshot()
        val books = dao.getAllBooksSnapshot()
        val todayEpoch = currentLocalEpochDay()
        val summary = computeWidgetSummary(sessions, books, todayEpoch)
        val candidate = dao.getLatestBookForWidget()
        val isDark = resolveWidgetDarkMode(context)

        val streakText = "${summary.currentStreak}d streak"
        val coverBmp = buildWidgetCoverBitmap(candidate, isDark)

        val tapIntent = if (candidate != null) {
            Intent(context, MainActivity::class.java).apply {
                action = "com.example.widget.OPEN_BOOK"
                putExtra(EXTRA_OPEN_BOOK_ID, candidate.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                action = "com.example.widget.OPEN_LIBRARY"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_continue_reading).apply {
                setImageViewBitmap(R.id.widget_book_cover, coverBmp)
                setTextViewText(R.id.widget_continue_streak, streakText)
                if (candidate != null) {
                    val pct = candidate.progressPercent.roundToInt().coerceIn(0, 100)
                    setTextViewText(R.id.widget_book_format, candidate.format.displayName)
                    setTextViewText(R.id.widget_book_title, candidate.title)
                    setTextViewText(R.id.widget_book_author, candidate.author)
                    setProgressBar(R.id.widget_book_progress, 100, pct, false)
                    val meta = if (candidate.totalReadingSeconds > 0L) {
                        "$pct% • ${formatShortDuration(candidate.totalReadingSeconds)} read"
                    } else if (pct > 0) {
                        "$pct% read"
                    } else {
                        "Tap to start reading"
                    }
                    setTextViewText(R.id.widget_book_meta, meta)
                } else {
                    setTextViewText(R.id.widget_book_format, "FOLIO")
                    setTextViewText(R.id.widget_book_title, context.getString(R.string.empty_library_title))
                    setTextViewText(R.id.widget_book_author, "Import EPUB, PDF, MOBI, CBZ & more")
                    setProgressBar(R.id.widget_book_progress, 100, 0, false)
                    setTextViewText(R.id.widget_book_meta, "100% offline • Tap to open")
                }
                setOnClickPendingIntent(R.id.widget_continue_root, pendingIntent)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun resolveWidgetDarkMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun renderHeatmapBitmap(
        dailyTotals: Map<Long, Long>,
        todayEpoch: Long,
        isDark: Boolean,
        weekCount: Int = 18
    ): Bitmap {
        val width = 640
        val height = 210
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val emptyColor = if (isDark) 0xFF232426.toInt() else 0xFFE7E2D8.toInt()
        val greenTiers = if (isDark) {
            intArrayOf(0xFF215E33.toInt(), 0xFF2EA043.toInt(), 0xFF39C555.toInt(), 0xFF4ADE80.toInt())
        } else {
            intArrayOf(0xFF86EFAC.toInt(), 0xFF4ADE80.toInt(), 0xFF22C55E.toInt(), 0xFF16A34A.toInt())
        }
        val monthTextColor = if (isDark) 0xFFA19A90.toInt() else 0xFF6A6157.toInt()
        val todayRingColor = if (isDark) 0xFF4ADE80.toInt() else 0xFF16A34A.toInt()

        val monthHeaderH = 26f
        val gap = 6f
        val gridTop = monthHeaderH + 4f
        val cellByWidth = (width - gap * (weekCount - 1)) / weekCount.toFloat()
        val cellByHeight = (height - gridTop - gap * 6f) / 7f
        val cellSize = minOf(cellByWidth, cellByHeight)
        val totalGridW = cellSize * weekCount + gap * (weekCount - 1)
        val leftPad = ((width - totalGridW) / 2f).coerceAtLeast(0f)
        val step = cellSize + gap
        val corner = (cellSize * 0.24f).coerceIn(3f, 8f)

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = todayEpoch * 86_400_000L
        }
        val todayDow = (utcCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val currentWeekMonday = todayEpoch - todayDow
        val startMonday = currentWeekMonday - (weekCount - 1) * 7L

        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monthTextColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val markers = mutableListOf<Pair<Int, String>>()
        for (col in 0 until weekCount) {
            val weekStart = startMonday + col * 7L
            for (row in 0..6) {
                val d = weekStart + row
                if (d <= todayEpoch) {
                    utcCal.timeInMillis = d * 86_400_000L
                    if (utcCal.get(Calendar.DAY_OF_MONTH) == 1) {
                        markers += col to monthFmt.format(Date(d * 86_400_000L))
                        break
                    }
                }
            }
        }
        val firstMonth = monthFmt.format(Date(startMonday * 86_400_000L))
        if (markers.isEmpty() || markers.first().first >= 3) {
            markers.add(0, 0 to firstMonth)
        }
        markers.filterIndexed { idx, pair ->
            idx == 0 || pair.first - markers[idx - 1].first >= 2
        }.forEach { (col, label) ->
            canvas.drawText(label, leftPad + col * step, 19f, monthPaint)
        }

        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = todayRingColor
        }
        val rect = RectF()
        for (col in 0 until weekCount) {
            for (row in 0 until 7) {
                val dayEpoch = startMonday + col * 7L + row
                if (dayEpoch > todayEpoch) continue
                val seconds = dailyTotals[dayEpoch] ?: 0L
                tilePaint.color = when {
                    seconds <= 0L -> emptyColor
                    seconds < 300L -> greenTiers[0]
                    seconds < 1200L -> greenTiers[1]
                    seconds < 2700L -> greenTiers[2]
                    else -> greenTiers[3]
                }
                val x = leftPad + col * step
                val y = gridTop + row * step
                rect.set(x, y, x + cellSize, y + cellSize)
                canvas.drawRoundRect(rect, corner, corner, tilePaint)
                if (dayEpoch == todayEpoch && seconds <= 0L) {
                    rect.inset(1.1f, 1.1f)
                    canvas.drawRoundRect(rect, corner, corner, ringPaint)
                }
            }
        }
        return bitmap
    }

    private fun buildWidgetCoverBitmap(book: BookEntity?, isDark: Boolean): Bitmap {
        val targetW = 160
        val targetH = 230
        val coverFile = book?.coverPath?.let { File(it) }?.takeIf { it.exists() }
        if (coverFile != null) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverFile.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > targetW * 2 || bounds.outHeight / sample > targetH * 2) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeFile(coverFile.absolutePath, opts)
            if (decoded != null && decoded.width > 0 && decoded.height > 0) {
                val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val scale = maxOf(targetW.toFloat() / decoded.width, targetH.toFloat() / decoded.height)
                val scaledW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
                val scaledH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true)
                val dx = (targetW - scaledW) / 2f
                val dy = (targetH - scaledH) / 2f
                val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = android.graphics.Matrix().apply { setTranslate(dx, dy) }
                shader.setLocalMatrix(matrix)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                canvas.drawRoundRect(RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()), 18f, 18f, paint)
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
                return out
            }
        }

        // Fallback monogram cover bitmap
        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF3A271E.toInt() else 0xFFC86D3B.toInt()
        }
        canvas.drawRoundRect(RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()), 18f, 18f, bgPaint)
        val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
        }
        canvas.drawRect(14f, 0f, 19f, targetH.toFloat(), spinePaint)
        val initials = (book?.title ?: "Folio")
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "F" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFAF6F0.toInt()
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = targetH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, targetW / 2f + 4f, textY, textPaint)
        return out
    }

    private fun formatShortDuration(totalSeconds: Long): String {
        val mins = totalSeconds / 60L
        val hours = mins / 60L
        val remMins = mins % 60L
        return when {
            hours > 0 -> "${hours}h ${remMins}m"
            mins > 0 -> "${mins}m"
            else -> "${totalSeconds}s"
        }
    }
}
