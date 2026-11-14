package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFunction
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperation
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperationRuntime
import kotlin.math.roundToInt

enum class PlottingKind { FUNCTION, CONSTANT, INPUT, OUTPUT }
enum class PlottingConnection { THINKABLE, LINK, ADJACENT }

data class PlottingInput(val source: Int? = null, val constant: Double = 0.0, val connection: PlottingConnection = PlottingConnection.THINKABLE)

data class PlottingNode(
    val id: Int,
    val x: Int,
    val y: Int,
    val kind: PlottingKind = PlottingKind.FUNCTION,
    val name: String = "",
    val operation: CombobulatorOperation = CombobulatorOperation(),
    val inputs: List<PlottingInput> = List(3) { PlottingInput() },
    val value: Double = 0.0,
    val device: String = "",
    val rotation: Int = 0,
    val deviceSide: Direction = Direction.NORTH,
    val reading: String = "",
) {
    val inputCount get() = when (kind) {
        PlottingKind.FUNCTION -> operation.function.inputs
        PlottingKind.OUTPUT -> 1
        else -> 0
    }
    val hasOutput get() = kind != PlottingKind.OUTPUT

    fun direction(port: Int): Pair<Int, Int> {
        var direction = when (port) { -1 -> 1 to 0; 0 -> -1 to 0; 1 -> 0 to -1; else -> 0 to 1 }
        repeat(rotation) { direction = -direction.second to direction.first }
        return direction
    }

    fun touches(source: PlottingNode, port: Int): Boolean {
        val delta = x - source.x to y - source.y
        val input = direction(port)
        return delta == source.direction(-1) && delta == (-input.first to -input.second)
    }
}

data class PlottingPlan(val name: String = "Untitled", val nodes: List<PlottingNode> = emptyList()) {
    fun write() = CompoundTag().apply {
        putString("Name", name)
        put("Nodes", ListTag().apply {
            nodes.forEach { node -> add(CompoundTag().apply {
                putInt("Id", node.id)
                putInt("X", node.x)
                putInt("Y", node.y)
                putString("Kind", node.kind.name)
                putString("Name", node.name)
                put("Operation", node.operation.write())
                putDouble("Value", node.value)
                putString("Device", node.device)
                putString("DeviceSide", node.deviceSide.serializedName)
                putString("Reading", node.reading)
                putInt("Rotation", node.rotation)
                put("Inputs", ListTag().apply {
                    node.inputs.forEach { input -> add(CompoundTag().apply {
                        input.source?.let { putInt("Source", it) }
                        putDouble("Constant", input.constant)
                        putString("Connection", input.connection.name)
                    }) }
                })
            }) }
        })
    }

    companion object {
        const val MAX_PLANS = 8
        const val MAX_NODES = 128
        const val GRID_LIMIT = 64

        fun read(tag: CompoundTag): PlottingPlan {
            val ids = mutableSetOf<Int>()
            val cells = mutableSetOf<Pair<Int, Int>>()
            val nodes = tag.getList("Nodes", Tag.TAG_COMPOUND.toInt()).take(MAX_NODES).mapNotNull { entry ->
                val n = entry as CompoundTag
                val id = n.getInt("Id")
                val x = n.getInt("X").coerceIn(-GRID_LIMIT, GRID_LIMIT)
                val y = n.getInt("Y").coerceIn(-GRID_LIMIT, GRID_LIMIT)
                if (id !in 1..MAX_NODES || !ids.add(id) || !cells.add(x to y)) return@mapNotNull null
                val bindings = n.getList("Inputs", Tag.TAG_COMPOUND.toInt())
                PlottingNode(id, x, y,
                    PlottingKind.entries.firstOrNull { it.name == n.getString("Kind") } ?: PlottingKind.FUNCTION,
                    n.getString("Name").take(40), CombobulatorOperation.read(n.getCompound("Operation")),
                    List(3) { index ->
                        val input = bindings.getCompound(index)
                        PlottingInput(input.getInt("Source").takeIf { input.contains("Source") && it in 1..MAX_NODES },
                            input.getDouble("Constant").takeIf { it.isFinite() } ?: 0.0,
                            PlottingConnection.entries.firstOrNull { it.name == input.getString("Connection") }
                                ?: if (input.getBoolean("Redstone")) PlottingConnection.LINK else PlottingConnection.THINKABLE)
                    }, n.getDouble("Value").takeIf { it.isFinite() } ?: 0.0,
                    readDevice(n.getString("Device")), n.getInt("Rotation").mod(4),
                    Direction.byName(n.getString("DeviceSide")) ?: Direction.NORTH,
                    n.getString("Reading").take(128))
            }
            return PlottingPlan(tag.getString("Name").take(40), nodes)
        }

        fun example() = PlottingPlan("Threshold test", listOf(
            PlottingNode(1, 0, 1, PlottingKind.INPUT, "Foot distance", value = -1.0, device = "vs_clockwork:distance_sensor"),
            PlottingNode(2, 2, 0, name = "Valid hit", operation = CombobulatorOperation(CombobulatorFunction.COMPARE, high = 1.0),
                inputs = listOf(PlottingInput(1), PlottingInput(constant = 0.0), PlottingInput())),
            PlottingNode(3, 2, 2, name = "Near ground", operation = CombobulatorOperation(CombobulatorFunction.COMPARE,
                comparison = org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorComparison.LESS_OR_EQUAL, high = 1.0),
                inputs = listOf(PlottingInput(1), PlottingInput(constant = 0.5), PlottingInput())),
            PlottingNode(4, 4, 1, name = "Grounded", operation = CombobulatorOperation(CombobulatorFunction.AND, high = 1.0),
                inputs = listOf(PlottingInput(2), PlottingInput(3), PlottingInput())),
            PlottingNode(5, 6, 1, PlottingKind.OUTPUT, "Ready", inputs = listOf(PlottingInput(4), PlottingInput(), PlottingInput()))
        ))

        private fun readDevice(id: String) = id.takeIf { it.length <= 256 && ResourceLocation.tryParse(it) != null } ?: ""
    }
}

internal class PlottingSimulation {
    private val runtimes = mutableMapOf<Int, CombobulatorOperationRuntime>()
    private val testInputs = mutableMapOf<Int, Double>()
    var values: Map<Int, Double> = emptyMap()
        private set
    var ticks = 0L
        private set

    fun reset(plan: PlottingPlan) {
        runtimes.clear()
        testInputs.clear()
        ticks = 0
        values = plan.nodes.associate { it.id to if (it.inputCount == 0) it.value else 0.0 }
    }

    fun inputValue(node: PlottingNode) = if (node.kind == PlottingKind.INPUT) testInputs[node.id] ?: node.value else node.value

    fun setInput(id: Int, value: Double) {
        require(value.isFinite())
        testInputs[id] = value
    }

    fun tick(plan: PlottingPlan) {
        val snapshot = values.toMutableMap()
        plan.nodes.filter { it.kind == PlottingKind.CONSTANT || it.kind == PlottingKind.INPUT }
            .forEach { snapshot[it.id] = inputValue(it) }
        val sources = plan.nodes.filter { it.hasOutput }.map { it.id }.toSet()
        val nodes = plan.nodes.associateBy { it.id }
        val next = mutableMapOf<Int, Double>()
        for (node in plan.nodes) {
            val inputs = DoubleArray(3) { index ->
                val binding = node.inputs[index]
                val source = nodes[binding.source]
                val connected = binding.source in sources && (binding.connection != PlottingConnection.ADJACENT ||
                    source != null && node.touches(source, index))
                val value = if (binding.source == null) binding.constant else snapshot[binding.source].takeIf { connected } ?: Double.NaN
                if (binding.connection != PlottingConnection.LINK || binding.source == null) value else if (!value.isFinite()) 0.0 else value.coerceIn(0.0, 15.0).roundToInt().toDouble()
            }
            next[node.id] = when (node.kind) {
                PlottingKind.FUNCTION -> runtimes.getOrPut(node.id) { CombobulatorOperationRuntime() }
                    .apply { tick(node.operation, inputs) }.output
                PlottingKind.CONSTANT, PlottingKind.INPUT -> inputValue(node)
                PlottingKind.OUTPUT -> inputs[0]
            }
        }
        values = next
        ticks++
    }
}
