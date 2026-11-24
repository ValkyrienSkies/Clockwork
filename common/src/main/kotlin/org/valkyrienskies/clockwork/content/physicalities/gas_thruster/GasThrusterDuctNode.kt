package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.nodes.INodeSimulationStep
import org.valkyrienskies.kelvin.api.nodes.INodeStateSource
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.simulation.NodeStateChange
import org.valkyrienskies.kelvin.api.simulation.NodeStepContext
import org.valkyrienskies.kelvin.api.simulation.NodeStepResult
import org.valkyrienskies.kelvin.util.GasPhysics
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Kelvin solver-native Gas Thruster nozzle boundary node.
 *
 * Runs inside Kelvin's substeps (200 Hz) with three physically coupled regimes:
 * 1. Pressure-driven choked / subsonic nozzle outflow when chamber pressure > ambient pressure (produces thrust).
 * 2. Kadenacy exhaust-column inertia: a fast exhaust slug briefly coasts past ambient pressure when pressure
 *    falls, producing zero extra thrust while pulling a brief low-pressure suction pocket (~75-88 kPa).
 * 3. Ambient air refill: when the exhaust slug has come to rest and chamber pressure < ambient pressure
 *    (and the nozzle mouth is open to atmosphere), ambient air flows slowly back in through the nozzle up
 *    to ambient pressure — naturally powering valved pulsejets without magic air intake.
 */
class GasThrusterDuctNode(
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
), INodeStateSource, INodeSimulationStep {

    @Volatile var ambientPressure: Double = 0.0
    @Volatile var ambientTemperature: Double = 293.15
    @Volatile var ambientAirGas: GasType? = null
    @Volatile var nozzleOpenToAir: Boolean = false
    @Volatile var environmentValid: Boolean = false

    /** Exhaust-column inertia flow rate in kg/s (Kadenacy effect). */
    var exhaustMomentumFlow: Double = 0.0
        private set

    private enum class StepMode { NONE, OUTFLOW, REFILL }

    private var pendingMode: StepMode = StepMode.NONE
    private var pendingNozzleMdot: Double = 0.0
    private var pendingExitPressure: Double = 0.0
    private var pendingExhaustVelocity: Double = 0.0
    private var pendingChamberTemp: Double = 0.0
    private var pendingDt: Double = 0.0

    private val tickExpelledMass = HashMap<GasType, Double>()
    private var tickThrustImpulse = 0.0
    private var tickWeightedVelocity = 0.0
    private var tickWeightedTemp = 0.0
    private var tickWeightedExpansion = 0.0
    private var tickThrustMass = 0.0
    private var tickDuration = 0.0

    data class TickSummary(
        val expelledGasMasses: Map<GasType, Double>,
        val averageRawThrust: Double,
        val averageVelocity: Double,
        val averageTemperature: Double,
        val averageExpansion: Float,
    ) {
        val totalExpelledMass: Double get() = expelledGasMasses.values.sum()
    }

    fun drainTickSummary(): TickSummary {
        val duration = tickDuration
        val masses = if (tickExpelledMass.isEmpty()) emptyMap() else HashMap(tickExpelledMass)
        val avgRawThrust = if (duration > 1e-9 && tickThrustImpulse > 0.0) tickThrustImpulse / duration else 0.0
        val avgVelocity = if (tickThrustMass > 1e-12) tickWeightedVelocity / tickThrustMass else 0.0
        val avgTemp = if (tickThrustMass > 1e-12) tickWeightedTemp / tickThrustMass else 0.0
        val avgExpansion = if (tickThrustMass > 1e-12) (tickWeightedExpansion / tickThrustMass).coerceIn(0.0, 1.0).toFloat() else 0f

        tickExpelledMass.clear()
        tickThrustImpulse = 0.0
        tickWeightedVelocity = 0.0
        tickWeightedTemp = 0.0
        tickWeightedExpansion = 0.0
        tickThrustMass = 0.0
        tickDuration = 0.0

        return TickSummary(masses, avgRawThrust, avgVelocity, avgTemp, avgExpansion)
    }

    override fun getStateChange(context: NodeStepContext): NodeStateChange {
        pendingMode = StepMode.NONE
        pendingNozzleMdot = 0.0
        pendingExitPressure = 0.0
        pendingExhaustVelocity = 0.0
        pendingChamberTemp = 0.0
        pendingDt = 0.0

        val dt = context.step.durationSeconds
        if (!environmentValid || !dt.isFinite() || dt <= 0.0) return NodeStateChange.NONE
        val pAmb = ambientPressure
        if (!pAmb.isFinite() || pAmb < 0.0) return NodeStateChange.NONE

        pendingDt = dt
        val state = context.node
        val chamberPressure = state.pressure
        val chamberTemp = state.temperature
        if (!chamberPressure.isFinite() || !chamberTemp.isFinite() || chamberTemp <= 0.0) return NodeStateChange.NONE

        val gasMasses = state.gasMasses
        var availableMass = 0.0
        for (mass in gasMasses.values) {
            if (mass.isFinite() && mass > 0.0) availableMass += mass
        }

        // 1) Pressure-driven isentropic nozzle outflow (choked or subsonic)
        var nozzleMdot = 0.0
        var exitPressure = pAmb
        var exhaustVelocity = 0.0
        if (availableMass > 1e-12 && chamberPressure > pAmb) {
            val gamma = GasPhysics.gammaMix(gasMasses)
            val specificGasConstant = GasPhysics.mixtureR(gasMasses)
            if (gamma.isFinite() && specificGasConstant.isFinite() && gamma > 1.0 && specificGasConstant > 0.0) {
                val criticalRatio = (2.0 / (gamma + 1.0)).pow(gamma / (gamma - 1.0))
                val ambientRatio = (pAmb / chamberPressure).coerceIn(0.0, 1.0)
                val choked = ambientRatio <= criticalRatio
                exitPressure = if (choked) chamberPressure * criticalRatio else pAmb
                val exitRatio = (exitPressure / chamberPressure).coerceIn(0.0, 1.0)

                val theoretical = if (choked) {
                    GasPhysics.mdotChoked(
                        gasMasses, chamberPressure, chamberTemp,
                        NOZZLE_RADIUS_M, DISCHARGE_COEFFICIENT,
                    )
                } else {
                    val expansionTerm = (2.0 * gamma / (specificGasConstant * chamberTemp * (gamma - 1.0))) *
                        (ambientRatio.pow(2.0 / gamma) - ambientRatio.pow((gamma + 1.0) / gamma))
                    DISCHARGE_COEFFICIENT * NOZZLE_AREA_M2 * chamberPressure * sqrt(expansionTerm.coerceAtLeast(0.0))
                }
                val energyPerMass = (2.0 * gamma / (gamma - 1.0)) * specificGasConstant * chamberTemp *
                    (1.0 - exitRatio.pow((gamma - 1.0) / gamma))
                val ve = sqrt(energyPerMass.coerceAtLeast(0.0))

                if (theoretical.isFinite() && theoretical > 0.0 && ve.isFinite()) {
                    nozzleMdot = theoretical
                    exhaustVelocity = ve
                }
            }
        }

        // 2) Kadenacy exhaust-column momentum (only in atmosphere when nozzle mouth is open to air)
        if (pAmb > 0.0 && nozzleOpenToAir) {
            val jetTarget = if (exhaustVelocity >= KADENACY_MIN_VELOCITY_MS) nozzleMdot else 0.0
            if (jetTarget >= exhaustMomentumFlow && jetTarget > 0.0) {
                exhaustMomentumFlow += (jetTarget - exhaustMomentumFlow) * min(1.0, dt / KADENACY_CHARGE_TAU_S)
            } else if (exhaustMomentumFlow > 0.0) {
                val backpressureDecel = KADENACY_INERTANCE * max(pAmb - chamberPressure, 0.0)
                exhaustMomentumFlow = max(0.0, exhaustMomentumFlow - (backpressureDecel + KADENACY_DRAG * exhaustMomentumFlow) * dt)
            }
        } else {
            exhaustMomentumFlow = 0.0
        }

        // 3a) Outflow (pressure-driven jet and/or Kadenacy coasting suction)
        val outMdot = max(nozzleMdot, exhaustMomentumFlow)
        if (outMdot > 1e-6 && availableMass > 1e-12) {
            val massToExpel = min(outMdot * dt, availableMass * MAX_SUBSTEP_EXPEL_FRACTION)
            if (massToExpel <= 1e-12) return NodeStateChange.NONE

            val deltas = HashMap<GasType, Double>(gasMasses.size)
            var energyRemoved = 0.0
            for ((gas, mass) in gasMasses) {
                if (!mass.isFinite() || mass <= 0.0) continue
                val removed = min(mass, massToExpel * (mass / availableMass))
                if (removed <= 0.0) continue
                deltas[gas] = -removed
                energyRemoved += removed * (gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex) * chamberTemp
            }
            if (deltas.isEmpty() || !energyRemoved.isFinite()) return NodeStateChange.NONE

            pendingMode = StepMode.OUTFLOW
            pendingNozzleMdot = nozzleMdot
            pendingExitPressure = exitPressure
            pendingExhaustVelocity = exhaustVelocity
            pendingChamberTemp = chamberTemp
            return NodeStateChange(deltas, -energyRemoved)
        }

        // 3b) Ambient air refill through the open nozzle up to ambient pressure
        val air = ambientAirGas
        val tAmb = ambientTemperature
        if (pAmb > 0.0 && nozzleOpenToAir && chamberPressure < pAmb &&
            tAmb.isFinite() && tAmb > 0.0 && air != null && state.volume > 0.0
        ) {
            val airCv = air.specificHeatCapacity * 1000.0 / air.adiabaticIndex
            val airR = (air.adiabaticIndex - 1.0) * airCv
            if (airR <= 1e-12) return NodeStateChange.NONE
            val rhoAmb = pAmb / (airR * tAmb)
            val mdotIn = DISCHARGE_COEFFICIENT * (NOZZLE_AREA_M2 * REFILL_AREA_MULTIPLIER) *
                sqrt(2.0 * rhoAmb * (pAmb - chamberPressure))
            val maxMassToAmbient = (pAmb - chamberPressure) * state.volume / (airR * tAmb)
            val dmIn = min(mdotIn * dt, max(0.0, maxMassToAmbient))
            if (dmIn > 1e-12 && dmIn.isFinite()) {
                pendingMode = StepMode.REFILL
                return NodeStateChange(mapOf(air to dmIn), dmIn * airCv * tAmb)
            }
        }

        return NodeStateChange.NONE
    }

    override fun onStepEnd(context: NodeStepContext, result: NodeStepResult) {
        val dt = pendingDt
        if (dt <= 0.0) return
        tickDuration += dt

        if (pendingMode != StepMode.OUTFLOW) return
        val applied = result.appliedSourceChange.gasMassDeltas
        var expelledTotal = 0.0
        for (delta in applied.values) {
            if (delta < 0.0) expelledTotal += -delta
        }
        if (expelledTotal <= 1e-12) return

        val actualOutMdot = expelledTotal / dt
        val thrustMdot = min(actualOutMdot, pendingNozzleMdot)
        if (pendingNozzleMdot <= 1e-12 || thrustMdot <= 0.0) return

        val thrustMass = thrustMdot * dt
        val thrustFractionOfExpelled = (thrustMass / expelledTotal).coerceIn(0.0, 1.0)
        for ((gas, delta) in applied) {
            if (delta < 0.0) {
                tickExpelledMass.merge(gas, (-delta) * thrustFractionOfExpelled, Double::plus)
            }
        }

        val flowFraction = (thrustMdot / pendingNozzleMdot).coerceIn(0.0, 1.0)
        val pressureThrust = (pendingExitPressure - ambientPressure) * NOZZLE_AREA_M2 * flowFraction
        val rawThrust = thrustMdot * pendingExhaustVelocity + pressureThrust
        if (rawThrust.isFinite() && rawThrust > 0.0) {
            tickThrustImpulse += rawThrust * dt
            tickWeightedVelocity += pendingExhaustVelocity * thrustMass
            tickWeightedTemp += pendingChamberTemp * thrustMass
            val expansion = (1.0 - ambientPressure / pendingExitPressure.coerceAtLeast(1e-9)).coerceIn(0.0, 1.0)
            tickWeightedExpansion += expansion * thrustMass
            tickThrustMass += thrustMass
        }
    }

    companion object {
        const val DISCHARGE_COEFFICIENT = 0.8
        const val NOZZLE_AREA_M2 = 0.00154
        private val NOZZLE_RADIUS_M = sqrt(NOZZLE_AREA_M2 / PI)
        private const val MAX_SUBSTEP_EXPEL_FRACTION = 0.95

        // Kadenacy exhaust-column inertia tuning
        private const val KADENACY_MIN_VELOCITY_MS = 150.0
        private const val KADENACY_CHARGE_TAU_S = 0.05
        private const val KADENACY_INERTANCE = 1.5e-5
        private const val KADENACY_DRAG = 0.35

        // Open-mouth ambient air refill scaling
        private const val REFILL_AREA_MULTIPLIER = 25.0
    }
}
