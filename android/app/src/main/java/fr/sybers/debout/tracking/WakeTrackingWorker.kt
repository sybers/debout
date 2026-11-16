package fr.sybers.debout.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.sybers.debout.appContainer
import fr.sybers.debout.domain.WakeOutcome
import fr.sybers.debout.domain.awaitWakeOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps the process alive and probing while a wake runs, so its outcome shows in a
 * notification even once the app or the tile panel is closed. Runs in the app's process: it
 * follows the wake that the shared [fr.sybers.debout.network.WakeCoordinator] drives.
 */
class WakeTrackingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notifications = WakeNotifications(context)
    private val machineId = inputData.getLong(KEY_MACHINE_ID, 0)
    private val startedAt = inputData.getLong(KEY_STARTED_AT, System.currentTimeMillis())

    /** Only called below Android 12, where an expedited worker runs as a foreground service. */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val name = machineName().orEmpty()
        return ForegroundInfo(notifications.progressId(machineId), notifications.booting(name, startedAt))
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        val name = machineName() ?: return@withContext Result.success()
        val progress = launch {
            // On the first wake, the permission dialog only opens once the wake has started.
            while (!applicationContext.canPostNotifications()) delay(PERMISSION_POLL)
            notifications.showBooting(machineId, name, startedAt)
        }
        val outcome = withTimeoutOrNull(TRACKING_LIMIT) {
            applicationContext.appContainer.wakeCoordinator.watch(machineId).map { it.state }.awaitWakeOutcome()
        }
        progress.cancel()
        notifications.showOutcome(machineId, name, outcome ?: WakeOutcome.Abandoned)
        Result.success()
    }

    private suspend fun machineName(): String? = applicationContext.appContainer.machines.get(machineId)?.name

    companion object {
        private const val KEY_MACHINE_ID = "machine_id"
        private const val KEY_STARTED_AT = "started_at"

        /** A wake gives up after ~90 s: past this, something went wrong and the notification goes away. */
        private val TRACKING_LIMIT = 2.minutes
        private val PERMISSION_POLL = 500.milliseconds

        fun enqueue(context: Context, machineId: Long) {
            val request = OneTimeWorkRequestBuilder<WakeTrackingWorker>()
                .setInputData(workDataOf(KEY_MACHINE_ID to machineId, KEY_STARTED_AT to System.currentTimeMillis()))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("wake-$machineId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
