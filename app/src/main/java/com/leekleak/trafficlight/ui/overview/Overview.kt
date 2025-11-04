package com.leekleak.trafficlight.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.ui.history.SummaryItem
import com.leekleak.trafficlight.ui.history.dayUsageToBarData

@Composable
fun Overview(
    paddingValues: PaddingValues
) {
    val viewModel = OverviewVM()

    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues
    ) {
        TodayOverview(viewModel)
    }
}

fun LazyListScope.TodayOverview(viewModel: OverviewVM) {
    item {
        Text(
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
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
                    painter = painterResource(R.drawable.wifi),
                    tint = MaterialTheme.colorScheme.primary,
                    data = { usage.totalWifi() }
                )
                SummaryItem(
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