package fr.sybers.debout.ui.machine

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import fr.sybers.debout.R
import fr.sybers.debout.domain.QuickAccessSlot

@get:ColorRes
val QuickAccessSlot.colorRes: Int
    get() = when (this) {
        QuickAccessSlot.First -> R.color.quick_access_first
        QuickAccessSlot.Second -> R.color.quick_access_second
        QuickAccessSlot.Third -> R.color.quick_access_third
    }

/** The badge shape alone: the system tints tile icons. */
@get:DrawableRes
val QuickAccessSlot.tileIcon: Int
    get() = when (this) {
        QuickAccessSlot.First -> R.drawable.ic_slot_first
        QuickAccessSlot.Second -> R.drawable.ic_slot_second
        QuickAccessSlot.Third -> R.drawable.ic_slot_third
    }

@get:DrawableRes
val QuickAccessSlot.shortcutIcon: Int
    get() = when (this) {
        QuickAccessSlot.First -> R.drawable.ic_shortcut_first
        QuickAccessSlot.Second -> R.drawable.ic_shortcut_second
        QuickAccessSlot.Third -> R.drawable.ic_shortcut_third
    }
