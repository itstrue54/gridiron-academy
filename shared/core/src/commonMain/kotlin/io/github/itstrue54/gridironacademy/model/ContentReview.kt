package io.github.itstrue54.gridironacademy.model

import kotlinx.serialization.Serializable

/**
 * One step's football sign-off (SPEC 13.3): who checked it, when, against which NFL rule
 * season, and a fingerprint of the step as it was read. Editing the step changes the
 * fingerprint, so the sign-off goes stale and the step needs another look.
 */
@Serializable
data class Signoff(
    val step: String,
    val reviewer: String,
    /** ISO date, YYYY-MM-DD. */
    val date: String,
    val ruleSeason: Int,
    /** First 16 hex digits of SHA-256 over the step's canonical JSON (tools/review.py). */
    val hash: String,
)

/** `review.json`: every sign-off. Written by tools/review.py, never by hand. */
@Serializable
data class ReviewLog(val version: Int = 1, val signoffs: List<Signoff> = emptyList())
