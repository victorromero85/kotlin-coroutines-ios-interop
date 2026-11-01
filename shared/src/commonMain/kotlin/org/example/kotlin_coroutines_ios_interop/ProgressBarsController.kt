package org.example.kotlin_coroutines_ios_interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives a tree of progress bars, one coroutine per bar, to demonstrate structured concurrency.
 *
 * [start] launches three top-level bars under a single parent job. [addChild] launches a new bar as
 * a child of an existing bar's job, up to [MAX_DEPTH]. Because every bar's job is a child of the
 * bar it was added to, cancelling any bar cancels its whole subtree while its parent and siblings
 * keep running, and a bar whose own progress reaches 100% does not complete until its children do.
 * Making both behaviours visible is the point of this class.
 *
 * The coroutine scope is owned explicitly rather than inherited from a platform lifecycle (Android's
 * `viewModelScope`, for instance) for two reasons: scope ownership and cancellation stay visible
 * instead of being handled off-screen, and the class stays free of platform dependencies so Swift
 * can use it unchanged.
 *
 * Callers own this object's lifetime. Nothing cancels its scope automatically. [start], [cancel],
 * [addChild] and [cancelBar] are not thread-safe and must all be called from the same thread,
 * normally the UI thread.
 */
class ProgressBarsController {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _bars = MutableStateFlow<List<BarState>>(emptyList())

    /**
     * State of every bar in depth-first order, re-emitted on every tick.
     *
     * Android collects this with `collectAsStateWithLifecycle()`. On iOS, SKIE exposes it as an
     * `AsyncSequence` that SwiftUI consumes with `for await`.
     */
    val bars: StateFlow<List<BarState>> = _bars.asStateFlow()

    private var parentJob: Job? = null
    private val jobs = mutableMapOf<Int, Job>()
    private var nextId = 0

    /**
     * Replaces any previous bars with three new top-level bars, all children of one parent job.
     *
     * Each bar advances at its own randomly chosen pace, so they are visibly independent of one
     * another. Does nothing if a run is already in progress.
     */
    fun start() {
        if (parentJob?.isActive == true) return

        _bars.value = emptyList()
        jobs.clear()
        val parent = Job(scope.coroutineContext.job)
        repeat(3) { launchBar(parent, depth = 0) }
        // The parent has no body of its own: complete() lets it finish once its children have.
        parent.complete()
        parentJob = parent
    }

    /**
     * Cancels the parent job, which cancels every bar with it.
     *
     * Only the parent is touched here. The bars stop because they are part of its job hierarchy,
     * not because anything iterates over them.
     */
    fun cancel() {
        parentJob?.cancel()
    }

    /**
     * Launches a new bar as a child of the bar with [parentId], placed after that bar's existing
     * descendants. Does nothing unless the parent's [BarState.canAddChild] is true.
     */
    fun addChild(parentId: Int) {
        val parentBar = _bars.value.find { it.id == parentId } ?: return
        if (!parentBar.canAddChild) return
        val parent = jobs[parentId] ?: return
        launchBar(parent, depth = parentBar.depth + 1, after = parentId)
    }

    /**
     * Cancels the bar with [id] and, through its job, all of its descendants. Its parent and
     * siblings are unaffected: cancellation travels down the hierarchy, never up.
     */
    fun cancelBar(id: Int) {
        jobs[id]?.cancel()
    }

    /**
     * Adds a bar to the list and launches its coroutine as a child of [parent].
     *
     * Passing [parent] in the launch context makes it the new job's parent, which is what ties the
     * bar into the hierarchy even though it is launched from outside the parent's body.
     *
     * @param after id of the parent bar, so the new bar is inserted after its subtree; `null` to append
     */
    private fun launchBar(parent: Job, depth: Int, after: Int? = null) {
        val id = nextId++
        val bar = BarState(id, depth, progress = 0f, status = BarStatus.Running)
        _bars.update { bars -> bars.insertAfterSubtree(after, bar) }

        val job = scope.launch(parent) { advance(id) }
        job.invokeOnCompletion { cause ->
            val status = if (cause == null) BarStatus.Completed else BarStatus.Cancelled
            updateBar(id) { it.copy(status = status) }
        }
        jobs[id] = job
    }

    /** Fills the bar over a random duration, then flags it as waiting if it still has children. */
    private suspend fun advance(id: Int) {
        val totalMs = Random.nextLong(minDurationMs, maxDurationMs + 1)
        val step = tickMs.toFloat() / totalMs
        var p = 0f
        while (p < 1f) {
            delay(tickMs)
            p = (p + step).coerceAtMost(1f)
            updateBar(id) { it.copy(progress = p) }
        }
        if (currentCoroutineContext().job.children.any { it.isActive }) {
            updateBar(id) { it.copy(status = BarStatus.WaitingForChildren) }
        }
    }

    private fun updateBar(id: Int, transform: (BarState) -> BarState) {
        _bars.update { bars -> bars.map { if (it.id == id) transform(it) else it } }
    }

    private fun List<BarState>.insertAfterSubtree(parentId: Int?, bar: BarState): List<BarState> {
        val parentIndex = indexOfFirst { it.id == parentId }
        if (parentIndex == -1) return this + bar
        val parentDepth = this[parentIndex].depth
        var index = parentIndex + 1
        while (index < size && this[index].depth > parentDepth) index++
        return toMutableList().apply { add(index, bar) }
    }

    companion object {
        private const val maxDurationMs = 45_000L
        private const val minDurationMs = 8_000L   // floor leaves time to add children before a bar finishes
        private const val tickMs = 50L
    }

}
