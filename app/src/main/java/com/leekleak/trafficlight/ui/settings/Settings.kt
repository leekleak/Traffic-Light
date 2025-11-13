package com.leekleak.trafficlight.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R

@Composable
fun Settings(
    paddingValues: PaddingValues
) {
    val viewModel = SettingsVM()
    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = paddingValues
    ) {
    item {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(R.string.settings)
        )
    }
        item {
            PreferenceCategory(title = stringResource(R.string.notifications)) {
                val modeAOD by viewModel.modeAOD.collectAsState()
                SwitchPreference(
                    title = stringResource(R.string.screen_off_update),
                    summary = stringResource(R.string.screen_off_update_description),
                    icon = null,
                    value = modeAOD,
                    onValueChanged = { viewModel.setModeAOD(it) }
                )
                val bigIcon by viewModel.bigIcon.collectAsState()
                SwitchPreference(
                    title = stringResource(R.string.oversample_icon),
                    summary = stringResource(R.string.oversample_icon_description),
                    icon = null,
                    value = bigIcon,
                    onValueChanged = { viewModel.setBigIcon(it) }
                )
            }
            PreferenceCategory(title = "History") {
                val dbSize by viewModel.dbSize.collectAsState()
                Preference(
                    title = "Repopulate History",
                    summary = null,
                    icon = null,
                    onClick = { viewModel.repopulateDB() },
                    controls = {
                        Text("${dbSize/24} days")
                    }
                )
            }
        }
    }
}