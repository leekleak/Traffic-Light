package com.leekleak.trafficlight.ui.history

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.padHour
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun History(
    paddingValues: PaddingValues
) {
    val viewModel = HistoryVM()
    Dashboard(viewModel, paddingValues)
}

@Composable
fun Dashboard(viewModel: HistoryVM, paddingValues: PaddingValues) {
    val haptic = LocalHapticFeedback.current
    val pages = viewModel.usageHistoryFlow.collectAsLazyPagingItems()
    val maxSize = viewModel.getMaxCombinedUsage.collectAsState(0L)
    val selected = remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues
    ) {
        HistoryOverview(pages, maxSize.value, selected.intValue) { i: Int ->
            selected.intValue = i
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}

fun LazyListScope.HistoryOverview(
    pages: LazyPagingItems<DayUsage>,
    maxSize: Long,
    selected: Int,
    onClick: (i: Int) -> Unit
) {
    item {
        Text(
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(R.string.history)
        )
    }
    items(pages.itemCount, key = pages.itemKey { it.date }) { index ->
        pages[index]?.let {
            HistoryItem(maxSize, it, index, selected, onClick)
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
                            data = Pair(usage.totalWifi, usage.totalCellular)
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
                                value = usage.totalWifi
                            )
                            DataBadge(
                                iconId = R.drawable.cellular,
                                description = stringResource(R.string.cellular),
                                bgTint = MaterialTheme.colorScheme.tertiary,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                value = usage.totalCellular
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

fun classyFont(): FontFamily =
    FontFamily(
        Font(
            R.font.mendl_serif
        ),
    )
