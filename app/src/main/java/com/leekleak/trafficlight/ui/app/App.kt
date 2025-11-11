package com.leekleak.trafficlight.ui.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.leekleak.trafficlight.ui.navigation.NavigationManager
import com.leekleak.trafficlight.ui.permissions.Permissions
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App() {
    val activity = LocalActivity.current
    val viewModel = AppVM()

    val batteryOptimizationState = remember { mutableStateOf(false) }
    batteryOptimizationCheck(viewModel, batteryOptimizationState)

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS).status
    } else {
        PermissionStatus.Granted
    }

    LaunchedEffect(null) {
        while (true) {
            viewModel.runService(activity)
            delay(1000L)
        }
    }

    if (!batteryOptimizationState.value && notifPermission.isGranted) {
        NavigationManager()
    } else {
        Permissions(notifPermission, batteryOptimizationState.value)
    }
}

@ExperimentalPermissionsApi
@Composable
internal fun batteryOptimizationCheck(
    viewModel: AppVM,
    state: MutableState<Boolean>,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                state.value = !viewModel.batteryOptimizationDisabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}