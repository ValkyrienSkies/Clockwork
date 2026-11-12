package org.valkyrienskies.clockwork.content.kinetics.combobulator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CombobulatorOperationTest {
    @Test fun signedFractionalArithmeticAndFaultRecovery() {
        val runtime = CombobulatorOperationRuntime()
        runtime.tick(CombobulatorOperation(CombobulatorFunction.MULTIPLY), doubleArrayOf(-12.5, 0.8))
        assertEquals(-10.0, runtime.output)
        runtime.tick(CombobulatorOperation(CombobulatorFunction.DIVIDE), doubleArrayOf(1.0, 0.0))
        assertEquals(CombobulatorFault.ARITHMETIC, runtime.fault)
        runtime.tick(CombobulatorOperation(CombobulatorFunction.DIVIDE), doubleArrayOf(1.0, 4.0))
        assertNull(runtime.fault)
        assertEquals(0.25, runtime.output)
        runtime.tick(CombobulatorOperation(), doubleArrayOf(Double.NaN, 0.0))
        assertEquals(CombobulatorFault.UNAVAILABLE, runtime.fault)
    }

    @Test fun gatesUseNonzeroTruthAndConfiguredLevels() {
        val expected = mapOf(
            CombobulatorFunction.AND to listOf(false, false, false, true),
            CombobulatorFunction.OR to listOf(false, true, true, true),
            CombobulatorFunction.XOR to listOf(false, true, true, false),
            CombobulatorFunction.NAND to listOf(true, true, true, false),
            CombobulatorFunction.NOR to listOf(true, false, false, false),
            CombobulatorFunction.XNOR to listOf(true, false, false, true))
        for ((function, truth) in expected) for (index in 0..3) {
            val runtime = CombobulatorOperationRuntime()
            runtime.tick(CombobulatorOperation(function, low = -0.25, high = 2.5),
                doubleArrayOf(if (index >= 2) -0.001 else 0.0, if (index % 2 == 1) 0.5 else 0.0))
            assertEquals(if (truth[index]) 2.5 else -0.25, runtime.output, "$function/$index")
        }
    }

    @Test fun compareToleranceAndSwitchPreservePrecision() {
        val runtime = CombobulatorOperationRuntime()
        runtime.tick(CombobulatorOperation(CombobulatorFunction.COMPARE, CombobulatorComparison.EQUAL), doubleArrayOf(0.1 + 0.2, 0.3))
        assertEquals(15.0, runtime.output)
        runtime.tick(CombobulatorOperation(CombobulatorFunction.SWITCH), doubleArrayOf(-0.375, 12.125, -1.0))
        assertEquals(12.125, runtime.output)
        runtime.tick(CombobulatorOperation(CombobulatorFunction.SWITCH), doubleArrayOf(-0.375, 12.125, 0.0))
        assertEquals(-0.375, runtime.output)
    }

    @Test fun memorySurvivesSaveAndResetTakesPriority() {
        val operation = CombobulatorOperation(CombobulatorFunction.LATCH)
        val runtime = CombobulatorOperationRuntime()
        runtime.tick(operation, doubleArrayOf(1.0, 0.0))
        val restored = CombobulatorOperationRuntime().also { it.read(runtime.write()) }
        restored.tick(operation, doubleArrayOf(0.0, 0.0))
        assertEquals(15.0, restored.output)
        restored.tick(operation, doubleArrayOf(1.0, 1.0))
        assertEquals(0.0, restored.output)
        val counter = CombobulatorOperation(CombobulatorFunction.COUNTER)
        runtime.tick(counter, doubleArrayOf(0.0, 0.0))
        runtime.tick(counter, doubleArrayOf(-1.0, 0.0))
        val reloaded = CombobulatorOperationRuntime().also { it.read(runtime.write()) }
        reloaded.tick(counter, doubleArrayOf(-1.0, 0.0))
        assertEquals(1.0, reloaded.output)
        reloaded.tick(counter, doubleArrayOf(0.0, 0.0))
        reloaded.tick(counter, doubleArrayOf(1.0, 0.0))
        assertEquals(2.0, reloaded.output)
        reloaded.tick(counter, doubleArrayOf(1.0, 1.0))
        assertEquals(0.0, reloaded.output)
    }

    @Test fun delayPulseAndTimerHaveDefinedTickLengths() {
        val delay = CombobulatorOperation(CombobulatorFunction.DELAY, ticks = 2)
        var runtime = CombobulatorOperationRuntime()
        runtime.tick(delay, doubleArrayOf(-1.25))
        assertEquals(0.0, runtime.output)
        runtime = CombobulatorOperationRuntime().also { it.read(runtime.write()) }
        runtime.tick(delay, doubleArrayOf(2.5))
        assertEquals(0.0, runtime.output)
        runtime.tick(delay, doubleArrayOf(3.5))
        assertEquals(-1.25, runtime.output)
        val pulse = CombobulatorOperation(CombobulatorFunction.PULSE, ticks = 2)
        runtime = CombobulatorOperationRuntime()
        runtime.tick(pulse, doubleArrayOf(1.0)); assertEquals(15.0, runtime.output)
        runtime.tick(pulse, doubleArrayOf(1.0)); assertEquals(15.0, runtime.output)
        runtime.tick(pulse, doubleArrayOf(1.0)); assertEquals(0.0, runtime.output)
        val timer = CombobulatorOperation(CombobulatorFunction.TIMER, ticks = 3)
        runtime = CombobulatorOperationRuntime()
        val outputs = (0..5).map { runtime.tick(timer, doubleArrayOf(1.0)); runtime.output }
        assertEquals(listOf(0.0, 0.0, 15.0, 0.0, 0.0, 15.0), outputs)
        runtime.tick(timer, doubleArrayOf(0.0)); assertEquals(0.0, runtime.output)
    }

    @Test fun operationBindingsRoundTripAndNormalize() {
        val program = CombobulatorProgram.controller(CombobulatorOperation(CombobulatorFunction.MULTIPLY)).let {
            it.copy(channels = it.channels.map { port -> if (port.id == 0) port.copy(constant = -0.375) else port })
        }
        assertEquals(program.write(), CombobulatorProgram.read(program.write()).write())
        val tag = program.write()
        tag.getCompound("Operation").putInt("Ticks", Int.MAX_VALUE)
        tag.getCompound("Operation").putDouble("High", Double.NaN)
        val parsed = CombobulatorProgram.read(tag)
        assertEquals(1200, parsed.operation!!.ticks)
        assertEquals(15.0, parsed.operation!!.high)
    }
}
