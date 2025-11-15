package com.leekleak.trafficlight.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall

@Composable
fun Settings(
    paddingValues: PaddingValues
) {
    val viewModel = SettingsVM()
    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.settings)
        categoryTitleSmall(R.string.notifications)
        PreferenceCategory {
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
        categoryTitleSmall(R.string.history)
        PreferenceCategory {
            val dbSize by viewModel.dbSize.collectAsState()
            Preference(
                title = "Clear History",
                summary = null,
                icon = null,
                onClick = { viewModel.clearDB() },
                controls = {
                    Text("${dbSize/24} days")
                }
            )
        }
    }
}