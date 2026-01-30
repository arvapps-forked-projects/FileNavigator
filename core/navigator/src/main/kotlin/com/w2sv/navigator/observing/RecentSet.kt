package com.w2sv.navigator.observing

/**
 * A bounded FIFO container that keeps only the most recently added elements.
 *
 * When the capacity is exceeded, the oldest element is automatically evicted.
 * Useful for small “recently seen” buffers such as deduplication or suppression windows.
 *
 * Invariant: the number of elements never exceeds [maxSize].
 */
internal class RecentSet<E>(private val maxSize: Int) {
    private val deque = ArrayDeque<E>(maxSize)

    val isFull: Boolean
        get() = deque.size == maxSize

    val size: Int
        get() = deque.size

    fun add(element: E) {
        if (deque.size == maxSize) {
            deque.removeFirst()
        }
        deque.addLast(element)
    }

    fun addAll(elements: Collection<E>) {
        elements.forEach(::add)
    }

    /**
     * Replaces the first element matching [predicate] with [element].
     * Returns true if an element was replaced.
     */
    fun replaceIf(predicate: (E) -> Boolean, element: E): Boolean {
        val removed = removeIf(predicate)
        add(element)
        return removed
    }

    operator fun contains(element: E): Boolean =
        deque.contains(element)

    fun removeIf(predicate: (E) -> Boolean): Boolean =
        deque.removeIf(predicate)

    fun clear() =
        deque.clear()

    override fun toString(): String =
        deque.toString()
}
