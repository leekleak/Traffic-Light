package com.leekleak.trafficlight.ui.permissions

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Permissions(
    notifPermission: Boolean,
    backgroundPermission: Boolean,
    usagePermission: Boolean
) {
    val activity = LocalActivity.current
    val viewModel = PermissionVM()

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding + 8.dp,
            bottom = bottomPadding + 8.dp
        )

    Scaffold {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues
        ) {
            item {
                Text(
                    modifier = Modifier.padding(8.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    text = stringResource(R.string.permissions)
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.notification_permission),
                    description = stringResource(R.string.notification_permission_description),
                    enabled = !notifPermission,
                    onClick = { activity?.let { viewModel.allowNotifications(it) } }
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.battery_optimization),
                    description = stringResource(R.string.battery_optimization_description),
                    enabled = !backgroundPermission,
                    onClick = { activity?.let { viewModel.allowBackground(it) } }
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.usage_statistics),
                    description = stringResource(R.string.usage_statistics_description),
                    enabled = !usagePermission,
                    onClick = { activity?.let { viewModel.allowUsage(it) } }
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
