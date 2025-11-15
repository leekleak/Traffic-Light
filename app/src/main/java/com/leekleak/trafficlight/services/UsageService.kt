package com.leekleak.trafficlight.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Paint
import android.os.IBinder
import android.util.Log
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.SizeFormatter.Companion.smartFormat
import com.leekleak.trafficlight.util.clipAndPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class UsageService : Service(), KoinComponent {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    private val preferenceRepo: PreferenceRepo by inject()
    private var notificationManager: NotificationManager? = null

    private var notification: Notification? = null
    private var notificationBuilder = NotificationCompat.Builder(this, "N")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Traffic Light")
        .setChannelId(NOTIFICATION_CHANNEL_ID)
        .setOngoing(true)
        .setWhen(Long.MAX_VALUE) // Keep above other notifications
        .setShowWhen(false) // Hide timestamp
    private var screenOn: Boolean = true
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                }
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        serviceScope.launch {
            preferenceRepo.bigIcon.collect { forceUpdate = true }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        serviceScope.cancel()
    }

    private fun onDismissedIntent(context: Context): PendingIntent? {
        val intent = Intent(context, DismissListener::class.java)
        intent.putExtra("com.leekleak.trafficlight.notificationId", NOTIFICATION_ID)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context.applicationContext,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job == null) {
            startJob()

            todayUsage = hourlyUsageRepo.calculateDayUsage(LocalDate.now())
            notificationBuilder
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java).apply {
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }, PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setDeleteIntent(
                    onDismissedIntent(this)
                )
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

            try {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification!!,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
            } catch (_: Exception) {
                Log.e("Traffic Light", "Failed to start foreground service")
            }
        }
        return START_STICKY
    }

    fun startJob() {
        job = serviceScope.launch {
            val trafficSnapshot = TrafficSnapshot()
            trafficSnapshot.updateSnapshot()
            var nextTick = System.nanoTime()
            while (true) {
                trafficSnapshot.updateSnapshot()
                if (screenOn || preferenceRepo.modeAOD.first()) launch { updateNotification(trafficSnapshot) }
                launch { updateDatabase() }

                nextTick += 1_000_000_000L
                delay((nextTick - System.nanoTime()) / 1_000_000)
            }
        }
    }

    private fun updateDatabase() {
        val dateTime = LocalDateTime.now()
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())

        val stamp = dateTime.truncatedTo(ChronoUnit.HOURS).toInstant(timezone).toEpochMilli()
        val stampNow = dateTime.toInstant(timezone).toEpochMilli()

        todayUsage = todayUsage.copy(
            hours = todayUsage.hours.toMutableMap().apply {
                this[stamp] = hourlyUsageRepo.getCurrentHourUsage(stamp, stampNow)
            }
        ).also { it.categorizeUsage() }
    }

    var forceUpdate = false
    var lastSnapshot: TrafficSnapshot = TrafficSnapshot()
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot?) {
        if (lastSnapshot.closeEnough(trafficSnapshot) && !forceUpdate) {
            forceUpdate = false
            Log.i("UsageService", "Skipped notification update: ${trafficSnapshot?.totalSpeed}")
            return
        }

        val snapshot = trafficSnapshot ?: TrafficSnapshot()

        lastSnapshot = snapshot.copy()

        val speedFormatter = SizeFormatter(true, 0)
        val sizeFormatter = SizeFormatter(false, 2)
        val title = getString(R.string.speed, speedFormatter.format(snapshot.totalSpeed))
        val spacing = 18
        val messageShort =
            "Wi-Fi: ${sizeFormatter.format(todayUsage.totalWifi)}".clipAndPad(spacing) +
            "Mobile: ${sizeFormatter.format(todayUsage.totalCellular)}"
        val message =
            "Wi-Fi: ${sizeFormatter.format(todayUsage.totalWifi)}\n" +
            "Mobile: ${sizeFormatter.format(todayUsage.totalCellular)}\n" +
            "Down: ${speedFormatter.format(snapshot.downSpeed)}\n" +
            "Up: ${speedFormatter.format(snapshot.upSpeed)}\n"

        notification = notificationBuilder
            .setSmallIcon(createIcon(snapshot))
            .setContentTitle(title)
            .setContentText(messageShort)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        notification?.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    suspend fun createIcon(snapshot: TrafficSnapshot): IconCompat {
        val bigIcon = if (preferenceRepo.bigIcon.first()) 2f else 1f
        val density = Density(this@UsageService)
        val multiplier = 24 * density.density * bigIcon / 96f

        val bitmap = createBitmap((96 * multiplier).toInt(), (96 * multiplier).toInt())
        val canvas = NativeCanvas(bitmap)

        val text = smartFormat(snapshot.totalSpeed, true)
        val speed = text.take(text.indexOfFirst { it.isLetter() })
        val unit = text.substring(text.indexOfFirst { it.isLetter() })

        val paint = Paint().apply {
            color = ContextCompat.getColor(this@UsageService, R.color.white)
            textSize = 72f * multiplier
            textAlign = Paint.Align.CENTER
            typeface = resources.getFont(R.font.roboto_condensed_semi_bold)
            letterSpacing = -0.05f * multiplier
        }
        canvas.drawText(speed, 48f * multiplier, 56f * multiplier, paint)

        paint.apply {
            textSize = 46f * multiplier
            letterSpacing = 0f * multiplier
        }
        canvas.drawText(unit, 48f * multiplier, 96f * multiplier, paint)

        return IconCompat.createWithBitmap(bitmap)
    }

    companion object {
        const val NOTIFICATION_ID = 228
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"

        private val _todayUsageFlow = MutableStateFlow(DayUsage())
        val todayUsageFlow = _todayUsageFlow.asStateFlow()
        var todayUsage: DayUsage
            get() = _todayUsageFlow.value
            set(value) {
                _todayUsageFlow.value = value
            }

        private var instance: UsageService? = null

        fun isInstanceCreated(): Boolean {
            return instance != null
        }

        fun startService(context: Context) {
            if (!isInstanceCreated()) {
                val intent = Intent(context, UsageService::class.java)
                context.startService(intent)
                Log.i("UsageService", "Started service")
            }
        }

        fun stopService() {
            if (isInstanceCreated()) {
                instance?.stopSelf()
                instance = null
            }
        }
    }
}
