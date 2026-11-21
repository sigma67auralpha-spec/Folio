package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ReadStatus
import com.example.ui.viewmodel.LibraryUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun currentLocalEpochDay(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): Long = (nowMillis + timeZone.getOffset(nowMillis)) / 86_400_000L

internal fun calculateStreaks(dailyTotals: Map<Long, Long>, todayEpoch: Long): Pair<Int, Int> {
    var currentStreak = 0
    var cursor = todayEpoch
    while ((dailyTotals[cursor] ?: 0L) > 0L) {
        currentStreak++
        cursor--
    }
    if (currentStreak == 0 && (dailyTotals[todayEpoch - 1] ?: 0L) > 0L) {
        cursor = todayEpoch - 1
        while ((dailyTotals[cursor] ?: 0L) > 0L) {
            currentStreak++
            cursor--
        }
    }

    val activeDays = dailyTotals.filterValues { it > 0L }.keys.sorted()
    var bestStreak = 0
    var run = 0
    var prevDay: Long? = null
    for (day in activeDays) {
        run = if (prevDay != null && day == prevDay + 1) run + 1 else 1
        if (run > bestStreak) bestStreak = run
        prevDay = day
    }
    return currentStreak to bestStreak
}

@Composable
fun StatsScreen(
    state: LibraryUiState,
    modifier: Modifier = Modifier
) {
    val totalSeconds = state.allBooks.sumOf { it.book.totalReadingSeconds }
    val finishedCount = state.allBooks.count { it.book.readStatus == ReadStatus.FINISHED }
    val totalHighlights = state.allBooks.sumOf { it.highlights.size }
    val todayEpoch = currentLocalEpochDay()

    val dailyTotals = remember(state.sessions) {
        state.sessions.groupBy { it.epochDay }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
    }

    val (streak, bestStreak) = remember(dailyTotals, todayEpoch) {
        calculateStreaks(dailyTotals, todayEpoch)
    }

    val isDark = state.preferences.appDarkTheme ?: isSystemInDarkTheme()

    val utcDayFmt = remember {
        SimpleDateFormat("EEE", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val last7Days = (6 downTo 0).map { offset ->
        val day = todayEpoch - offset
        val mins = ((dailyTotals[day] ?: 0L) / 60f)
        val label = utcDayFmt.format(Date(day * 86_400_000L))
        label to mins
    }

    val formatCounts = state.allBooks.groupBy { it.book.format.displayName }
        .mapValues { it.value.size }
        .entries.sortedByDescending { it.value }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Reading Journal", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Private on-device stats • Can be hidden anytime in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            StreakHeatmapCard(
                dailyTotals = dailyTotals,
                todayEpoch = todayEpoch,
                isDark = isDark
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard("Current Streak", "$streak day${if (streak == 1) "" else "s"}", Modifier.weight(1f))
                StatMetricCard("Best Streak", "$bestStreak day${if (bestStreak == 1) "" else "s"}", Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard("Time Read", formatDuration(totalSeconds), Modifier.weight(1f))
                StatMetricCard("Finished • Notes", "$finishedCount • $totalHighlights", Modifier.weight(1f))
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last 7 Days (Minutes)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    val barColor = MaterialTheme.colorScheme.secondary
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val maxMins = (last7Days.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(10f)
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        val barCount = last7Days.size
                        val slotWidth = size.width / barCount
                        val barWidth = slotWidth * 0.48f
                        last7Days.forEachIndexed { idx, (_, mins) ->
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            val h = (mins / maxMins).coerceIn(0f, 1f) * size.height
                            if (h > 1f) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, size.height - h),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        last7Days.forEach { (label, _) ->
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (formatCounts.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Library Formats", style = MaterialTheme.typography.titleMedium)
                        formatCounts.forEach { (fmt, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(fmt, style = MaterialTheme.typography.bodyMedium)
                                Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakHeatmapCard(
    dailyTotals: Map<Long, Long>,
    todayEpoch: Long,
    isDark: Boolean
) {
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    val cardBg = if (isDark) Color(0xFF171819) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) Color(0xFF2B2D30) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val emptyTile = if (isDark) Color(0xFF232426) else Color(0xFFE7E2D8)
    val greenTiers = if (isDark) {
        listOf(Color(0xFF215E33), Color(0xFF2EA043), Color(0xFF39C555), Color(0xFF4ADE80))
    } else {
        listOf(Color(0xFF86EFAC), Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF16A34A))
    }

    val monthFmt = remember {
        SimpleDateFormat("MMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val detailDateFmt = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val utcCal = remember(todayEpoch) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = todayEpoch * 86_400_000L
        }
    }
    // Monday = 0 .. Sunday = 6
    val todayDow = (utcCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val currentWeekMonday = todayEpoch - todayDow

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .testTag("streak_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gap = 3.5.dp
                val targetStep = 14.5.dp
                val weekCount = ((maxWidth + gap) / targetStep).toInt().coerceIn(18, 28)
                val cellSize = (maxWidth - gap * (weekCount - 1)) / weekCount
                val step = cellSize + gap
                val startMonday = currentWeekMonday - (weekCount - 1) * 7L

                val monthMarkers = remember(weekCount, startMonday) {
                    val markers = mutableListOf<Pair<Int, String>>()
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    for (col in 0 until weekCount) {
                        val weekStart = startMonday + col * 7L
                        var firstOfMonthDay: Long? = null
                        for (row in 0..6) {
                            val d = weekStart + row
                            if (d <= todayEpoch) {
                                cal.timeInMillis = d * 86_400_000L
                                if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
                                    firstOfMonthDay = d
                                    break
                                }
                            }
                        }
                        if (firstOfMonthDay != null) {
                            markers += col to monthFmt.format(Date(firstOfMonthDay * 86_400_000L))
                        }
                    }
                    // Include opening month at column 0 if there's room before the next month label
                    val firstColMonth = monthFmt.format(Date(startMonday * 86_400_000L))
                    if (markers.isEmpty() || markers.first().first >= 3) {
                        markers.add(0, 0 to firstColMonth)
                    }
                    markers.filterIndexed { index, pair ->
                        index == 0 || pair.first - markers[index - 1].first >= 2
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Month labels row aligned above week columns
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    ) {
                        monthMarkers.forEach { (col, label) ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                modifier = Modifier.offset(x = step * col)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 7-row x weekCount-column grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (col in 0 until weekCount) {
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                for (row in 0 until 7) {
                                    val dayEpoch = startMonday + col * 7L + row
                                    if (dayEpoch > todayEpoch) {
                                        Spacer(modifier = Modifier.size(cellSize))
                                    } else {
                                        val seconds = dailyTotals[dayEpoch] ?: 0L
                                        val tileColor = when {
                                            seconds <= 0L -> emptyTile
                                            seconds < 300L -> greenTiers[0]
                                            seconds < 1200L -> greenTiers[1]
                                            seconds < 2700L -> greenTiers[2]
                                            else -> greenTiers[3]
                                        }
                                        val isSelected = selectedDay == dayEpoch
                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .clip(RoundedCornerShape(3.5.dp))
                                                .background(tileColor)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.onSurface,
                                                            RoundedCornerShape(3.5.dp)
                                                        )
                                                    } else Modifier
                                                )
                                                .clickable {
                                                    selectedDay = if (selectedDay == dayEpoch) null else dayEpoch
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infoText = selectedDay?.let { day ->
                    val secs = dailyTotals[day] ?: 0L
                    val dateStr = detailDateFmt.format(Date(day * 86_400_000L))
                    if (secs > 0L) "$dateStr • ${formatDuration(secs)}" else "$dateStr • No reading logged"
                } ?: "Tap a day to inspect"
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(2.dp))
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(emptyTile))
                    greenTiers.forEach { c ->
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val mins = totalSeconds / 60
    val hours = mins / 60
    val remMins = mins % 60
    return when {
        hours > 0 -> "${hours}h ${remMins}m"
        mins > 0 -> "${mins}m"
        else -> "${totalSeconds}s"
    }
}
