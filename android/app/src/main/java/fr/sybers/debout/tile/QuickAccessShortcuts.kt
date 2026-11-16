package fr.sybers.debout.tile

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import fr.sybers.debout.R
import fr.sybers.debout.WakeIntent
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.QuickAccessSlot
import fr.sybers.debout.ui.machine.shortcutIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** One app shortcut per quick access slot, identified by the slot so a pinned shortcut follows the list order too. */
class QuickAccessShortcuts(private val context: Context) {

    suspend fun follow(machines: Flow<List<Machine>>) {
        machines
            .map { list -> list.take(QuickAccessSlot.entries.size).map { it.id to it.name } }
            .distinctUntilChanged()
            .collect(::publish)
    }

    private fun publish(machines: List<Pair<Long, String>>) {
        val shortcuts = machines.mapIndexed { index, (id, name) ->
            val slot = QuickAccessSlot.entries[index]
            ShortcutInfoCompat.Builder(context, "quick_access_${slot.number}")
                .setShortLabel(name)
                .setLongLabel(context.getString(R.string.shortcut_wake, name))
                .setIcon(IconCompat.createWithResource(context, slot.shortcutIcon))
                .setIntent(WakeIntent.create(context, id))
                .setRank(index)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
