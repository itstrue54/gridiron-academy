package io.github.itstrue54.gridironacademy.engine

/**
 * Seeded generator (xorshift64*). All shuffling and sampling in the engine goes
 * through this so a level test or game can be replayed from its seed.
 */
class Rng(seed: Long) {
    private var s: Long = if (seed == 0L) 0x9E3779B97F4A7C15uL.toLong() else seed

    fun nextLong(): Long {
        s = s xor (s ushr 12)
        s = s xor (s shl 25)
        s = s xor (s ushr 27)
        return s * 2685821657736338717L
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun <T> shuffled(items: List<T>): List<T> {
        val a = items.toMutableList()
        for (i in a.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
        return a
    }

    fun <T> sample(items: List<T>, n: Int): List<T> = shuffled(items).take(n)
}
