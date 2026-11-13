package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CombobulatorOperationTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test fun floorAndCeilRoundInBothDirectionsAndOnlyRequireA() {
        val runtime = CombobulatorOperationRuntime()
        for ((function, expected) in listOf(
            CombobulatorFunction.FLOOR to listOf(2.0, -3.0, 4.0),
            CombobulatorFunction.CEIL to listOf(3.0, -2.0, 4.0))) {
            for ((index, input) in listOf(2.75, -2.75, 4.0).withIndex()) {
                runtime.tick(CombobulatorOperation(function), doubleArrayOf(input, Double.NaN))
                assertEquals(expected[index], runtime.output)
                assertNull(runtime.fault)
            }
            runtime.tick(CombobulatorOperation(function), doubleArrayOf(Double.NaN))
            assertEquals(CombobulatorFault.UNAVAILABLE, runtime.fault)
        }
    }

    @Test fun moduloWrapsSignedFractionsAndRecoversFromZeroDivisor() {
        val runtime = CombobulatorOperationRuntime()
        val operation = CombobulatorOperation(CombobulatorFunction.MODULO)
        for ((a, b, expected) in listOf(
            Triple(5.75, 2.5, 0.75), Triple(-5.75, 2.5, 1.75),
            Triple(5.75, -2.5, -1.75), Triple(-5.75, -2.5, -0.75),
            Triple(Double.MAX_VALUE, Double.MIN_VALUE, 0.0))) {
            runtime.tick(operation, doubleArrayOf(a, b))
            assertEquals(expected, runtime.output)
            assertNull(runtime.fault)
        }
        for (zero in listOf(0.0, -0.0)) {
            runtime.tick(operation, doubleArrayOf(5.0, zero))
            assertEquals(CombobulatorFault.ARITHMETIC, runtime.fault)
        }
        runtime.tick(operation, doubleArrayOf(-1.0, 4.0))
        assertEquals(3.0, runtime.output)
        assertNull(runtime.fault)
    }

    @Test fun newArithmeticModesPersistAndExposeTheirRequiredInputs() {
        for (function in listOf(CombobulatorFunction.FLOOR, CombobulatorFunction.CEIL, CombobulatorFunction.MODULO)) {
            val program = CombobulatorProgram.controller(CombobulatorOperation(function))
            val restored = CombobulatorProgram.read(program.write())
            assertEquals(function, restored.operation!!.function)
            assertEquals(if (function == CombobulatorFunction.MODULO) 2 else 1, restored.channels.count { it.input })
            assertEquals(program.write(), restored.write())
        }
    }

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
