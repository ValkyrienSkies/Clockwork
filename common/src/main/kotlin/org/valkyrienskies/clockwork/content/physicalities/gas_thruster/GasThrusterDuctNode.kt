package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import org.valkyrienskies.clockwork.content.logistics.gas.combustor.CombustorDuctNode
import org.valkyrienskies.clockwork.content.logistics.gas.thermal.ThermalTransfer
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.nodes.INodeSimulationStep
import org.valkyrienskies.kelvin.api.nodes.INodeStateSource
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode
import org.valkyrienskies.kelvin.api.simulation.NodeStateChange
import org.valkyrienskies.kelvin.api.simulation.NodeStepContext
import org.valkyrienskies.kelvin.api.simulation.NodeStepResult
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.kelvin.util.NodeThermodynamics
import kotlin.math.PI
import kotlin.math.abs
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
    @Volatile var ambientHeatTransferCoefficient: Double = DEFAULT_AMBIENT_HEAT_TRANSFER_COEFFICIENT

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

        // Ambient air thermal equalization (like a radiator, scaled by air pressure so vacuum is thermally insulated)
        val tAmb = ambientTemperature
        val ambientHeatDelta = if (pAmb > 0.0 && tAmb.isFinite() && tAmb > 0.0 && ambientHeatTransferCoefficient > 0.0) {
            val airScale = (pAmb / SEA_LEVEL_PRESSURE_PA).coerceIn(0.0, 1.0)
            val nodeCapacity = GasPhysics.nodeHeatCapacity(gasMasses, heatCapacity)
            val delta = ThermalTransfer.equalize(
                state.heatEnergy, nodeCapacity, tAmb, ambientHeatTransferCoefficient * airScale, dt,
            )
            if (abs(delta) > 1e-9) delta else 0.0
        } else {
            0.0
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
                val nextMomentum = exhaustMomentumFlow - (backpressureDecel + KADENACY_DRAG * exhaustMomentumFlow) * dt
                exhaustMomentumFlow = if (nextMomentum < KADENACY_STOP_FLOW_KG_S) 0.0 else nextMomentum
            }
        } else {
            exhaustMomentumFlow = 0.0
        }

        // 3a) Outflow (pressure-driven jet and/or Kadenacy coasting suction)
        val outMdot = max(nozzleMdot, exhaustMomentumFlow)
        if (outMdot > 1e-6 && availableMass > 1e-12) {
            val massToExpel = min(outMdot * dt, availableMass * MAX_SUBSTEP_EXPEL_FRACTION)
            if (massToExpel <= 1e-12) return NodeStateChange.NONE

            val deltas = HashMap<GasType, Double>(gasMasses.size + 1)
            var energyRemoved = 0.0
            for ((gas, mass) in gasMasses) {
                if (!mass.isFinite() || mass <= 0.0) continue
                val removed = min(mass, massToExpel * (mass / availableMass))
                if (removed <= 0.0) continue
                deltas[gas] = -removed
                energyRemoved += removed * (gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex) * chamberTemp
            }
            if (deltas.isEmpty() || !energyRemoved.isFinite()) return NodeStateChange.NONE

            // If we are barely trickling (not jetting or coasting), open-mouth buoyant exchange still lets ambient air in
            // so a slow unlit fuel trickle cannot permanently suffocate an open thruster.
            var energyAdded = 0.0
            val air = ambientAirGas
            if (pAmb > 0.0 && nozzleOpenToAir && exhaustMomentumFlow <= 0.0 &&
                exhaustVelocity < OPEN_MOUTH_EXCHANGE_MAX_VELOCITY_MS &&
                tAmb.isFinite() && tAmb > 0.0 && air != null && state.volume > 0.0
            ) {
                val airCv = air.specificHeatCapacity * 1000.0 / air.adiabaticIndex
                val airR = (air.adiabaticIndex - 1.0) * airCv
                if (airR > 1e-12) {
                    val exchangeFrac = min(0.5, dt / OPEN_MOUTH_EXCHANGE_TAU_S)
                    var extraRemovedPressure = 0.0
                    for ((gas, mass) in gasMasses) {
                        if (gas == air || !mass.isFinite() || mass <= 1e-12) continue
                        val alreadyRemoved = -(deltas[gas] ?: 0.0)
                        val remaining = (mass - alreadyRemoved).coerceAtLeast(0.0)
                        val dmOut = remaining * exchangeFrac
                        if (dmOut <= 1e-12) continue
                        val gasCv = gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
                        val gasR = (gas.adiabaticIndex - 1.0) * gasCv
                        deltas[gas] = -(alreadyRemoved + dmOut)
                        energyRemoved += dmOut * gasCv * chamberTemp
                        extraRemovedPressure += dmOut * gasR * chamberTemp / state.volume
                    }
                    if (extraRemovedPressure > 0.0) {
                        val dmIn = extraRemovedPressure * state.volume / (airR * tAmb)
                        if (dmIn > 1e-12 && dmIn.isFinite()) {
                            deltas[air] = (deltas[air] ?: 0.0) + dmIn
                            energyAdded += dmIn * airCv * tAmb
                        }
                    }
                }
            }

            pendingMode = StepMode.OUTFLOW
            pendingNozzleMdot = nozzleMdot
            pendingExitPressure = exitPressure
            pendingExhaustVelocity = exhaustVelocity
            pendingChamberTemp = chamberTemp
            return NodeStateChange(deltas, -energyRemoved + energyAdded + ambientHeatDelta)
        }

        // 3b) Open-mouth atmospheric refill + passive exchange of stagnant non-air gas up to ambient pressure
        val air = ambientAirGas
        if (pAmb > 0.0 && nozzleOpenToAir &&
            tAmb.isFinite() && tAmb > 0.0 && air != null && state.volume > 0.0
        ) {
            val airCv = air.specificHeatCapacity * 1000.0 / air.adiabaticIndex
            val airR = (air.adiabaticIndex - 1.0) * airCv
            if (airR > 1e-12) {
                val deltas = HashMap<GasType, Double>(gasMasses.size + 1)
                var energyDelta = ambientHeatDelta
                var removedPressure = 0.0

                // Passive diffusion/buoyant exchange at the open mouth purges stagnant fuel/exhaust when not jetting.
                val exchangeFrac = min(0.5, dt / OPEN_MOUTH_EXCHANGE_TAU_S)
                for ((gas, mass) in gasMasses) {
                    if (gas == air || !mass.isFinite() || mass <= 1e-12) continue
                    val dmOut = mass * exchangeFrac
                    if (dmOut <= 1e-12) continue
                    val gasCv = gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
                    val gasR = (gas.adiabaticIndex - 1.0) * gasCv
                    deltas[gas] = -dmOut
                    energyDelta -= dmOut * gasCv * chamberTemp
                    removedPressure += dmOut * gasR * chamberTemp / state.volume
                }

                val effectivePressure = (chamberPressure - removedPressure).coerceAtLeast(0.0)
                if (effectivePressure < pAmb) {
                    val rhoAmb = pAmb / (airR * tAmb)
                    val mdotIn = DISCHARGE_COEFFICIENT * (NOZZLE_AREA_M2 * REFILL_AREA_MULTIPLIER) *
                        sqrt(2.0 * rhoAmb * (pAmb - effectivePressure))
                    val maxMassToAmbient = (pAmb - effectivePressure) * state.volume / (airR * tAmb)
                    val dmIn = min(mdotIn * dt, max(0.0, maxMassToAmbient))
                    if (dmIn > 1e-12 && dmIn.isFinite()) {
                        deltas[air] = dmIn
                        energyDelta += dmIn * airCv * tAmb
                    }
                }

                if ((deltas.isNotEmpty() || abs(energyDelta) > 1e-9) && energyDelta.isFinite()) {
                    pendingMode = StepMode.REFILL
                    return NodeStateChange(deltas, energyDelta)
                }
            }
        }

        if (ambientHeatDelta != 0.0) {
            return NodeStateChange(emptyMap(), ambientHeatDelta)
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
        private const val KADENACY_MIN_VELOCITY_MS = 100.0
        private const val KADENACY_CHARGE_TAU_S = 0.04
        private const val KADENACY_INERTANCE = 5.0e-6
        private const val KADENACY_DRAG = 0.25
        private const val KADENACY_STOP_FLOW_KG_S = 0.015

        // Open-mouth ambient air refill and passive stagnant-gas exchange
        private const val REFILL_AREA_MULTIPLIER = 35.0
        private const val OPEN_MOUTH_EXCHANGE_TAU_S = 1.2
        private const val OPEN_MOUTH_EXCHANGE_MAX_VELOCITY_MS = 60.0
        private const val TAILPIPE_DIFFUSION_TAU_S = 0.6
        private const val TAILPIPE_DIFFUSION_TARGET_AIR_FRACTION = 0.65
        private const val TAILPIPE_DIFFUSION_MAX_OVERPRESSURE_PA = 15_000.0
        private const val TAILPIPE_DIFFUSION_MAX_DELTA_P_PA = 8_000.0
        private const val TAILPIPE_DIFFUSION_MAX_HOPS = 3

        // Ambient air thermal equalization (W/K at sea level, matching Radiator default)
        private const val SEA_LEVEL_PRESSURE_PA = 101_325.0
        const val DEFAULT_AMBIENT_HEAT_TRANSFER_COEFFICIENT = 5.0

        /**
         * Conservatively mixes stagnant gas species (Fickian/buoyant diffusion) along an open atmospheric
         * tailpipe (up to [TAILPIPE_DIFFUSION_MAX_HOPS] hops back from an open thruster mouth) when not jetting.
         *
         * Strictly conserves every gas species mass and total thermal energy, never crosses a combustor's rear
         * reed-valve inlet or a closed valve, and does nothing in vacuum or during active pressurized jetting.
         */
        fun diffuseOpenTailpipe(
            network: DuctNetwork<*>,
            thrusterPos: DuctNodePos,
            ambientPressure: Double,
            dtSeconds: Double,
        ) {
            if (!ambientPressure.isFinite() || ambientPressure <= 0.0 || dtSeconds <= 0.0) return
            val thrusterNode = network.getNodeAt(thrusterPos) as? GasThrusterDuctNode ?: return
            if (!thrusterNode.nozzleOpenToAir || thrusterNode.exhaustMomentumFlow > 0.0) return

            val maxP = ambientPressure + TAILPIPE_DIFFUSION_MAX_OVERPRESSURE_PA
            val visited = HashSet<DuctNodePos>(8)
            val queue = ArrayDeque<Pair<DuctNodePos, Int>>()
            visited.add(thrusterPos)
            queue.addLast(thrusterPos to 0)

            val alpha = min(0.35, dtSeconds / TAILPIPE_DIFFUSION_TAU_S)
            while (queue.isNotEmpty()) {
                val (posA, depth) = queue.removeFirst()
                if (depth >= TAILPIPE_DIFFUSION_MAX_HOPS) continue
                val nodeA = network.getNodeAt(posA) ?: continue
                val infoA = network.nodeInfo[posA] ?: continue
                val pA = infoA.currentPressure
                if (!pA.isFinite() || pA > maxP) continue

                for (edge in nodeA.nodeEdges) {
                    if (nodeA is CombustorDuctNode && nodeA.isInletEdge(edge)) continue
                    val posB = when (posA) {
                        edge.nodeA -> edge.nodeB
                        edge.nodeB -> edge.nodeA
                        else -> continue
                    }
                    if (!visited.add(posB)) continue
                    val nodeB = network.getNodeAt(posB) ?: continue
                    if (nodeB is CombustorDuctNode && nodeB.isInletEdge(edge)) continue
                    if (nodeB is ValveDuctNode && nodeB.radius <= 1e-6) continue
                    if (nodeB !is PipeDuctNode) continue
                    val infoB = network.nodeInfo[posB] ?: continue
                    val pB = infoB.currentPressure
                    if (!pB.isFinite() || pB > maxP || abs(pA - pB) > TAILPIPE_DIFFUSION_MAX_DELTA_P_PA) continue

                    val volA = (nodeA.volume + infoA.volumeChange).takeIf { it.isFinite() && it > 1e-6 } ?: continue
                    val volB = (nodeB.volume + infoB.volumeChange).takeIf { it.isFinite() && it > 1e-6 } ?: continue
                    val tA = infoA.currentTemperature.takeIf { it.isFinite() && it > 0.0 } ?: continue
                    val tB = infoB.currentTemperature.takeIf { it.isFinite() && it > 0.0 } ?: continue

                    // Only convectively scavenge when the upstream chamber/duct is actually oxygen-starved
                    // relative to the open thruster mouth; leave air-rich startup pilot charges undisturbed.
                    val air = thrusterNode.ambientAirGas
                    if (air != null) {
                        val totalA = infoA.currentGasMasses.values.sum()
                        val totalB = infoB.currentGasMasses.values.sum()
                        val airFracA = if (totalA > 1e-12) infoA.currentGasMasses.getDouble(air) / totalA else 0.0
                        val airFracB = if (totalB > 1e-12) infoB.currentGasMasses.getDouble(air) / totalB else 0.0
                        if (airFracB >= TAILPIPE_DIFFUSION_TARGET_AIR_FRACTION || airFracA <= airFracB + 0.05) {
                            if (nodeB !is CombustorDuctNode) queue.addLast(posB to (depth + 1))
                            continue
                        }
                    }

                    val swapVol = alpha * min(volA, volB)
                    val fracA = (swapVol / volA).coerceIn(0.0, 0.35)
                    val fracB = (swapVol / volB).coerceIn(0.0, 0.35)

                    val gases = HashSet<GasType>(infoA.currentGasMasses.size + infoB.currentGasMasses.size)
                    gases.addAll(infoA.currentGasMasses.keys)
                    gases.addAll(infoB.currentGasMasses.keys)
                    if (gases.isEmpty()) continue

                    var energyDeltaA = 0.0
                    var energyDeltaB = 0.0
                    var changed = false
                    for (gas in gases) {
                        val mA = infoA.currentGasMasses.getDouble(gas).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                        val mB = infoB.currentGasMasses.getDouble(gas).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                        val outA = mA * fracA
                        val outB = mB * fracB
                        if (outA <= 1e-12 && outB <= 1e-12) continue
                        val netToB = outA - outB
                        val cv = gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
                        val nextA = (mA - netToB).coerceAtLeast(0.0)
                        val nextB = (mB + netToB).coerceAtLeast(0.0)
                        if (nextA <= 1e-12) infoA.currentGasMasses.removeDouble(gas) else infoA.currentGasMasses.put(gas, nextA)
                        if (nextB <= 1e-12) infoB.currentGasMasses.removeDouble(gas) else infoB.currentGasMasses.put(gas, nextB)
                        energyDeltaA += outB * cv * tB - outA * cv * tA
                        energyDeltaB += outA * cv * tA - outB * cv * tB
                        changed = true
                    }
                    if (changed) {
                        infoA.currentEnergy = (infoA.currentEnergy + energyDeltaA).coerceAtLeast(0.001)
                        infoB.currentEnergy = (infoB.currentEnergy + energyDeltaB).coerceAtLeast(0.001)
                        NodeThermodynamics.update(nodeA, infoA)
                        NodeThermodynamics.update(nodeB, infoB)
                    }

                    // Keep walking backward through plain spacer ducts, but stop at a combustion chamber.
                    if (nodeB !is CombustorDuctNode) {
                        queue.addLast(posB to (depth + 1))
                    }
                }
            }
        }
    }
}
