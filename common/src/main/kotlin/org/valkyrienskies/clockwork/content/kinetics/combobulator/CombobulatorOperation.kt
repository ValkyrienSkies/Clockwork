package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

enum class CombobulatorFunction(val inputs: Int) {
    ADD(2), SUBTRACT(2), MULTIPLY(2), DIVIDE(2), COMPARE(2),
    AND(2), OR(2), NOT(1), XOR(2), NAND(2), NOR(2), XNOR(2),
    DELAY(1), PULSE(1), TIMER(1), LATCH(2), COUNTER(2), SWITCH(3);

    val timed get() = this == DELAY || this == PULSE || this == TIMER
    val booleanOutput get() = this in setOf(COMPARE, AND, OR, NOT, XOR, NAND, NOR, XNOR, PULSE, TIMER, LATCH)
}

data class CombobulatorOperation(
    val function: CombobulatorFunction = CombobulatorFunction.ADD,
    val comparison: CombobulatorComparison = CombobulatorComparison.GREATER_OR_EQUAL,
    val ticks: Int = 20,
    val low: Double = 0.0,
    val high: Double = 15.0,
    val tolerance: Double = 0.000001,
) {
    fun write() = CompoundTag().apply {
        putString("Function", function.name)
        putString("Comparison", comparison.name)
        putInt("Ticks", ticks)
        putDouble("Low", low)
        putDouble("High", high)
        putDouble("Tolerance", tolerance)
    }

    companion object {
        fun read(tag: CompoundTag) = CombobulatorOperation(
            CombobulatorFunction.entries.firstOrNull { it.name == tag.getString("Function") } ?: CombobulatorFunction.ADD,
            CombobulatorComparison.entries.firstOrNull { it.name == tag.getString("Comparison") } ?: CombobulatorComparison.GREATER_OR_EQUAL,
            tag.getInt("Ticks").coerceIn(1, CombobulatorProgram.MAX_TICKS),
            tag.getDouble("Low").takeIf { it.isFinite() } ?: 0.0,
            tag.getDouble("High").takeIf { it.isFinite() } ?: 15.0,
            tag.getDouble("Tolerance").takeIf { it.isFinite() && it >= 0 } ?: 0.000001,
        )
    }
}

internal class CombobulatorOperationRuntime {
    private var previous = false
    private var latch = false
    private var count = 0.0
    private var remaining = 0
    private var phase = 0
    private val delay = ArrayDeque<Double>()
    var output = 0.0
        private set
    var fault: CombobulatorFault? = null
        private set

    fun tick(operation: CombobulatorOperation, inputs: DoubleArray) {
        fault = null
        if ((0 until operation.function.inputs).any { !inputs.getOrElse(it) { Double.NaN }.isFinite() }) {
            fault = CombobulatorFault.UNAVAILABLE
            output = Double.NaN
            previous = false
            remaining = 0
            phase = 0
            delay.clear()
            return
        }
        val a = inputs[0]
        val b = inputs.getOrElse(1) { 0.0 }
        val high = a != 0.0
        val rising = high && !previous
        previous = high
        fun boolean(value: Boolean) = if (value) operation.high else operation.low
        output = when (operation.function) {
            CombobulatorFunction.ADD -> a + b
            CombobulatorFunction.SUBTRACT -> a - b
            CombobulatorFunction.MULTIPLY -> a * b
            CombobulatorFunction.DIVIDE -> a / b
            CombobulatorFunction.COMPARE -> boolean(when (operation.comparison) {
                CombobulatorComparison.EQUAL -> abs(a - b) <= operation.tolerance
                CombobulatorComparison.NOT_EQUAL -> abs(a - b) > operation.tolerance
                else -> operation.comparison.test(a, b)
            })
            CombobulatorFunction.AND -> boolean(high && b != 0.0)
            CombobulatorFunction.OR -> boolean(high || b != 0.0)
            CombobulatorFunction.NOT -> boolean(!high)
            CombobulatorFunction.XOR -> boolean(high != (b != 0.0))
            CombobulatorFunction.NAND -> boolean(!(high && b != 0.0))
            CombobulatorFunction.NOR -> boolean(!(high || b != 0.0))
            CombobulatorFunction.XNOR -> boolean(high == (b != 0.0))
            CombobulatorFunction.LATCH -> {
                if (b != 0.0) latch = false else if (high) latch = true
                boolean(latch)
            }
            CombobulatorFunction.COUNTER -> {
                if (b != 0.0) count = 0.0 else if (rising) count++
                count
            }
            CombobulatorFunction.SWITCH -> if (inputs[2] != 0.0) b else a
            CombobulatorFunction.DELAY -> {
                delay.addLast(a)
                if (delay.size > operation.ticks) delay.removeFirst() else 0.0
            }
            CombobulatorFunction.PULSE -> {
                if (rising) remaining = operation.ticks
                boolean(remaining > 0).also { if (remaining > 0) remaining-- }
            }
            CombobulatorFunction.TIMER -> {
                if (!high) phase = 0 else phase++
                boolean(high && phase >= operation.ticks).also { if (phase >= operation.ticks) phase = 0 }
            }
        }
        if (!output.isFinite()) fault = CombobulatorFault.ARITHMETIC
    }

    fun write() = CompoundTag().apply {
        putBoolean("Previous", previous)
        putDouble("Output", output)
        putBoolean("Latch", latch)
        putDouble("Count", count)
        putInt("Remaining", remaining)
        putInt("Phase", phase)
        putLongArray("Delay", delay.map { it.toRawBits() })
    }

    fun read(tag: CompoundTag) {
        output = tag.getDouble("Output").takeIf { it.isFinite() } ?: Double.NaN
        previous = tag.getBoolean("Previous")
        latch = tag.getBoolean("Latch")
        count = tag.getDouble("Count").takeIf { it.isFinite() } ?: 0.0
        remaining = tag.getInt("Remaining").coerceIn(0, CombobulatorProgram.MAX_TICKS)
        phase = tag.getInt("Phase").coerceIn(0, CombobulatorProgram.MAX_TICKS)
        delay.clear()
        tag.getLongArray("Delay").take(CombobulatorProgram.MAX_TICKS).map(Double::fromBits)
            .filter { it.isFinite() }.forEach(delay::addLast)
    }
}
