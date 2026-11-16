package fr.sybers.debout.domain

/** The first machines of the list are the ones the tiles and app shortcuts control. */
enum class QuickAccessSlot {
    First,
    Second,
    Third,
    ;

    val number: Int get() = ordinal + 1

    companion object {
        fun at(position: Int): QuickAccessSlot? = entries.getOrNull(position)
    }
}
