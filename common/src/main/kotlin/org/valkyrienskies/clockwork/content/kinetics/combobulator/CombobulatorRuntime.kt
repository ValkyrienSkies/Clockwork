package org.valkyrienskies.clockwork.content.kinetics.combobulator

enum class CombobulatorFault { TIMEOUT, UNAVAILABLE, ARITHMETIC, STOPPED }

internal class CombobulatorRuntime {
    private val held = DoubleArray(CombobulatorProgram.MAX_CHANNELS)
    private val pulses = LongArray(CombobulatorProgram.MAX_CHANNELS)
    private val registers = DoubleArray(CombobulatorProgram.REGISTER_COUNT)
    private var initialized = false
    private var previousStart = false
    private var entered = 0L
    var activeStep = -1
        private set
    var fault: CombobulatorFault? = null
        private set

    fun output(id: Int, now: Long): Double = if (pulses[id] > now) 15.0 else held[id]

    fun tick(program: CombobulatorProgram, inputs: DoubleArray, now: Long) {
        val high = (inputs.getOrNull(program.start) ?: 0.0) > 0
        val rising = initialized && high && !previousStart
        initialized = true
        previousStart = high

        val step = program.steps.getOrNull(activeStep)
        if (step == null) {
            if (rising && program.steps.isNotEmpty()) {
                fault = null
                registers.fill(0.0)
                enter(program, 0, now)
            }
            return
        }
        if (program.repeat && !high) {
            stop(program)
            return
        }
        val elapsed = now - entered
        if (step.node != CombobulatorNode.STEP) {
            execute(program, step, inputs, now)
            return
        }
        if (step.waitForInput) {
            val bound = program.channels.any { it.id == step.input && it.input && it.bound }
            val matched = bound && inputs[step.input] in step.minimum.toDouble()..step.maximum.toDouble()
            if (matched) advance(program, now, step.next)
            else if (elapsed >= step.timeout) {
                advance(program, now, step.otherwise, CombobulatorFault.TIMEOUT)
            }
        } else if (elapsed >= step.delay) {
            advance(program, now, step.next)
        }
    }

    private fun execute(program: CombobulatorProgram, step: CombobulatorStep, inputs: DoubleArray, now: Long) {
        val a = operand(step.a, inputs)
        val b = operand(step.b, inputs)
        if (!a.isFinite() || !b.isFinite()) {
            fault = CombobulatorFault.UNAVAILABLE
            stop(program)
            return
        }
        if (step.node == CombobulatorNode.COMPARE) {
            advance(program, now, if (step.comparison.test(a, b)) step.next else step.otherwise)
            return
        }
        val result = when (step.node) {
            CombobulatorNode.ADD -> a + b
            CombobulatorNode.SUBTRACT -> a - b
            CombobulatorNode.MULTIPLY -> a * b
            CombobulatorNode.DIVIDE -> a / b
            else -> error("Expected math node")
        }
        if (!result.isFinite()) {
            fault = CombobulatorFault.ARITHMETIC
            stop(program)
            return
        }
        val target = step.destination
        if (target in CombobulatorProgram.registers) {
            registers[target - CombobulatorProgram.MAX_CHANNELS] = result
        } else if (target in held.indices) {
            held[target] = result
            pulses[target] = 0
        }
        advance(program, now, step.next)
    }

    private fun operand(operand: CombobulatorOperand, inputs: DoubleArray): Double = when (operand.source) {
        -1 -> operand.constant
        in CombobulatorProgram.registers -> registers[operand.source - CombobulatorProgram.MAX_CHANNELS]
        else -> inputs.getOrElse(operand.source) { Double.NaN }
    }

    private fun advance(
        program: CombobulatorProgram,
        now: Long,
        target: Int,
        failure: CombobulatorFault = CombobulatorFault.STOPPED,
    ) {
        val index = if (target == CombobulatorProgram.NEXT) activeStep + 1 else target
        when {
            index in program.steps.indices -> enter(program, index, now)
            target == CombobulatorProgram.STOP -> {
                fault = failure
                stop(program)
            }
            program.repeat -> enter(program, 0, now)
            else -> activeStep = -1
        }
    }

    private fun enter(program: CombobulatorProgram, index: Int, now: Long) {
        activeStep = index
        entered = now
        val step = program.steps[index]
        if (step.node != CombobulatorNode.STEP) return
        step.actions.forEach { (id, action) ->
            if (action.pulseTicks > 0) {
                pulses[id] = maxOf(pulses[id], now + action.pulseTicks)
            } else {
                held[id] = action.level.toDouble()
                pulses[id] = 0
            }
        }
    }

    private fun stop(program: CombobulatorProgram) {
        activeStep = -1
        pulses.fill(0)
        program.channels.filterNot { it.input }.forEach { held[it.id] = it.stopLevel.toDouble() }
    }
}
