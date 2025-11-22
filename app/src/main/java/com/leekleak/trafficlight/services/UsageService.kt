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
import android.graphics.Bitmap
import android.graphics.Color
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
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.clipAndPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

class UsageService : Service(), KoinComponent {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    private val preferenceRepo: PreferenceRepo by inject()
    private val notificationManager: NotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private var notification: Notification? = null
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, "N")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Traffic Light")
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSilent(true)
            .setWhen(Long.MAX_VALUE) // Keep above other notifications
            .setShowWhen(false) // Hide timestamp
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (job == null) startJob()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (!aodMode) {
                        job?.cancel()
                        job = null
                    }
                }
            }
        }
    }

    var forceUpdate = false
    var bigIcon = false
    var aodMode = false

    private val formatter by lazy { SizeFormatter() }
    var minSizeWifi: Long = DataSize(todayUsage.totalWifi.toFloat()).unit.getSize(2)
    var minSizeMobile: Long = DataSize(todayUsage.totalCellular.toFloat()).unit.getSize(2)

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
            preferenceRepo.modeAOD.collect {
                aodMode = it
            }
        }
        serviceScope.launch {
            preferenceRepo.bigIcon.collect {
                forceUpdate = true
                bigIcon = it
            }
        }
        serviceScope.launch {
            preferenceRepo.speedBits.collect {
                formatter.asBits = it
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
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

            try {
                notification?.let {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        it,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                    )
                }
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
            trafficSnapshot.setCurrentAsLast()

            var dataUsedWifi = 0L
            var dataUsedMobile = 0L
            var lastSpeed = 0
            while (true) {
                trafficSnapshot.updateSnapshot()


                /**
                 * If network speed is changing rapidly, we use this while loop to self-calibrate
                 * the refresh timing to match the timing of the TrafficStats API updates.
                 *
                 * If network speed is not changing rapidly (i.e. it's zero)
                 * it's quite likely that the next tick will also be zero, so we ignore that and
                 * simply sleep for 1 second
                 */
                if (lastSpeed != 0) {
                    val tick = System.nanoTime()
                    while (trafficSnapshot.isCurrentSameAsLast() && System.nanoTime() < tick + 250_000_000) {
                        delay(100)
                        trafficSnapshot.updateSnapshot()
                    }
                }

                dataUsedWifi += max(trafficSnapshot.wifiSpeed, 0)
                dataUsedMobile += max(trafficSnapshot.mobileSpeed, 0)

                if (dataUsedWifi > minSizeWifi || dataUsedMobile > minSizeMobile) {
                    updateDatabase()
                    if (dataUsedWifi > minSizeWifi) dataUsedWifi = 0
                    if (dataUsedMobile > minSizeMobile) dataUsedMobile = 0
                }

                lastSpeed = trafficSnapshot.totalSpeed.toInt()

                updateNotification(trafficSnapshot)
                trafficSnapshot.setCurrentAsLast()

                delay(if (lastSpeed != 0) 900 else 1000)
            }
        }
    }

    private fun updateDatabase() {
        val dateTime = LocalDateTime.now()

        if (todayUsage.date != LocalDate.now()) {
            todayUsage = hourlyUsageRepo.calculateDayUsage(LocalDate.now())
        } else {
            val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())

            val stamp = dateTime.truncatedTo(ChronoUnit.HOURS).toInstant(timezone).toEpochMilli()
            val stampNow = dateTime.toInstant(timezone).toEpochMilli()

            todayUsage = todayUsage.copy(
                hours = todayUsage.hours.toMutableMap().apply {
                    this[stamp] = hourlyUsageRepo.calculateHourData(stamp, stampNow)
                }
            ).also { it.categorizeUsage() }
        }
        minSizeWifi = DataSize(todayUsage.totalWifi.toFloat()).unit.getSize(2)
        minSizeMobile = DataSize(todayUsage.totalCellular.toFloat()).unit.getSize(2)
    }

    var lastSnapshot: TrafficSnapshot = TrafficSnapshot(-1)
    private fun updateNotification(trafficSnapshot: TrafficSnapshot) {
        val skip = formatter.format(lastSnapshot.totalSpeed, 2, true) ==
                   formatter.format(trafficSnapshot.totalSpeed, 2, true)
        if (skip && !forceUpdate) return
        forceUpdate = false

        lastSnapshot = trafficSnapshot.copy()

        val title = getString(R.string.speed, formatter.format(trafficSnapshot.totalSpeed, 2, true))
        val spacing = 18
        val messageShort =
            getString(R.string.wi_fi, formatter.format(todayUsage.totalWifi, 2)).clipAndPad(spacing) +
            getString(R.string.mobile, formatter.format(todayUsage.totalCellular, 2))

        notification = notificationBuilder
            .setSmallIcon(createIcon(trafficSnapshot))
            .setContentTitle(title)
            .setContentText(messageShort)
            .build()
        notification?.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private val paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(this@UsageService, R.color.white)
            typeface = resources.getFont(R.font.roboto_condensed_semi_bold)
            textAlign = Paint.Align.CENTER
        }
    }
    private var bitmap: Bitmap? = null
    private var lastBitmapData: String = ""
    fun createIcon(snapshot: TrafficSnapshot): IconCompat {
        val density = Density(this@UsageService)
        val multiplier = 24 * density.density / 96f * if (bigIcon) 2f else 1f
        val height = (96 * multiplier).toInt()

        val data = formatter.partFormat(snapshot.totalSpeed, true)
        if (lastBitmapData == data.toString() && !forceUpdate) {
            Log.e("leekleak","Skipped updating")
            return IconCompat.createWithBitmap(bitmap!!)
        } else {
            lastBitmapData = data.toString()
        }

        if (bitmap == null || bitmap!!.height != height) {
            bitmap = createBitmap(height, height)
        } else {
            bitmap?.eraseColor(Color.TRANSPARENT)
        }

        val canvas = NativeCanvas(bitmap!!)

        val bytesPerSecond: Boolean = data[2].lowercase() == "b/s"
        val speed = if (!bytesPerSecond || snapshot.totalSpeed == 0L) {
            data[0] + if (data[0].length == 1 && data[1].isNotEmpty()) "." + data[1] else ""
        } else "<1"
        val unit = if (!bytesPerSecond) data[2] else "K${data[2]}"

        paint.apply {
            textSize = 72f * multiplier
            letterSpacing = -0.05f * multiplier
        }
        canvas.drawText(speed, 48f * multiplier, 56f * multiplier, paint)

        paint.apply {
            textSize = 46f * multiplier
            letterSpacing = 0f * multiplier
        }
        canvas.drawText(unit, 48f * multiplier, 96f * multiplier, paint)

        return IconCompat.createWithBitmap(bitmap!!)
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
