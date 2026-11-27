package org.valkyrienskies.clockwork.content.kinetics.combobulator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CombobulatorSchedulerTest {
    @Test fun linearChainSchedulesUpstreamFirstRegardlessOfInputOrder() {
        // 1 -> 2 -> 3 -> 4 -> 5
        val deps = mapOf(
            1 to emptyList(),
            2 to listOf(1),
            3 to listOf(2),
            4 to listOf(3),
            5 to listOf(4),
        )
        for (input in listOf(listOf(1, 2, 3, 4, 5), listOf(5, 4, 3, 2, 1), listOf(3, 5, 1, 4, 2))) {
            val batches = CombobulatorScheduler.schedule(input, { deps.getValue(it) }, { false })
            assertEquals(listOf(listOf(1), listOf(2), listOf(3), listOf(4), listOf(5)), batches.map { it.nodes })
            assertTrue(batches.all { it.preTickReads.isEmpty() })
        }
    }

    @Test fun diamondEvaluatesBothBranchesBeforeConvergingNode() {
        // A -> B -> D, A -> C -> D
        val deps = mapOf(
            "A" to emptyList(),
            "B" to listOf("A"),
            "C" to listOf("A"),
            "D" to listOf("B", "C"),
        )
        val batches = CombobulatorScheduler.schedule(listOf("D", "C", "B", "A"), { deps.getValue(it) }, { it == "D" })
        val order = batches.flatMap { it.nodes }
        assertEquals(4, order.size)
        assertEquals("A", order.first())
        assertEquals("D", order.last())
        assertTrue(batches.all { it.preTickReads.isEmpty() })
    }

    @Test fun purelyCombinationalCycleBatchesTogetherWithIntraSccPreTickReads() {
        // IN -> A <-> B -> OUT
        val deps = mapOf(
            "IN" to emptyList(),
            "A" to listOf("IN", "B"),
            "B" to listOf("A"),
            "OUT" to listOf("B"),
        )
        for (input in listOf(listOf("IN", "A", "B", "OUT"), listOf("OUT", "B", "A", "IN"))) {
            val batches = CombobulatorScheduler.schedule(input, { deps.getValue(it) }, { false })
            assertEquals(3, batches.size)
            assertEquals(listOf("IN"), batches[0].nodes)
            assertEquals(emptySet<String>(), batches[0].preTickReads)
            assertEquals(setOf("A", "B"), batches[1].nodes.toSet())
            assertEquals(setOf("A", "B"), batches[1].preTickReads)
            assertEquals(listOf("OUT"), batches[2].nodes)
            assertEquals(emptySet<String>(), batches[2].preTickReads)
        }
    }

    @Test fun selfLoopIsMarkedCyclic() {
        val deps = mapOf("A" to listOf("A"))
        val batches = CombobulatorScheduler.schedule(listOf("A"), { deps.getValue(it) }, { false })
        assertEquals(1, batches.size)
        assertEquals(listOf("A"), batches[0].nodes)
        assertEquals(setOf("A"), batches[0].preTickReads)
    }

    @Test fun cyclicSccWithSequentialBoundaryRunsCombinationalInteriorBeforeBoundary() {
        // IN -> SUB -> ADD -> DELAY -> ADD, and ADD -> OUT
        val deps = mapOf(
            "IN" to emptyList(),
            "SUB" to listOf("IN"),
            "ADD" to listOf("SUB", "DELAY"),
            "DELAY" to listOf("ADD"),
            "OUT" to listOf("ADD"),
        )
        val batches = CombobulatorScheduler.schedule(
            listOf("OUT", "DELAY", "ADD", "SUB", "IN"),
            { deps.getValue(it) },
            { it == "DELAY" },
        )
        assertEquals(listOf(listOf("IN"), listOf("SUB"), listOf("ADD"), listOf("DELAY"), listOf("OUT")), batches.map { it.nodes })
        assertEquals(emptySet<String>(), batches[0].preTickReads)
        assertEquals(emptySet<String>(), batches[1].preTickReads)
        assertEquals(setOf("DELAY"), batches[2].preTickReads)
        assertEquals(setOf("DELAY"), batches[3].preTickReads)
        assertEquals(emptySet<String>(), batches[4].preTickReads)
    }

    @Test fun nestedCombinationalCycleInsideSequentialLoopStaysDeterministic() {
        // A <-> B -> REG -> A
        val deps = mapOf(
            "A" to listOf("B", "REG"),
            "B" to listOf("A"),
            "REG" to listOf("B"),
        )
        val batches = CombobulatorScheduler.schedule(listOf("REG", "B", "A"), { deps.getValue(it) }, { it == "REG" })
        assertEquals(2, batches.size)
        assertEquals(setOf("A", "B"), batches[0].nodes.toSet())
        assertEquals(setOf("REG", "A", "B"), batches[0].preTickReads)
        assertEquals(listOf("REG"), batches[1].nodes)
        assertEquals(setOf("REG"), batches[1].preTickReads)
    }

    @Test fun deepChainDoesNotOverflowStack() {
        val count = 2000
        val nodes = (1..count).toList().reversed()
        val batches = CombobulatorScheduler.schedule(
            nodes,
            { if (it > 1) listOf(it - 1) else emptyList() },
            { false },
        )
        assertEquals((1..count).toList(), batches.flatMap { it.nodes })
    }

    @Test fun sequentialClassificationCoversMemoryAndTimingOperations() {
        val expectedSequential = setOf(
            CombobulatorFunction.DELAY,
            CombobulatorFunction.PULSE,
            CombobulatorFunction.TIMER,
            CombobulatorFunction.LATCH,
            CombobulatorFunction.COUNTER,
            CombobulatorFunction.REGISTER,
        )
        for (function in CombobulatorFunction.entries) {
            assertEquals(function in expectedSequential, function.sequential, "$function")
        }
    }
}
