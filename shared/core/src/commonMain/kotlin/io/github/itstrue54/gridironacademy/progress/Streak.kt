package io.github.itstrue54.gridironacademy.progress

/**
 * Streak rules: consecutive days count, with one free rest day per Monday-based week.
 * Pure functions of [Progress] and an epoch day passed in by the caller (no clocks in core).
 */
object Streak {
    /** Monday-based week index. Epoch day 0 was a Thursday, so Monday 1970-01-05 (day 4) starts week 1. */
    fun weekOf(day: Long): Long {
        val n = day + 3
        var q = n / 7
        if (n % 7 < 0) q -= 1 // floor, not truncate
        return q
    }

    /** What [Progress.touch] does for [today]. */
    fun touch(p: Progress, today: Long): Progress {
        val last = p.lastActiveDay
        return when {
            today <= last -> p
            today == last + 1 -> p.copy(streakDays = p.streakDays + 1, lastActiveDay = today)
            today == last + 2 && p.streakDays > 0 && p.restWeek != weekOf(last + 1) ->
                p.copy(streakDays = p.streakDays + 1, lastActiveDay = today, restWeek = weekOf(last + 1))
            else -> p.copy(streakDays = 1, lastActiveDay = today)
        }
    }

    /** The streak to show on [today]: zero once it has lapsed. */
    fun current(p: Progress, today: Long): Int {
        val gap = today - p.lastActiveDay
        return when {
            gap <= 1 -> p.streakDays
            gap == 2L && p.restWeek != weekOf(p.lastActiveDay + 1) -> p.streakDays
            else -> 0
        }
    }

    /** True when this week's rest day has already been spent. */
    fun restUsedThisWeek(p: Progress, today: Long): Boolean = p.restWeek == weekOf(today)

    /**
     * True when the streak is alive only because the rest day covers yesterday: the last active
     * day was two days ago and that week's rest is still free. Practising today spends it.
     */
    fun restCoversYesterday(p: Progress, today: Long): Boolean =
        today - p.lastActiveDay == 2L && current(p, today) > 0
}
