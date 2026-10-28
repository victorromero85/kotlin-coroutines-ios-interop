package org.example.kotlin_coroutines_ios_interop

/**
 * Snapshot of a single progress bar, as observed by the UI.
 *
 * @property id stable identifier for the bar, matching its position in the list
 * @property progress fraction complete, from `0f` to `1f`
 * @property running whether a child coroutine is currently advancing this bar
 */
data class BarState(
    val id: Int,
    val progress: Float,
    val running: Boolean
)