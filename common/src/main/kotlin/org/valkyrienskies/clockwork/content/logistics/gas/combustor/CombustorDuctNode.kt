package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import net.minecraft.core.Direction
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.INodeFlowGate
import org.valkyrienskies.kelvin.api.nodes.INodeFlowRateModifier
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.simulation.NodeFlowContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Kelvin solver-native combustion chamber node with one rear inlet and one front exhaust outlet.
 *
 * - Front ([facing]) port: unrestricted bidirectional exhaust so hot gas vents out and ambient air
 *   drawn back through a Gas Thruster can enter during a pulsejet's suction stroke.
 * - Rear ([facing.opposite]) port: one-way reed-valve inlet that closes when [throttle] is 0 or
 *   whenever chamber pressure exceeds inlet pressure, and smoothly meters inflow from a small pilot
 *   trickle when cold up to full throttle once lit.
 */
class CombustorDuctNode(
    pos: DuctNodePos,
    volume: Double,
    maxPressure: Double,
    maxTemperature: Double,
    heatCapacity: Double = 44.9,
) : PipeDuctNode(
    pos = pos,
    volume = volume,
    maxPressure = maxPressure,
    maxTemperature = maxTemperature,
    heatCapacity = heatCapacity,
), INodeFlowGate, INodeFlowRateModifier {

    @Volatile
    var facing: Direction = Direction.NORTH

    @Volatile
    var ambientPressure: Double = 0.0
        set(value) {
            field = if (value.isFinite() && value >= 0.0) value else 0.0
        }

    @Volatile
    var throttle: Double = 0.0
        set(value) {
            field = if (value.isFinite()) value.coerceIn(0.0, 1.0) else 0.0
        }

    enum class PortKind {
        EXHAUST,
        INLET,
        BLOCKED,
    }

    fun classifyNeighbor(neighbor: DuctNodePos): PortKind {
        if (neighbor.dimensionId != pos.dimensionId) return PortKind.BLOCKED
        val dx = (neighbor.x - pos.x).roundToInt()
        val dy = (neighbor.y - pos.y).roundToInt()
        val dz = (neighbor.z - pos.z).roundToInt()
        if (dy != 0) return PortKind.BLOCKED
        val f = facing
        if (dx == f.stepX && dz == f.stepZ) return PortKind.EXHAUST
        if (dx == -f.stepX && dz == -f.stepZ) return PortKind.INLET
        return PortKind.BLOCKED
    }

    fun isInletNeighbor(neighbor: DuctNodePos): Boolean =
        classifyNeighbor(neighbor) == PortKind.INLET

    fun isInletEdge(edge: DuctEdge): Boolean {
        val other = when (pos) {
            edge.nodeA -> edge.nodeB
            edge.nodeB -> edge.nodeA
            else -> return false
        }
        return isInletNeighbor(other)
    }

    /** Zeroes conductive heat transfer across the inlet edge so hot combustion chambers don't cook upstream fuel pipes. */
    fun insulateInletEdges() {
        for (edge in nodeEdges) {
            if (isInletEdge(edge)) {
                edge.wallThermalConductance = 0.0
                edge.thermalConductivityMultiplier = 0.0
            }
        }
    }

    override fun allowsFlow(context: NodeFlowContext, flowRate: Double): Boolean {
        return when (classifyNeighbor(context.neighbor.pos)) {
            PortKind.EXHAUST -> true
            PortKind.INLET -> flowRate < 0.0 && throttle > 0.0
            PortKind.BLOCKED -> false
        }
    }

    override fun modifyFlowRate(context: NodeFlowContext, flowRate: Double): Double {
        return when (classifyNeighbor(context.neighbor.pos)) {
            PortKind.EXHAUST -> flowRate
            PortKind.BLOCKED -> 0.0
            PortKind.INLET -> {
                if (flowRate >= 0.0 || throttle <= 0.0) return 0.0
                val heatOpen = ((context.node.temperature - PILOT_RAMP_START_K) / PILOT_RAMP_SPAN_K).coerceIn(0.0, 1.0)
                val lowAmbient = ((VACUUM_CHARGE_PRESSURE_PA - ambientPressure) / VACUUM_CHARGE_PRESSURE_PA)
                    .coerceIn(0.0, 1.0)
                val vacuumCharge = lowAmbient *
                    ((VACUUM_CHARGE_PRESSURE_PA - context.node.pressure) / VACUUM_CHARGE_PRESSURE_PA)
                        .coerceIn(0.0, 1.0)
                val openness = max(heatOpen * heatOpen, vacuumCharge * VACUUM_CHARGE_MAX_OPEN)
                val actualRateCap = (INLET_PILOT_RATE_KG_S + INLET_LIT_EXTRA_RATE_KG_S * openness) * throttle
                if (actualRateCap <= 0.0) return 0.0

                // JacobiSeidelSolver scales flow rates by relaxation (0.1) after modifyFlowRate.
                val solverRateCap = actualRateCap / SOLVER_RELAXATION_COMPENSATION
                max(flowRate, -solverRateCap)
            }
        }
    }

    companion object {
        private const val PILOT_RAMP_START_K = 450.0
        private const val PILOT_RAMP_SPAN_K = 400.0
        private const val VACUUM_CHARGE_PRESSURE_PA = 65_000.0
        private const val VACUUM_CHARGE_MAX_OPEN = 0.6

        // Target physical mass inflow cap on the single rear inlet (kg/s at 100% throttle).
        private const val INLET_PILOT_RATE_KG_S = 0.012
        private const val INLET_LIT_EXTRA_RATE_KG_S = 0.390

        private const val SOLVER_RELAXATION_COMPENSATION = 0.1
    }
}
