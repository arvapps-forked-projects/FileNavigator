package com.w2sv.navigator.observing

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class RecentSetTest {

    @Test
    fun `add elements respects max size and evicts oldest`() {
        val recent = RecentSet<Int>(3).apply { addAll(listOf(1, 2, 3)) }
        assertEquals(listOf(1, 2, 3).toString(), recent.toString())

        recent.add(4)
        assertEquals(listOf(2, 3, 4).toString(), recent.toString())
    }

    @Test
    fun `replaceIf replaces first matching element`() {
        val recent = RecentSet<Int>(3).apply { addAll(listOf(1, 2, 3)) }

        val replaced = recent.replaceIf({ it == 2 }, 5)
        assertTrue(replaced)
        assertEquals(listOf(1, 3, 5).toString(), recent.toString())

        val notReplaced = recent.replaceIf({ it == 10 }, 7)
        assertFalse(notReplaced) // Because 10 wasn't found, it just adds 7
        assertEquals(listOf(3, 5, 7).toString(), recent.toString())
    }

    @Test
    fun `contains, removeIf and clear behave as expected`() {
        val recent = RecentSet<Int>(3).apply { addAll(listOf(1, 2)) }

        assertTrue(1 in recent)
        assertFalse(3 in recent)

        val removed = recent.removeIf { it % 2 == 0 }
        assertTrue(removed)
        assertEquals(listOf(1).toString(), recent.toString())

        recent.clear()
        assertEquals(0, recent.size)
        assertFalse(recent.isFull)
    }

    @Test
    fun `isFull reflects capacity`() {
        val recent = RecentSet<Int>(2)

        recent.add(1)
        assertFalse(recent.isFull)

        recent.add(2)
        assertTrue(recent.isFull)
    }
}
