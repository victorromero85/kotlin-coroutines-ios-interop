package org.example.kotlin_coroutines_ios_interop

/** Deepest nesting level a bar can be launched at; top-level bars are depth `0`. */
const val MAX_DEPTH = 2

/** Lifecycle of the coroutine behind a bar, mirroring the states of its `Job`. */
enum class BarStatus {
    /** The coroutine's own body is still advancing the bar. */
    Running,

    /** The body has finished, but the job cannot complete until its children do. */
    WaitingForChildren,

    /** The job and every child it launched have finished normally. */
    Completed,

    /** The job was cancelled, either directly or because an ancestor was. */
    Cancelled,
}

/**
 * Snapshot of a single progress bar, as observed by the UI.
 *
 * Bars form a tree but are exposed as a flat list in depth-first order, so each bar is followed by
 * its descendants and [depth] is enough to indent it.
 *
 * @property id stable identifier for the bar, unique across runs
 * @property depth nesting level, from `0` for top-level bars up to [MAX_DEPTH]
 * @property progress fraction complete, from `0f` to `1f`
 * @property status lifecycle of the coroutine behind this bar
 */
data class BarState(
    val id: Int,
    val depth: Int,
    val progress: Float,
    val status: BarStatus,
) {
    /** Whether the coroutine behind this bar is still active, including while waiting for children. */
    val running: Boolean
        get() = status == BarStatus.Running || status == BarStatus.WaitingForChildren

    /** Children can only be launched while the bar's own body is running and below [MAX_DEPTH]. */
    val canAddChild: Boolean
        get() = status == BarStatus.Running && depth < MAX_DEPTH
}
