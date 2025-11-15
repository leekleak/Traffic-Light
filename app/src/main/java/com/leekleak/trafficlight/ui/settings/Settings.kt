package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall

@Composable
fun Settings(
    paddingValues: PaddingValues
) {
    val viewModel = SettingsVM()
    val activity = LocalActivity.current
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
                icon = painterResource(R.drawable.aod),
                value = modeAOD,
                onValueChanged = { viewModel.setModeAOD(it) }
            )
            val bigIcon by viewModel.bigIcon.collectAsState()
            SwitchPreference(
                title = stringResource(R.string.oversample_icon),
                summary = stringResource(R.string.oversample_icon_description),
                icon = painterResource(R.drawable.oversample),
                value = bigIcon,
                onValueChanged = { viewModel.setBigIcon(it) }
            )
        }
        categoryTitleSmall(R.string.history)
        PreferenceCategory {
            val dbSize by viewModel.dbSize.collectAsState()
            Preference(
                title = stringResource(R.string.clear_history),
                summary = null,
                icon = painterResource(R.drawable.clear_history),
                onClick = { viewModel.clearDB() },
                controls = {
                    Text(stringResource(R.string.days, dbSize / 24))
                }
            )
        }
        categoryTitleSmall(R.string.about)
        PreferenceCategory {
            Preference(
                title = stringResource(R.string.github),
                summary = null,
                icon = painterResource(R.drawable.github),
                onClick = { viewModel.openGithub(activity) },
            )
        }
    }
}