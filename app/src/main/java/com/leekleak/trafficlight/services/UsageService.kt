package com.leekleak.trafficlight.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Paint
import android.os.IBinder
import android.util.Log
import androidx.compose.ui.graphics.NativeCanvas
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepository
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.SizeFormatter.Companion.smartFormat
import com.leekleak.trafficlight.util.clipAndPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class UsageService : Service(), KoinComponent {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private val dayUsageRepo: DayUsageRepository by inject(DayUsageRepository::class.java)
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var notificationBuilder = NotificationCompat.Builder(this, "N")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Traffic Light")
        .setChannelId(NOTIFICATION_CHANNEL_ID)
        .setOngoing(true)
        .setSilent(true)
        .setWhen(Long.MAX_VALUE) // Keep above other notifications
        .setShowWhen(false) // Hide timestamp

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    private var screenOn: Boolean = true
    /*private val screenStateReceiver = object : BroadcastReceiver() {
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
    }*/

    override fun onCreate() {
        super.onCreate()
        instance = this
        /*registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })*/
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
            updateNotification(null)

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
                if (screenOn) launch { updateNotification(trafficSnapshot) }
                launch { updateDatabase(trafficSnapshot) }

                nextTick += 1_000_000_000L
                delay((nextTick - System.nanoTime()) / 1_000_000)
            }
        }
    }

    private fun updateDatabase(trafficSnapshot: TrafficSnapshot?) {
        if (trafficSnapshot == null) return

        val dateTime = LocalDateTime.now()
        if (dayUsageRepo.dayUsageExists(dateTime.toLocalDate())) {
            val usage = runBlocking { dayUsageRepo.getDayUsage(dateTime.toLocalDate()).first() }
            val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())

            usage?.let {
                val stamp = dateTime.truncatedTo(ChronoUnit.HOURS).toInstant(timezone).toEpochMilli()

                it.hours[stamp] = it.hours.getOrPut(stamp) {
                    HourUsage()
                }.plus(trafficSnapshot.toHourUsage())

                dayUsageRepo.updateDayUsage(it)
            }
        } else {
            dayUsageRepo.addDayUsage(DayUsage(dateTime.toLocalDate()))
        }
    }


    var lastSnapshot: TrafficSnapshot = TrafficSnapshot()
    private fun updateNotification(trafficSnapshot: TrafficSnapshot?) {
        if (
            trafficSnapshot != null &&
            smartFormat(lastSnapshot.totalSpeed, true) ==
            smartFormat(trafficSnapshot.totalSpeed, true) &&
            smartFormat(lastSnapshot.downSpeed, true) ==
            smartFormat(trafficSnapshot.downSpeed, true) &&
            smartFormat(lastSnapshot.upSpeed, true) ==
            smartFormat(trafficSnapshot.upSpeed, true)
        ) {
            Log.i("UsageService", "Skipped notification update")
            return
        }

        val snapshot = trafficSnapshot ?: TrafficSnapshot()

        lastSnapshot = snapshot.copy()

        val todayUsage = runBlocking { dayUsageRepo.getTodayUsage().first() }
        val speedFormatter = SizeFormatter(true, 0)
        val sizeFormatter = SizeFormatter(false, 2)
        val title = getString(R.string.speed, speedFormatter.format(snapshot.totalSpeed))
        val spacing = 18
        val messageShort =
            "\uD83D\uDEDC: ${sizeFormatter.format(todayUsage?.totalWifi() ?: 0)}".clipAndPad(spacing) +
            "\uD83D\uDCF6: ${sizeFormatter.format(todayUsage?.totalCellular() ?: 0)}".clipAndPad(spacing)
        val message =
            "\uD83D\uDEDC: ${sizeFormatter.format(todayUsage?.totalWifi() ?: 0)}\n".clipAndPad(spacing) +
            "\uD83D\uDCF6: ${sizeFormatter.format(todayUsage?.totalCellular() ?: 0)}\n".clipAndPad(spacing) +
            "⬇\uFE0F: ${speedFormatter.format(snapshot.downSpeed)}\n".clipAndPad(spacing) +
            "⬆\uFE0F: ${speedFormatter.format(snapshot.upSpeed)}\n".clipAndPad(spacing)

        notification = notificationBuilder
            .setSmallIcon(createIcon(snapshot))
            .setContentTitle(title)
            .setContentText(messageShort)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        notification?.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    fun createIcon(snapshot: TrafficSnapshot): IconCompat {
        val bitmap = createBitmap(96, 96)
        val canvas = NativeCanvas(bitmap)

        val text = smartFormat(snapshot.totalSpeed, true)
        val speed = text.take(text.indexOfFirst { it.isLetter() })
        val unit = text.substring(text.indexOfFirst { it.isLetter() })

        val paint = Paint().apply {
            color = ContextCompat.getColor(this@UsageService, R.color.white)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = resources.getFont(R.font.roboto_condensed_semi_bold)
            letterSpacing = -0.05f
        }
        canvas.drawText(speed, 48f, 56f, paint)

        paint.apply {
            textSize = 46f
            letterSpacing = 0f
        }
        canvas.drawText(unit, 48f, 96f, paint)

        return IconCompat.createWithBitmap(bitmap)
    }

    companion object {
        const val NOTIFICATION_ID = 228
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"

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
