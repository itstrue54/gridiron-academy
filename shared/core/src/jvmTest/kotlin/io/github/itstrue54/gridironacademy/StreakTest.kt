package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.progress.Progress
import io.github.itstrue54.gridironacademy.progress.Streak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakTest {
    // Epoch day 11 is Monday 1970-01-12; days 11..17 are one week (index 2).
    private val mon = 11L

    private fun run(vararg days: Long): Progress = days.fold(Progress()) { p, d -> p.touch(d) }

    @Test fun sameDayNoChange() {
        val p = run(mon, mon)
        assertEquals(1, p.streakDays)
        assertEquals(mon, p.lastActiveDay)
    }

    @Test fun nextDayAddsOne() {
        assertEquals(2, run(mon, mon + 1).streakDays)
    }

    @Test fun oneMissedDayUsesRest() {
        val p = run(mon, mon + 1, mon + 3) // missed Wednesday
        assertEquals(3, p.streakDays)
        assertEquals(Streak.weekOf(mon + 2), p.restWeek)
        assertEquals(mon + 3, p.lastActiveDay)
    }

    @Test fun secondMissedDayInSameWeekBreaks() {
        val p = run(mon, mon + 1, mon + 3, mon + 5) // missed Wed (rest) then Fri
        assertEquals(1, p.streakDays)
    }

    @Test fun restRefreshesNextWeek() {
        val used = Progress(streakDays = 5, lastActiveDay = mon + 8, restWeek = Streak.weekOf(mon))
        val p = used.touch(mon + 10) // missed day mon+9 is in the following week
        assertEquals(6, p.streakDays)
        assertEquals(Streak.weekOf(mon + 9), p.restWeek)
    }

    @Test fun twoMissedDaysBreak() {
        assertEquals(1, run(mon, mon + 1, mon + 4).streakDays)
    }

    @Test fun clockBackNoChange() {
        val p = run(mon, mon + 1)
        assertEquals(p, p.touch(mon - 5))
    }

    @Test fun currentShowsZeroAfterLapse() {
        val p = run(mon, mon + 1)
        assertEquals(2, Streak.current(p, mon + 1))
        assertEquals(0, Streak.current(p, mon + 4))
        // rest already spent for that week: the day after next is a lapse
        val spent = Progress(streakDays = 4, lastActiveDay = mon + 1, restWeek = Streak.weekOf(mon + 2))
        assertEquals(0, Streak.current(spent, mon + 3))
        assertTrue(Streak.restUsedThisWeek(spent, mon + 3))
    }

    @Test fun currentKeepsStreakDuringRestDay() {
        val p = run(mon, mon + 1)
        assertEquals(2, Streak.current(p, mon + 3))
        assertFalse(Streak.restUsedThisWeek(p, mon + 3))
        assertEquals(0, Streak.current(Progress(), 50))
    }

    @Test fun weekOfMondayBoundary() {
        assertEquals(0, Streak.weekOf(3)) // Sunday 1970-01-04
        assertEquals(1, Streak.weekOf(4)) // Monday 1970-01-05
        assertEquals(1, Streak.weekOf(10))
        assertEquals(2, Streak.weekOf(11))
        assertEquals(-1, Streak.weekOf(-4)) // floors, not truncates
    }

    @Test fun restCoversYesterdayOnlyWhileTheRestIsWhatKeepsTheStreak() {
        val p = run(mon, mon + 1)
        assertTrue(Streak.restCoversYesterday(p, mon + 3))
        assertFalse(Streak.restCoversYesterday(p, mon + 2), "yesterday was not missed")
        assertFalse(Streak.restCoversYesterday(p, mon + 4), "lapsed")
        // a rest spent on a missed Sunday belongs to the previous week; today is in the next
        val sundayMissed = Progress(streakDays = 4, lastActiveDay = mon + 5, restWeek = Streak.weekOf(mon))
        assertFalse(Streak.restCoversYesterday(sundayMissed, mon + 7), "that week's rest is spent, streak lapsed")
        val fresh = Progress(streakDays = 4, lastActiveDay = mon + 5, restWeek = Streak.weekOf(mon - 7))
        assertTrue(Streak.restCoversYesterday(fresh, mon + 7), "missed Sunday, rest free")
    }

    @Test fun missedSundayThenMondayReturn() {
        val sat = mon + 5
        val monday = mon + 7
        val sunWeek = Streak.weekOf(mon + 6)
        // rest free: Monday keeps the streak and the rest is recorded against Sunday's week
        val free = Progress(streakDays = 3, lastActiveDay = sat, restWeek = sunWeek - 1)
        assertEquals(3, Streak.current(free, monday))
        val back = free.touch(monday)
        assertEquals(4, back.streakDays)
        assertEquals(sunWeek, back.restWeek)
        assertEquals(back.streakDays, Streak.current(back, monday))
        // rest already spent that week: the streak is gone and Monday starts a new one
        val spent = Progress(streakDays = 3, lastActiveDay = sat, restWeek = sunWeek)
        assertEquals(0, Streak.current(spent, monday))
        assertEquals(1, spent.touch(monday).streakDays)
    }

    @Test fun replayMatchesTouchingDayByDay() {
        val days = longArrayOf(mon, mon + 1, mon + 3, mon + 4, mon + 7)
        assertEquals(run(*days), Streak.replay(days.toList()))
        // order and repeats don't matter: days from several devices arrive in any order
        assertEquals(run(*days), Streak.replay(listOf(mon + 4, mon, mon + 7, mon + 1, mon + 3, mon, mon + 4)))
        assertEquals(Progress(), Streak.replay(emptyList()))
    }
}
