package com.leekleak.trafficlight.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.padHour
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val viewModel = HomeScreenVM()

    //viewModel.pop()

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS).status
    } else {
        PermissionStatus.Granted
    }

    LaunchedEffect(null) {
        while (true) {
            viewModel.runService(activity)
            delay(5000L)
        }
    }

    Scaffold {
        if (notifPermission.isGranted) {
            Dashboard(viewModel)
        } else {
            Column (
                modifier = Modifier
                    .padding(it)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    text = stringResource(R.string.permissions)
                )
                PermissionCard(
                    title = stringResource(R.string.notification_permission_required),
                    description = stringResource(R.string.notification_permission_description),
                    enabled = !notifPermission.isGranted,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column (modifier = Modifier
        .clip(MaterialTheme.shapes.large)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, text = title)
        Text(modifier = Modifier.fillMaxWidth(), text = description)
        Button(enabled = enabled, onClick = onClick) { Text(stringResource(R.string.grant)) }
    }
}

@Composable
fun SummaryItem(
    modifier: Modifier,
    painter: Painter,
    tint: Color,
    data: () -> Long
) {
    Row (
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.Center,
    ) {
        val text = DataSize(value = data().toFloat(), precision = 2).toStringParts()
        val bigLetterCount = text.count {"04689".contains(it)}
        val spacing = when (bigLetterCount) {
            3 -> ((-2).sp)
            2 -> ((-1).sp)
            else -> 0.sp
        }

        Text(
            fontSize = 76.sp,
            text = text[0],
            fontFamily = chonkyFont(),
            letterSpacing = spacing,
            color = tint
        )
        Column (
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 6.dp, start = 2.dp),
            verticalArrangement = Arrangement.spacedBy((-8).dp)
        ) {
            Text(
                fontSize = 42.sp,
                text = "." + text[1].padEnd(2, '0'),
                fontFamily = chonkyFont(),
                color = tint
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    fontSize = 24.sp,
                    text = text[2],
                    fontFamily = chonkyFont(),
                    color = tint,
                )
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = tint,
                )
            }
        }
    }
}

@Composable
fun Dashboard(viewModel: HomeScreenVM) {
    val haptic = LocalHapticFeedback.current
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val usageHistory by viewModel.usageHistory(currentDate).collectAsState(listOf())
    var selected by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            if (currentDate != LocalDate.now()) {
                currentDate = LocalDate.now()
            }
            delay(5000L)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
        )
    ) {
        TodayOverview(viewModel)
        HistoryOverview(usageHistory, selected, onClick = { i: Int ->
            selected = i
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        })
    }
}

fun LazyListScope.TodayOverview(viewModel: HomeScreenVM) {
    item {
        Text(
            modifier = Modifier
                .padding(start = 8.dp, bottom = 8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(R.string.today)
        )
    }
    item {
        val usage by viewModel.todayUsage.collectAsState(DayUsage())

        val data = dayUsageToBarData(usage)
        Column (verticalArrangement = Arrangement.spacedBy(8.dp)){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                SummaryItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.wifi),
                    tint = MaterialTheme.colorScheme.primary,
                    data = { usage.totalWifi() }
                )
                SummaryItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.cellular),
                    tint = MaterialTheme.colorScheme.tertiary,
                    data = { usage.totalCellular() }
                )
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    BarGraph(
                        modifier = Modifier
                            .padding(top = 24.dp, start = 24.dp, bottom = 16.dp)
                            .height(150.dp),
                        data = data
                    )
                }
            }
        }
    }
}

fun LazyListScope.HistoryOverview(usageHistory: List<DayUsage?>, selected: Int, onClick: (i: Int) -> Unit) {
    item {
        Text(
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(R.string.history)
        )
    }
    if (usageHistory.count { it != null } == 0) {
        item {
            HistoryPlaceholder()
        }
    } else {
        val maximum = usageHistory.maxOf { day -> day?.hours?.entries?.sumOf { it.value.total } ?: 0 }
        usageHistory.forEachIndexed { i, it ->
            if (it != null) {
                item {
                    HistoryItem(maximum, it, i, selected, onClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun HistoryItem(
    maximum: Long,
    usage: DayUsage,
    i: Int,
    selected: Int,
    onClick: (i: Int) -> Unit
) {
    Column (
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
    ) {
        Box (
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onClick(if (selected != i) i else -1) },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column {
                    Text(
                        modifier = Modifier.width(86.sp.value.dp),
                        text = usage.date.toString().substring(5),
                        autoSize = TextAutoSize.StepBased(8.sp, 26.sp),
                        maxLines = 1,
                        fontFamily = classyFont(),
                        textAlign = TextAlign.Center
                    )
                    AnimatedVisibility(selected == i) {
                        Text(
                            modifier = Modifier.width(86.sp.value.dp),
                            text = usage.date.dayOfWeek
                                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                                .replaceFirstChar(Char::titlecase),
                            autoSize = TextAutoSize.StepBased(8.sp, 18.sp),
                            maxLines = 1,
                            fontFamily = classyFont(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedContent(selected == i) { selected ->
                    if (!selected) {
                        LineGraph(
                            maximum = maximum,
                            data = Pair(usage.totalWifi(), usage.totalCellular())
                        )
                    } else {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            DataBadge(
                                iconId = R.drawable.wifi,
                                description = stringResource(R.string.wifi),
                                bgTint = MaterialTheme.colorScheme.primary,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                value = usage.totalWifi()
                            )
                            DataBadge(
                                iconId = R.drawable.cellular,
                                description = stringResource(R.string.cellular),
                                bgTint = MaterialTheme.colorScheme.tertiary,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                value = usage.totalCellular()
                            )
                        }
                    }

                }

            }
        }
        AnimatedVisibility(
            visible = selected == i,
            enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
            exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
        ) {
            Box(modifier = Modifier.padding(2.dp)) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    BarGraph(
                        modifier = Modifier
                            .padding(top = 24.dp, start = 24.dp, bottom = 16.dp)
                            .height(150.dp),
                        data = dayUsageToBarData(usage)
                    )
                }
            }
        }
    }
}

@Composable
fun DataBadge (
    iconId: Int,
    description: String,
    bgTint: Color,
    tint: Color,
    value: Long
) {
    val sizeFormatter = remember { SizeFormatter(false, 1) }
    Box (modifier = Modifier.clip(MaterialTheme.shapes.small)) {
        Row(
            modifier = Modifier
                .background(bgTint)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy (4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = description,
                tint = tint
            )
            Text(
                text = sizeFormatter.format(value),
                color = tint
            )
        }
    }
}

@Composable
fun HistoryPlaceholder() {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Icon(
            modifier = Modifier.width(200.dp),
            painter = painterResource(R.drawable.fly),
            contentDescription = stringResource(R.string.fly),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.nothing_here),
            fontFamily = classyFont(),
            color = MaterialTheme.colorScheme.primary,
        )
    }

}

fun dayUsageToBarData(usage: DayUsage): List<BarData> {
    val data: MutableList<BarData> = mutableListOf()
    for (i in 0..22 step 2) {
        data.add(BarData(padHour(i), 0.0, 0.0))
    }
    for (i in usage.hours) {
        val hour = getHourFromMillis(i.key) / 2
        data[hour] += BarData(padHour(hour * 2), i.value.cellular.toDouble(), i.value.wifi.toDouble())
    }
    return data
}

fun getHourFromMillis(millis: Long): Int {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).hour
}

fun classyFont(): FontFamily = customFont(R.font.mendl_serif)
fun chonkyFont(): FontFamily = customFont(R.font.jaro)

@OptIn(ExperimentalTextApi::class)
fun customFont(id: Int): FontFamily {
    return FontFamily(
        Font(
            id,
        ),
    )
}


