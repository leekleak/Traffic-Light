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
            PreferenceCategory(title = "Notification") {
                val modeAOD by viewModel.modeAOD.collectAsState()
                SwitchPreference(
                    "AOD mode",
                    null,
                    value = modeAOD
                ) {
                    viewModel.setModeAOD(it)
                }
            }
        }
    }
}