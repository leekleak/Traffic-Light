package com.leekleak.trafficlight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leekleak.trafficlight.model.AppTheme
import com.leekleak.trafficlight.services.UsageService.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.ui.App

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        createNotificationChannel()

        setContent {
            AppTheme {
                App()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Traffic Light Service"
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}