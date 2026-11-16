package fr.sybers.debout.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

enum class WakeOutcome { Online, NoResponse, Abandoned }

/**
 * Follows a wake that has started until it ends. A state outside a wake means the wake was
 * cancelled (network lost) or never ran in this process: there is nothing to report.
 */
suspend fun Flow<WakeState>.awaitWakeOutcome(): WakeOutcome =
    mapNotNull {
        when (it) {
            WakeState.Sending, WakeState.Booting -> null
            WakeState.Online -> WakeOutcome.Online
            WakeState.NoResponse -> WakeOutcome.NoResponse
            WakeState.Unknown, WakeState.Offline -> WakeOutcome.Abandoned
        }
    }.first()
