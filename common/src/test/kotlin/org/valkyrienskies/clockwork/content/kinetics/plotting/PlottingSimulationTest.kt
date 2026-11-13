package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFunction
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperation
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperationRuntime

class PlottingSimulationTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    private fun function(id: Int, function: CombobulatorFunction, vararg inputs: PlottingInput) =
        PlottingNode(id, id, 0, operation = CombobulatorOperation(function), inputs = List(3) { inputs.getOrElse(it) { PlottingInput() } })

    @Test fun circuitUsesPreviousTickOutputsRegardlessOfNodeOrder() {
        val nodes = listOf(PlottingNode(1, 0, 0, PlottingKind.CONSTANT, value = 2.5),
            function(2, CombobulatorFunction.MULTIPLY, PlottingInput(1), PlottingInput(constant = 2.0)),
            function(3, CombobulatorFunction.ADD, PlottingInput(2), PlottingInput(constant = 1.0)))
        val a = PlottingSimulation().apply { reset(PlottingPlan(nodes = nodes)) }
        val b = PlottingSimulation().apply { reset(PlottingPlan(nodes = nodes.reversed())) }
        a.tick(PlottingPlan(nodes = nodes)); b.tick(PlottingPlan(nodes = nodes.reversed()))
        assertEquals(a.values, b.values)
        assertEquals(5.0, a.values[2])
        assertEquals(1.0, a.values[3])
        a.tick(PlottingPlan(nodes = nodes)); b.tick(PlottingPlan(nodes = nodes.reversed()))
        assertEquals(a.values, b.values)
        assertEquals(6.0, a.values[3])
    }

    @Test fun feedbackCircuitAdvancesWithoutRecursiveEvaluation() {
        val plan = PlottingPlan(nodes = listOf(function(1, CombobulatorFunction.ADD, PlottingInput(1), PlottingInput(constant = 1.0))))
        val simulation = PlottingSimulation().apply { reset(plan) }
        repeat(20) { simulation.tick(plan) }
        assertEquals(20.0, simulation.values[1])
        simulation.reset(plan)
        assertEquals(0.0, simulation.values[1])
        assertEquals(0L, simulation.ticks)
    }

    @Test fun registerStoresPreciseInputUntilAnotherWriteAndResetsWithSimulation() {
        val data = PlottingNode(1, 0, 0, PlottingKind.INPUT, value = 255.125)
        val enable = PlottingNode(2, 0, 1, PlottingKind.INPUT, value = 1.0)
        val register = function(3, CombobulatorFunction.REGISTER, PlottingInput(1), PlottingInput(2))
        val plan = PlottingPlan(nodes = listOf(data, enable, register))
        val simulation = PlottingSimulation().apply { reset(plan) }
        simulation.tick(plan)
        assertEquals(255.125, simulation.values[3])
        simulation.setInput(2, 0.0)
        simulation.setInput(1, -42.75)
        repeat(4) { simulation.tick(plan) }
        assertEquals(255.125, simulation.values[3])
        simulation.setInput(2, 1.0)
        simulation.tick(plan)
        assertEquals(-42.75, simulation.values[3])
        simulation.reset(plan)
        assertEquals(0.0, simulation.values[3])
    }

    @Test fun exampleRejectsMissingHitsAndDistinguishesGroundAndClearance() {
        val plan = PlottingPlan.example()
        val simulation = PlottingSimulation().apply { reset(plan) }
        repeat(4) { simulation.tick(plan) }
        assertEquals(0.0, simulation.values[5])
        simulation.setInput(1, 0.25)
        repeat(4) { simulation.tick(plan) }
        assertEquals(1.0, simulation.values[5])
        simulation.setInput(1, 2.0)
        repeat(4) { simulation.tick(plan) }
        assertEquals(0.0, simulation.values[5])
        simulation.reset(plan)
        assertEquals(-1.0, simulation.inputValue(plan.nodes.first()))
        assertEquals(-1.0, plan.nodes.first().value)
    }

    @Test fun everyFunctionMatchesTheWorldEvaluatorAcrossInputChanges() {
        for (function in CombobulatorFunction.entries) {
            var plan = PlottingPlan(nodes = listOf(function(1, function)))
            val simulation = PlottingSimulation().apply { reset(plan) }
            val world = CombobulatorOperationRuntime()
            repeat(60) { tick ->
                val values = doubleArrayOf(if (tick % 7 < 3) 2.5 else 0.0, if (tick % 13 == 0) 0.0 else 1.5, if (tick % 2 == 0) 1.0 else 0.0)
                plan = plan.copy(nodes = listOf(plan.nodes.first().copy(inputs = values.map { PlottingInput(constant = it) })))
                world.tick(plan.nodes.first().operation, values)
                simulation.tick(plan)
                assertEquals(world.output, simulation.values[1], "$function at tick $tick")
            }
        }
    }

    @Test fun resetClearsMemoryAndDelayAndRestoresInitialInputs() {
        val input = PlottingNode(1, 0, 0, PlottingKind.INPUT, value = 0.0)
        val plan = PlottingPlan(nodes = listOf(input,
            function(2, CombobulatorFunction.LATCH, PlottingInput(1)),
            function(3, CombobulatorFunction.COUNTER, PlottingInput(1)),
            function(4, CombobulatorFunction.DELAY, PlottingInput(1))))
        val simulation = PlottingSimulation().apply { reset(plan); setInput(1, 1.0) }
        repeat(25) { simulation.tick(plan) }
        assertEquals(15.0, simulation.values[2])
        assertEquals(1.0, simulation.values[3])
        assertEquals(1.0, simulation.values[4])
        simulation.reset(plan)
        repeat(25) { simulation.tick(plan) }
        assertEquals(0.0, simulation.values[2])
        assertEquals(0.0, simulation.values[3])
        assertEquals(0.0, simulation.values[4])
    }

    @Test fun redstoneClampsRoundsAndConvertsUnavailableToZero() {
        for ((value, expected) in listOf(-2.0 to 0.0, 4.6 to 5.0, 20.0 to 15.0)) {
            val plan = PlottingPlan(nodes = listOf(PlottingNode(1, 0, 0, PlottingKind.CONSTANT, value = value),
                function(2, CombobulatorFunction.ADD, PlottingInput(1, connection = PlottingConnection.LINK))))
            val simulation = PlottingSimulation().apply { reset(plan); tick(plan) }
            assertEquals(expected, simulation.values[2])
        }
        val plan = PlottingPlan(nodes = listOf(
            function(1, CombobulatorFunction.ADD, PlottingInput(99)),
            function(2, CombobulatorFunction.ADD, PlottingInput(99, connection = PlottingConnection.LINK))))
        val simulation = PlottingSimulation().apply { reset(plan); tick(plan) }
        assertTrue(simulation.values[1]!!.isNaN())
        assertEquals(0.0, simulation.values[2])
    }

    @Test fun planRoundTripPreservesLayoutSettingsAndBindingsButNotRuntime() {
        val plan = PlottingPlan.example().copy(name = "Left leg")
        assertEquals(plan, PlottingPlan.read(plan.write()))
        val simulation = PlottingSimulation().apply { reset(plan); setInput(1, 0.3); tick(plan) }
        assertEquals(plan, PlottingPlan.read(plan.write()))
        assertEquals(1L, simulation.ticks)
    }

    @Test fun constantsRemainPreciseWithoutAConnection() {
        val plan = PlottingPlan(nodes = listOf(function(1, CombobulatorFunction.ADD,
            PlottingInput(constant = -25.75, connection = PlottingConnection.LINK))))
        val simulation = PlottingSimulation().apply { reset(plan); tick(plan) }
        assertEquals(-25.75, simulation.values[1])
    }

    @Test fun adjacencyRequiresTouchingMatchingFaces() {
        val source = PlottingNode(1, 0, 0, PlottingKind.CONSTANT, value = 7.5)
        val sink = function(2, CombobulatorFunction.ADD, PlottingInput(1, connection = PlottingConnection.ADJACENT)).copy(x = 1)
        val plan = PlottingPlan(nodes = listOf(source, sink))
        val simulation = PlottingSimulation().apply { reset(plan); tick(plan) }
        assertEquals(7.5, simulation.values[2])
        simulation.tick(plan.copy(nodes = listOf(source, sink.copy(x = 2))))
        assertTrue(simulation.values[2]!!.isNaN())
        simulation.tick(plan.copy(nodes = listOf(source, sink.copy(rotation = 1))))
        assertTrue(simulation.values[2]!!.isNaN())
        simulation.tick(plan)
        assertEquals(7.5, simulation.values[2])
    }

    @Test fun loadingBoundsNodesCoordinatesAndInvalidNumbers() {
        val entries = ListTag().apply {
            repeat(140) { add(PlottingPlan(nodes = listOf(PlottingNode(it + 1, it - 64, 999, value = Double.NaN)))
                .write().getList("Nodes", 10).getCompound(0)) }
        }
        val plan = PlottingPlan.read(CompoundTag().apply { put("Nodes", entries); putString("Name", "x".repeat(100)) })
        assertEquals(128, plan.nodes.size)
        assertTrue(plan.nodes.all { it.y == 64 && it.value == 0.0 })
        assertEquals(40, plan.name.length)
    }

    @Test fun loadingRemovesDuplicateIdsAndOccupiedCells() {
        val plan = PlottingPlan(nodes = listOf(PlottingNode(1, 0, 0), PlottingNode(1, 1, 0), PlottingNode(2, 0, 0)))
        assertEquals(1, PlottingPlan.read(plan.write()).nodes.size)
    }

}
