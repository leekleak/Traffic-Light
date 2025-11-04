package com.leekleak.trafficlight.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        for (u in 1..5) {
            item {
                PreferenceCategory(title = "Notification") {
                    val state = remember { mutableStateOf(false) }
                    Preference("Hee $u", "AA")
                    SwitchPreference("Hee $u", null, value = state.value) {state.value = it}
                }
            }
        }
    }
}