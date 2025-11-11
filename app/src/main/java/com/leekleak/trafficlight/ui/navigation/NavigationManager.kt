package com.leekleak.trafficlight.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.permissions.Permissions
import com.leekleak.trafficlight.ui.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable


@OptIn(ExperimentalMaterial3Api::class)
sealed interface NavKeys : NavKey {
    @Serializable
    data object Overview : NavKeys
    @Serializable
    data object History : NavKeys
    @Serializable
    data object Settings : NavKeys
    @Serializable
    data object Permissions : NavKeys

    companion object{
        val items = listOf<NavKeys>(Overview, History, Settings, Permissions)

        val stateSaver = Saver<NavKeys, String>(
            save = { it::class.qualifiedName },
            restore = { qualifiedClass ->
                items.firstOrNull { it::class.qualifiedName == qualifiedClass } ?: NavKeys.Overview
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
fun NavigationManager() {
    var currentTab by rememberSaveable(stateSaver = NavKeys.stateSaver) { mutableStateOf(NavKeys.Overview) }
    val overviewBackStack = rememberNavBackStack(NavKeys.Overview)
    val historyBackStack = rememberNavBackStack(NavKeys.History)
    val settingsBackStack = rememberNavBackStack(NavKeys.Settings)
    val permissionsBackStack = rememberNavBackStack(NavKeys.Permissions)
    val backStack = when (currentTab) {
        NavKeys.Overview -> overviewBackStack
        NavKeys.History -> historyBackStack
        NavKeys.Settings -> settingsBackStack
        NavKeys.Permissions -> permissionsBackStack
    }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = NavigationVM()

    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ContentPadding.calculateBottomPadding() * 2

    val showToolbar = backStack != NavKeys.Permissions
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding + if (showToolbar) toolbarOffset else 0.dp
        )

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS).status
    } else {
        PermissionStatus.Granted
    }

    val batteryOptimizationDisabled = remember { mutableStateOf(viewModel.batteryOptimizationDisabled(context)) }

    LaunchedEffect(null) {
        while (true) {
            viewModel.runService(activity)
            batteryOptimizationDisabled.value = viewModel.batteryOptimizationDisabled(context)
            delay(1000L)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showToolbar) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        colors = vibrantColors,
                        content = {
                            NavigationButton(
                                currentTab, NavKeys.Overview, R.drawable.overview
                            ) { currentTab = NavKeys.Overview }
                            NavigationButton(
                                currentTab, NavKeys.History, R.drawable.history
                            ) { currentTab = NavKeys.History }
                            NavigationButton(
                                currentTab, NavKeys.Settings, R.drawable.settings
                            ) { currentTab = NavKeys.Settings }
                        },
                    )
                }
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<NavKeys.Overview> { Overview(paddingValues) }
                entry<NavKeys.History> { History(paddingValues) }
                entry<NavKeys.Settings> { Settings(paddingValues) }
                entry<NavKeys.Permissions> { Permissions(notifPermission, batteryOptimizationDisabled.value, paddingValues) }
            }
        )
    }
}


@Composable
fun NavigationButton(currentBackstack: NavKeys, route: NavKeys, icon: Int, onClick: () -> Unit) {
    IconButton(
        colors =
            if (currentBackstack == route){
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        onClick = { onClick() }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}