package com.leekleak.trafficlight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.leekleak.trafficlight.model.AppTheme
import com.leekleak.trafficlight.services.UsageService.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.ui.navigation.NavigationManager

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        createNotificationChannel()

        setContent {
            AppTheme {
                NavigationManager()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Traffic Light Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}