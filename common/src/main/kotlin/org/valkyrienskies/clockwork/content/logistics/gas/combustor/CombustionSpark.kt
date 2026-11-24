package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import com.google.gson.JsonElement
import net.minecraft.world.level.Level
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.DuctNodeState
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.api.recipe.GasReactionRequirement
import org.valkyrienskies.kelvin.api.simulation.NodeMaterialState
import org.valkyrienskies.kelvin.impl.recipe.DefaultKelvinRequirements
import org.valkyrienskies.kelvin.impl.recipe.KelvinReactionDataLoader
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.kelvin.util.NodeThermodynamics
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * Physically grounded "Spark Bubble" ignition helper.
 *
 * Instead of cheating reaction requirements or injecting huge bulk heat, a small finite spark
 * ([DEFAULT_SPARK_JOULES]) heats only a tiny local micro-bubble of the node's gas mixture up to
 * [DEFAULT_SPARK_TARGET_K], evaluates Kelvin's real reaction recipes and every requirement on that
 * bubble, and then propagates the flame front into the remaining cold gas only if the exothermic
 * heat released sustains the reaction temperature.
 *
 * Total mass and energy are conserved: inert gases receive only the tiny spark joules, overly
 * diluted mixtures quench immediately, and endothermic reactions self-extinguish.
 */
object CombustionSpark {
    const val DEFAULT_SPARK_JOULES = 2000.0
    const val DEFAULT_SPARK_TARGET_K = 1050.0

    private const val MIN_BUBBLE_REACTANT_KG = 1e-9
    private const val MAX_FLAME_STEPS = 32
    private const val REQUIREMENT_BISECTION_STEPS = 16
    private const val FLAME_MARGIN_K = 25.0
    private const val DEFAULT_HOLD_TEMP_K = 800.0

    data class SparkResult(
        val ignited: Boolean,
        val initialTemperature: Double,
        val finalTemperature: Double,
        val reactionsRan: Int,
    )

    fun ignite(
        level: Level,
        network: DuctNetwork<*>,
        pos: DuctNodePos,
        sparkJoules: Double = DEFAULT_SPARK_JOULES,
        targetTempK: Double = DEFAULT_SPARK_TARGET_K,
        reactions: Collection<GasBaseRecipe> = KelvinReactionDataLoader.gas_reactions.values,
    ): SparkResult = ignite(
        network = network,
        pos = pos,
        sparkJoules = sparkJoules,
        targetTempK = targetTempK,
        reactions = reactions,
        requirementCheck = { requirement, state, value -> requirement.apply_requirement(level, state, value) },
    )

    internal fun ignite(
        network: DuctNetwork<*>,
        pos: DuctNodePos,
        sparkJoules: Double = DEFAULT_SPARK_JOULES,
        targetTempK: Double = DEFAULT_SPARK_TARGET_K,
        reactions: Collection<GasBaseRecipe> = KelvinReactionDataLoader.gas_reactions.values,
        requirementCheck: (GasReactionRequirement, DuctNodeState, JsonElement) -> Boolean,
    ): SparkResult {
        val node = network.getNodeAt(pos) ?: return SparkResult(false, 0.0, 0.0, 0)
        val info = network.nodeInfo[pos] ?: return SparkResult(false, 0.0, 0.0, 0)
        val volume = (node.volume + info.volumeChange).takeIf { it.isFinite() && it > 0.0 }
            ?: return SparkResult(false, 0.0, 0.0, 0)
        val wallCap = node.heatCapacity.coerceAtLeast(0.0)

        val initialMasses = HashMap<GasType, Double>()
        for ((gas, mass) in info.currentGasMasses) {
            if (mass.isFinite() && mass > 0.0) initialMasses[gas] = mass
        }
        val initialCap = GasPhysics.nodeHeatCapacity(initialMasses, wallCap)
        val initialEnergy = info.currentEnergy.takeIf { it.isFinite() && it >= 0.0 } ?: (initialCap * 273.15)
        val initialTemp = if (initialCap > 1e-12) max(initialEnergy / initialCap, 1e-4) else 273.15

        val gasCap = GasPhysics.mixtureCapacity(initialMasses)
        if (gasCap <= 1e-12 || sparkJoules <= 0.0) {
            val nextEnergy = (initialEnergy + sparkJoules.coerceAtLeast(0.0)).coerceAtLeast(0.001)
            info.currentEnergy = nextEnergy
            NodeThermodynamics.update(node, info)
            return SparkResult(false, initialTemp, info.currentTemperature, 0)
        }

        val deltaT = max(targetTempK - initialTemp, 50.0)
        val initialFraction = min(1.0, sparkJoules / (gasCap * deltaT))

        val bubble = HashMap<GasType, Double>(initialMasses.size)
        val rest = HashMap<GasType, Double>(initialMasses.size)
        for ((gas, mass) in initialMasses) {
            val inBubble = mass * initialFraction
            val inRest = mass - inBubble
            if (inBubble > 0.0) bubble[gas] = inBubble
            if (inRest > 1e-12) rest[gas] = inRest
        }

        var bubbleEnergy = GasPhysics.mixtureCapacity(bubble) * initialTemp + sparkJoules
        var restGasEnergy = GasPhysics.mixtureCapacity(rest) * initialTemp
        val wallEnergy = wallCap * initialTemp

        var totalReactionsRan = 0
        var lowestReqTemp: Double? = null

        val firstPass = reactBubble(
            node = node,
            pos = pos,
            bubble = bubble,
            bubbleEnergy = bubbleEnergy,
            bubbleVolume = volume * max(initialFraction, 1e-6),
            reactions = reactions,
            requirementCheck = requirementCheck,
        )
        bubbleEnergy = firstPass.energy
        totalReactionsRan += firstPass.reactionsRan
        lowestReqTemp = firstPass.lowestMinTemp

        if (firstPass.reactionsRan > 0) {
            for (step in 0 until MAX_FLAME_STEPS) {
                val restCap = GasPhysics.mixtureCapacity(rest)
                if (restCap <= 1e-9) break

                val bubbleCap = GasPhysics.mixtureCapacity(bubble)
                if (bubbleCap <= 1e-12) break
                val bubbleTemp = bubbleEnergy / bubbleCap
                val holdTemp = (lowestReqTemp ?: DEFAULT_HOLD_TEMP_K) + FLAME_MARGIN_K
                if (bubbleTemp <= holdTemp) break

                // Entrain only as much cold gas as the bubble's excess thermal margin can hold above holdTemp.
                val entrainCapacity = bubbleCap * (bubbleTemp - holdTemp) / max(holdTemp - initialTemp, 1.0)
                val scoopFraction = (entrainCapacity / restCap).coerceIn(0.0, 1.0)
                if (scoopFraction < 1e-5) break

                val restKeys = rest.keys.toList()
                for (gas in restKeys) {
                    val available = rest[gas] ?: continue
                    val moved = available * scoopFraction
                    if (moved <= 0.0) continue
                    bubble[gas] = (bubble[gas] ?: 0.0) + moved
                    val remaining = available - moved
                    if (remaining <= 1e-12) rest.remove(gas) else rest[gas] = remaining
                    val movedEnergy = moved * (gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex) * initialTemp
                    bubbleEnergy += movedEnergy
                    restGasEnergy = (restGasEnergy - movedEnergy).coerceAtLeast(0.0)
                }

                val bubbleMass = bubble.values.sum()
                val totalMass = bubbleMass + rest.values.sum()
                val bubbleFrac = if (totalMass > 1e-12) bubbleMass / totalMass else 1.0
                val pass = reactBubble(
                    node = node,
                    pos = pos,
                    bubble = bubble,
                    bubbleEnergy = bubbleEnergy,
                    bubbleVolume = volume * max(bubbleFrac, 1e-6),
                    reactions = reactions,
                    requirementCheck = requirementCheck,
                )
                bubbleEnergy = pass.energy
                totalReactionsRan += pass.reactionsRan
                if (pass.lowestMinTemp != null) {
                    lowestReqTemp = if (lowestReqTemp == null) pass.lowestMinTemp else min(lowestReqTemp, pass.lowestMinTemp)
                }
                if (pass.reactionsRan == 0) break
            }
        }

        // Recombine bubble + rest + wall thermal energy back into the real node.
        info.currentGasMasses.clear()
        val combined = HashMap<GasType, Double>(bubble)
        for ((gas, mass) in rest) {
            combined[gas] = (combined[gas] ?: 0.0) + mass
        }
        for ((gas, mass) in combined) {
            if (mass > 1e-12) info.currentGasMasses.put(gas, mass)
        }
        info.currentEnergy = (bubbleEnergy + restGasEnergy + wallEnergy).coerceAtLeast(0.001)
        NodeThermodynamics.update(node, info)

        return SparkResult(
            ignited = totalReactionsRan > 0,
            initialTemperature = initialTemp,
            finalTemperature = info.currentTemperature,
            reactionsRan = totalReactionsRan,
        )
    }

    private data class BubblePass(val energy: Double, val reactionsRan: Int, val lowestMinTemp: Double?)

    private fun reactBubble(
        node: DuctNode,
        pos: DuctNodePos,
        bubble: HashMap<GasType, Double>,
        bubbleEnergy: Double,
        bubbleVolume: Double,
        reactions: Collection<GasBaseRecipe>,
        requirementCheck: (GasReactionRequirement, DuctNodeState, JsonElement) -> Boolean,
    ): BubblePass {
        var energy = bubbleEnergy
        var ran = 0
        var lowestMinTemp: Double? = null

        for (reaction in reactions) {
            val maxAmount = maxAmountFromInputs(bubble, reaction.gasses)
            if (maxAmount <= 0.0) continue

            val amount = limitByRequirements(node, pos, bubble, energy, bubbleVolume, reaction, maxAmount, requirementCheck)
            if (amount <= 0.0) continue

            for ((gas, req) in reaction.gasses) {
                val next = (bubble[gas] ?: 0.0) - amount * req
                if (next <= 1e-12) bubble.remove(gas) else bubble[gas] = next
            }
            for ((gas, res) in reaction.result) {
                bubble[gas] = (bubble[gas] ?: 0.0) + amount * res
            }
            energy = (energy + amount * reaction.energy).coerceAtLeast(0.001)
            ran++

            val minT = reaction.requirements[DefaultKelvinRequirements.minTemperature]?.asDouble
            if (minT != null && minT.isFinite()) {
                lowestMinTemp = if (lowestMinTemp == null) minT else min(lowestMinTemp, minT)
            }
        }

        return BubblePass(energy, ran, lowestMinTemp)
    }

    private fun maxAmountFromInputs(gasMasses: Map<GasType, Double>, inputs: Map<GasType, Double>): Double {
        if (inputs.isEmpty()) return 0.0
        var amount = Double.MAX_VALUE
        for ((gas, required) in inputs) {
            if (required <= 0.0) return 0.0
            val available = gasMasses[gas] ?: return 0.0
            if (available < MIN_BUBBLE_REACTANT_KG) return 0.0
            amount = min(amount, available / required)
        }
        return amount
    }

    private fun limitByRequirements(
        node: DuctNode,
        pos: DuctNodePos,
        bubble: Map<GasType, Double>,
        bubbleEnergy: Double,
        bubbleVolume: Double,
        reaction: GasBaseRecipe,
        maxAmount: Double,
        requirementCheck: (GasReactionRequirement, DuctNodeState, JsonElement) -> Boolean,
    ): Double {
        if (reaction.requirements.isEmpty()) return maxAmount

        fun holdsAfter(amount: Double): Boolean {
            val preview = previewBubble(node, pos, bubble, bubbleEnergy, bubbleVolume, reaction, amount)
            return reaction.requirements.all { (req, value) -> requirementCheck(req, preview, value) }
        }

        if (!holdsAfter(0.0)) return 0.0
        if (holdsAfter(maxAmount)) return maxAmount

        var lo = 0.0
        var hi = maxAmount
        repeat(REQUIREMENT_BISECTION_STEPS) {
            val mid = 0.5 * (lo + hi)
            if (holdsAfter(mid)) lo = mid else hi = mid
        }
        return lo
    }

    private fun previewBubble(
        node: DuctNode,
        pos: DuctNodePos,
        bubble: Map<GasType, Double>,
        bubbleEnergy: Double,
        bubbleVolume: Double,
        reaction: GasBaseRecipe,
        amount: Double,
    ): DuctNodeState {
        val masses = HashMap<GasType, Double>(bubble)
        for ((gas, mass) in reaction.gasses) masses[gas] = ((masses[gas] ?: 0.0) - amount * mass).coerceAtLeast(0.0)
        for ((gas, mass) in reaction.result) masses[gas] = (masses[gas] ?: 0.0) + amount * mass
        val energy = (bubbleEnergy + amount * reaction.energy).coerceAtLeast(0.001)
        val state = NodeThermodynamics.calculateState(
            node,
            NodeMaterialState(Collections.unmodifiableMap(masses), energy, bubbleVolume, 0.0),
        )
        return DuctNodeState(pos, state.temperature, state.pressure, masses, energy, bubbleVolume)
    }
}
