package com.leekleak.trafficlight.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.util.WideScreenWrapper
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

sealed interface NavKeys : NavKey {
    @Serializable
    data object Overview : NavKeys
    @Serializable
    data object History : NavKeys
    @Serializable
    data object Settings : NavKeys

    companion object{
        val items = listOf<NavKeys>(Overview, History, Settings)

        val stateSaver = Saver<NavKeys, String>(
            save = { it::class.qualifiedName },
            restore = { qualifiedClass ->
                items.firstOrNull { it::class.qualifiedName == qualifiedClass } ?: NavKeys.Overview
            }
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationManager() {
    var currentTab by rememberSaveable(stateSaver = NavKeys.stateSaver) { mutableStateOf(NavKeys.Overview) }
    val overviewBackStack = rememberNavBackStack(NavKeys.Overview)
    val historyBackStack = rememberNavBackStack(NavKeys.History)
    val settingsBackStack = rememberNavBackStack(NavKeys.Settings)
    val backStack = when (currentTab) {
        NavKeys.Overview -> overviewBackStack
        NavKeys.History -> historyBackStack
        NavKeys.Settings -> settingsBackStack
    }

    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ContentPadding.calculateBottomPadding() * 2

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding + toolbarOffset
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
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
    ) {
        WideScreenWrapper {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<NavKeys.Overview> { Overview(paddingValues) }
                    entry<NavKeys.History> { History(paddingValues) }
                    entry<NavKeys.Settings> { Settings(paddingValues) }
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                popTransitionSpec = { fadeIn() togetherWith fadeOut() },
                predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
            )
        }
    }
}


@Composable
fun NavigationButton(currentBackstack: NavKeys, route: NavKeys, icon: Int, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val animation = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    IconButton(
        modifier = Modifier.scale(animation.value),
        colors =
            if (currentBackstack == route){
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
            scope.launch {
                animation.snapTo(0.9f)
                animation.animateTo(1f)
            }
        }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}