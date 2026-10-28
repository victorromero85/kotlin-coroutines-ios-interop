package org.example.kotlin_coroutines_ios_interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives three progress bars from a single parent coroutine to demonstrate structured concurrency.
 *
 * [start] launches one parent coroutine, which in turn launches a child coroutine per bar. Because
 * the children are launched from inside the parent's body they form a job hierarchy, so cancelling
 * the parent cancels every child. Making that propagation visible is the point of this class.
 *
 * The coroutine scope is owned explicitly rather than inherited from a platform lifecycle (Android's
 * `viewModelScope`, for instance) for two reasons: scope ownership and cancellation stay visible
 * instead of being handled off-screen, and the class stays free of platform dependencies so Swift
 * can use it unchanged.
 *
 * Callers own this object's lifetime. Nothing cancels its scope automatically.
 */
class ProgressBarsController {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _bars = MutableStateFlow(List(3) { BarState(it, 0f, false) })

    /**
     * State of all three bars, re-emitted on every tick.
     *
     * Android collects this with `collectAsStateWithLifecycle()`. On iOS, SKIE exposes it as an
     * `AsyncSequence` that SwiftUI consumes with `for await`.
     */
    val bars: StateFlow<List<BarState>> = _bars.asStateFlow()

    private var parentJob: Job? = null

    /**
     * Starts all three bars under a single parent job, resetting their progress.
     *
     * Each bar advances at its own randomly chosen pace, so the children are visibly independent of
     * one another. Does nothing if a run is already in progress.
     */
    fun start() {
        if (parentJob?.isActive == true) return            // already running

        parentJob = scope.launch {
            _bars.value.map { it.id }.forEach { id ->
                launch {
                    update(id) { it.copy(progress = 0f, running = true) }
                    val totalMs = Random.nextLong(minDurationMs, maxDurationMs + 1)
                    val step = tickMs.toFloat() / totalMs
                    try {
                        var p = 0f
                        while (p < 1f) {
                            delay(tickMs)
                            p = (p + step).coerceAtMost(1f)
                            update(id) { it.copy(progress = p) }
                        }
                    } finally {
                        update(id) { it.copy(running = false) }
                    }
                }
            }
        }
    }

    /**
     * Cancels the parent job, which cancels all three children with it.
     *
     * Only the parent is touched here. The children stop because they are part of its job hierarchy,
     * not because anything iterates over them. Each bar keeps the progress it had reached, so the
     * three stop at different points and make it clear they were cancelled together.
     */
    fun cancel() {
        parentJob?.cancel()
    }

    private fun update(id: Int, transform: (BarState) -> BarState) {
        _bars.value = _bars.value.map { if (it.id == id) transform(it) else it }
    }

    companion object {
        private const val maxDurationMs = 20_000L
        private const val minDurationMs = 2_000L   // floor so fastest bar still visible
        private const val tickMs = 50L
    }

}
