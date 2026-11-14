package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

enum class CombobulatorFace {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    fun direction(front: Direction): Direction = when (this) {
        FRONT -> front
        BACK -> front.opposite
        LEFT -> front.counterClockWise
        RIGHT -> front.clockWise
        TOP -> Direction.UP
        BOTTOM -> Direction.DOWN
    }
}

data class CombobulatorChannel(
    val id: Int,
    val name: String,
    val input: Boolean,
    val wireless: Boolean = false,
    val face: CombobulatorFace? = null,
    val first: ItemStack = ItemStack.EMPTY,
    val second: ItemStack = ItemStack.EMPTY,
    val stopLevel: Int = 0,
    val thinkable: Boolean = false,
    val constant: Double? = null,
    val device: Boolean = false,
    val reading: String = "",
) {
    val bound: Boolean
        get() = constant != null || if (wireless) !first.isEmpty || !second.isEmpty else face != null
}

data class CombobulatorAction(val level: Int = 0, val pulseTicks: Int = 0)

enum class CombobulatorNode {
    STEP, COMPARE, ADD, SUBTRACT, MULTIPLY, DIVIDE;

    val math: Boolean get() = this != STEP && this != COMPARE
}

enum class CombobulatorComparison {
    LESS, LESS_OR_EQUAL, EQUAL, NOT_EQUAL, GREATER_OR_EQUAL, GREATER;

    fun test(a: Double, b: Double): Boolean = when (this) {
        LESS -> a < b
        LESS_OR_EQUAL -> a <= b
        EQUAL -> a == b
        NOT_EQUAL -> a != b
        GREATER_OR_EQUAL -> a >= b
        GREATER -> a > b
    }
}

data class CombobulatorOperand(val source: Int = -1, val constant: Double = 0.0)

data class CombobulatorStep(
    val name: String,
    val actions: Map<Int, CombobulatorAction> = emptyMap(),
    val delay: Int = 10,
    val waitForInput: Boolean = false,
    val input: Int = -1,
    val minimum: Int = 1,
    val maximum: Int = 15,
    val timeout: Int = 40,
    val node: CombobulatorNode = CombobulatorNode.STEP,
    val a: CombobulatorOperand = CombobulatorOperand(),
    val b: CombobulatorOperand = CombobulatorOperand(),
    val comparison: CombobulatorComparison = CombobulatorComparison.GREATER_OR_EQUAL,
    val destination: Int = CombobulatorProgram.MAX_CHANNELS,
    val next: Int = CombobulatorProgram.NEXT,
    val otherwise: Int = CombobulatorProgram.STOP,
)

data class CombobulatorProgram(
    val channels: List<CombobulatorChannel> = listOf(
        CombobulatorChannel(0, "Start", true, face = CombobulatorFace.BACK),
        CombobulatorChannel(1, "Done", false, face = CombobulatorFace.FRONT),
    ),
    val steps: List<CombobulatorStep> = emptyList(),
    val start: Int = 0,
    val repeat: Boolean = false,
    val operation: CombobulatorOperation? = null,
) {
    fun write() = CompoundTag().apply {
        putInt("Start", start)
        putBoolean("Repeat", repeat)
        operation?.let { put("Operation", it.write()) }
        put("Channels", ListTag().apply {
            channels.forEach { channel ->
                add(CompoundTag().apply {
                    putInt("Id", channel.id)
                    putString("Name", channel.name)
                    putBoolean("Input", channel.input)
                    putBoolean("Wireless", channel.wireless)
                    channel.face?.let { putString("Face", it.name) }
                    if (!channel.first.isEmpty) put("First", channel.first.save(CompoundTag()))
                    if (!channel.second.isEmpty) put("Second", channel.second.save(CompoundTag()))
                    putInt("StopLevel", channel.stopLevel)
                    putBoolean("Thinkable", channel.thinkable)
                    channel.constant?.let { putDouble("Constant", it) }
                    putBoolean("Device", channel.device)
                    putString("Reading", channel.reading)
                })
            }
        })
        put("Steps", ListTag().apply {
            steps.forEach { step ->
                add(CompoundTag().apply {
                    putString("Name", step.name)
                    putInt("Delay", step.delay)
                    putBoolean("WaitForInput", step.waitForInput)
                    putInt("Input", step.input)
                    putInt("Minimum", step.minimum)
                    putInt("Maximum", step.maximum)
                    putInt("Timeout", step.timeout)
                    putString("Node", step.node.name)
                    putInt("SourceA", step.a.source)
                    putDouble("ConstantA", step.a.constant)
                    putInt("SourceB", step.b.source)
                    putDouble("ConstantB", step.b.constant)
                    putString("Comparison", step.comparison.name)
                    putInt("Destination", step.destination)
                    putInt("Next", step.next)
                    putInt("Otherwise", step.otherwise)
                    put("Actions", ListTag().apply {
                        step.actions.forEach { (id, action) ->
                            add(CompoundTag().apply {
                                putInt("Id", id)
                                putInt("Level", action.level)
                                putInt("Pulse", action.pulseTicks)
                            })
                        }
                    })
                })
            }
        })
    }

    companion object {
        const val MAX_CHANNELS = 12
        const val MAX_STEPS = 16
        const val MAX_TICKS = 1200
        const val MAX_NAME = 24
        const val REGISTER_COUNT = 4
        const val NEXT = -1
        const val FINISH = -2
        const val STOP = -3
        val registers = MAX_CHANNELS until MAX_CHANNELS + REGISTER_COUNT

        fun read(tag: CompoundTag): CombobulatorProgram {
            val channels = mutableListOf<CombobulatorChannel>()
            val usedFaces = mutableSetOf<CombobulatorFace>()
            val channelTags = tag.getList("Channels", Tag.TAG_COMPOUND.toInt())
            for (index in 0 until minOf(channelTags.size, MAX_CHANNELS)) {
                val entry = channelTags.getCompound(index)
                val id = entry.getInt("Id")
                if (id !in 0 until MAX_CHANNELS || channels.any { it.id == id }) continue
                val wireless = entry.getBoolean("Wireless")
                val face = CombobulatorFace.entries.firstOrNull { it.name == entry.getString("Face") }
                channels += CombobulatorChannel(
                    id, entry.getString("Name").trim().take(MAX_NAME).ifBlank { "Channel ${id + 1}" },
                    entry.getBoolean("Input"), wireless,
                    face?.takeIf { wireless || (entry.contains("Constant") && entry.getBoolean("Input")) || usedFaces.add(it) },
                    readFrequency(entry, "First"), readFrequency(entry, "Second"),
                    entry.getInt("StopLevel").coerceIn(0, 15),
                    !wireless && entry.getBoolean("Thinkable"),
                    entry.getDouble("Constant").takeIf { entry.contains("Constant") && entry.getBoolean("Input") && it.isFinite() },
                    !wireless && !entry.getBoolean("Thinkable") && entry.getBoolean("Device"),
                    entry.getString("Reading").take(128),
                )
            }
            val inputs = channels.filter { it.input }.map { it.id }.toSet()
            val outputs = channels.filterNot { it.input }.map { it.id }.toSet()
            val stepTags = tag.getList("Steps", Tag.TAG_COMPOUND.toInt())
            val stepCount = minOf(stepTags.size, MAX_STEPS)
            val steps = (0 until stepCount).map { index ->
                val entry = stepTags.getCompound(index)
                val actionTags = entry.getList("Actions", Tag.TAG_COMPOUND.toInt())
                val actions = buildMap {
                    for (i in 0 until minOf(actionTags.size, MAX_CHANNELS)) {
                        val action = actionTags.getCompound(i)
                        val id = action.getInt("Id")
                        if (id in outputs) put(id, CombobulatorAction(
                            action.getInt("Level").coerceIn(0, 15),
                            action.getInt("Pulse").coerceIn(0, MAX_TICKS),
                        ))
                    }
                }
                val minimum = entry.getInt("Minimum").coerceIn(0, 15)
                CombobulatorStep(
                    entry.getString("Name").trim().take(MAX_NAME).ifBlank { "Step ${index + 1}" },
                    actions, entry.getInt("Delay").coerceIn(1, MAX_TICKS), entry.getBoolean("WaitForInput"),
                    entry.getInt("Input").takeIf { it in inputs } ?: -1,
                    minimum, entry.getInt("Maximum").coerceIn(minimum, 15),
                    entry.getInt("Timeout").coerceIn(1, MAX_TICKS),
                    CombobulatorNode.entries.firstOrNull { it.name == entry.getString("Node") } ?: CombobulatorNode.STEP,
                    readOperand(entry, "A", inputs), readOperand(entry, "B", inputs),
                    CombobulatorComparison.entries.firstOrNull { it.name == entry.getString("Comparison") }
                        ?: CombobulatorComparison.GREATER_OR_EQUAL,
                    entry.getInt("Destination").takeIf { entry.contains("Destination") && (it in outputs || it in registers) }
                        ?: registers.first,
                    readTarget(entry, "Next", stepCount, NEXT),
                    readTarget(entry, "Otherwise", stepCount, STOP),
                )
            }
            val operation = if (tag.contains("Operation")) CombobulatorOperation.read(tag.getCompound("Operation")) else null
            val result = CombobulatorProgram(
                channels, steps, tag.getInt("Start").takeIf { it in inputs } ?: -1, tag.getBoolean("Repeat"),
                operation,
            )
            return if (operation == null) result else result.copy(channels = controllerChannels(operation, channels), steps = emptyList())
        }

        fun controller(operation: CombobulatorOperation = CombobulatorOperation()) =
            CombobulatorProgram(channels = controllerChannels(operation, emptyList()), operation = operation)

        fun controllerChannels(operation: CombobulatorOperation, existing: List<CombobulatorChannel>): List<CombobulatorChannel> {
            val faces = mutableSetOf<CombobulatorFace>()
            return ((0 until operation.function.inputs).toList() + 3).map { id ->
                val port = existing.firstOrNull { it.id == id } ?: if (id == 3)
                    CombobulatorChannel(3, "Output", false, face = CombobulatorFace.FRONT, device = true)
                else CombobulatorChannel(id, "${'A' + id}", true, constant = if (id == 1) 1.0 else 0.0)
                port.copy(input = id != 3, constant = port.constant.takeIf { id != 3 }, face =
                    port.face?.takeIf { port.wireless || port.constant != null || faces.add(it) })
            }
        }

        private fun readOperand(tag: CompoundTag, key: String, inputs: Set<Int>): CombobulatorOperand {
            val id = tag.getInt("Source$key").takeIf { tag.contains("Source$key") && (it in inputs || it in registers) } ?: -1
            return CombobulatorOperand(id, tag.getDouble("Constant$key").takeIf { it.isFinite() } ?: 0.0)
        }

        private fun readTarget(tag: CompoundTag, key: String, count: Int, default: Int): Int {
            if (!tag.contains(key)) return default
            val target = tag.getInt(key)
            return if (target in STOP until count) target else STOP
        }

        private fun readFrequency(tag: CompoundTag, key: String): ItemStack {
            val stack = ItemStack.of(tag.getCompound(key))
            if (stack.isEmpty) return ItemStack.EMPTY
            val frequency = ItemStack(stack.item)
            stack.getTagElement("display")?.let { display ->
                if (display.contains("color", Tag.TAG_INT.toInt())) {
                    frequency.getOrCreateTagElement("display").putInt("color", display.getInt("color"))
                }
            }
            return frequency
        }
    }
}
