package fr.sybers.debout.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.sybers.debout.MainActivity
import fr.sybers.debout.R
import fr.sybers.debout.domain.WakeOutcome
import kotlin.time.Duration.Companion.seconds

/** Below Android 13, ContextCompat answers with whether the user left the app's notifications on. */
@SuppressLint("InlinedApi")
fun Context.canPostNotifications(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** The notification that follows a wake: "starting…", then the outcome, which dismisses itself. */
class WakeNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_channel_wake))
                .build(),
        )
    }

    fun progressId(machineId: Long): Int = (machineId * 2).toInt()

    /** The outcome replaces the progress under another id: WorkManager may still own the progress one. */
    private fun outcomeId(machineId: Long): Int = progressId(machineId) + 1

    fun booting(name: String, startedAt: Long) = builder()
        .setContentTitle(context.getString(R.string.notification_booting, name))
        .setOngoing(true)
        .setWhen(startedAt)
        .setUsesChronometer(true)
        .setShowWhen(true)
        .build()

    fun showBooting(machineId: Long, name: String, startedAt: Long) =
        notify(progressId(machineId), booting(name, startedAt))

    fun showOutcome(machineId: Long, name: String, outcome: WakeOutcome) {
        manager.cancel(progressId(machineId))
        val title = when (outcome) {
            WakeOutcome.Online -> context.getString(R.string.notification_online, name)
            WakeOutcome.NoResponse -> context.getString(R.string.notification_no_response, name)
            WakeOutcome.Abandoned -> return
        }
        notify(
            outcomeId(machineId),
            builder()
                .setContentTitle(title)
                .setAutoCancel(true)
                .setTimeoutAfter(OUTCOME_DURATION.inWholeMilliseconds)
                .build(),
        )
    }

    private fun builder() = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_power)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setOnlyAlertOnce(true)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )

    /** Without the permission the wake is followed silently, in the app only. */
    @SuppressLint("MissingPermission") // Lint does not see through canPostNotifications.
    private fun notify(id: Int, notification: Notification) {
        if (!context.canPostNotifications()) return
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL = "wake_tracking"
        val OUTCOME_DURATION = 6.seconds
    }
}
