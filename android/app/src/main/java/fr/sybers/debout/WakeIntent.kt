package fr.sybers.debout

import android.content.Context
import android.content.Intent

/** Opens the app on a machine and wakes it: the entry point of app shortcuts, and of tiles that need a permission first. */
object WakeIntent {
    private const val ACTION = "fr.sybers.debout.action.WAKE"
    private const val EXTRA_MACHINE_ID = "machine_id"

    fun create(context: Context, machineId: Long): Intent =
        Intent(ACTION, null, context, MainActivity::class.java).putExtra(EXTRA_MACHINE_ID, machineId)

    /** Null for any other intent, and for one replayed from recents, which must not wake again. */
    fun machineId(intent: Intent?): Long? {
        if (intent?.action != ACTION) return null
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return intent.getLongExtra(EXTRA_MACHINE_ID, 0).takeIf { it != 0L }
    }
}
