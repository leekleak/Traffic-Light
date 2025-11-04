package com.leekleak.trafficlight.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.permissions.Permissions
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable


@Serializable
object Overview
@Serializable
object History
@Serializable
object Settings
@Serializable
object Permissions

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
fun NavigationManager() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = NavigationVM()

    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ContentPadding.calculateBottomPadding() * 2

    val currentBackstack by navController.currentBackStackEntryAsState()
    val showToolbar = currentBackstack?.destination?.hasRoute(Permissions::class) == false
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

    val animationSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val popEnterOffset = -100.dp.px.toInt()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
        ) {
            NavHost(navController, if (!batteryOptimizationDisabled.value || !notifPermission.isGranted) Permissions else Overview,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = animationSpec
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = animationSpec
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = animationSpec
                    ) {popEnterOffset} + scaleIn(initialScale = 0.8f)
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    ) }
            ) {
                composable<Overview> { Overview(paddingValues) }
                composable<History> { History(paddingValues) }
                composable<Settings> { Settings(paddingValues) }
                composable<Permissions> { Permissions(notifPermission, batteryOptimizationDisabled.value, paddingValues) }
            }

            if (showToolbar) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                    colors = vibrantColors,
                    content = {
                        navigationButton(currentBackstack, navController, Overview, R.drawable.overview)
                        navigationButton(currentBackstack, navController, History, R.drawable.history)
                        navigationButton(currentBackstack, navController, Settings, R.drawable.settings)
                    },
                )
            }
        }
    }
}

fun NavController.safeNavigate(destination: Any) {
    if (this.currentDestination?.hasRoute(destination::class) == false) {
        this.navigate(destination)
    }
}


@Composable
fun navigationButton(currentBackstack: NavBackStackEntry?, navController: NavController, route: Any, icon: Int) {
    IconButton(
        colors =
            if (currentBackstack?.destination?.hasRoute(route::class) == true){
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        onClick = { navController.safeNavigate(route) }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}