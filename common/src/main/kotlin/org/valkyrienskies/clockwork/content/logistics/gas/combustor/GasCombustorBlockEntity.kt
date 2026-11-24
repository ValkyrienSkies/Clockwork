package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.recipe.DefaultKelvinRequirements
import org.valkyrienskies.kelvin.impl.recipe.KelvinReactionDataLoader
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import kotlin.math.abs
import kotlin.math.min

class GasCombustorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable, IThinkable {

    private lateinit var fuelSetting: ScrollOptionBehaviour<Fuel>

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        fuelSetting = object : ScrollOptionBehaviour<Fuel>(
            Fuel::class.java, Component.translatable("vs_clockwork.gas_combustor.fuel"), this, FuelSlot()
        ) {
            override fun write(tag: CompoundTag, clientPacket: Boolean) {
                super.write(tag, clientPacket)
                tag.remove("ScrollValue")
                tag.putInt("FuelMode", value)
            }

            override fun read(tag: CompoundTag, clientPacket: Boolean) {
                val fuel = if (tag.contains("FuelMode")) {
                    tag.getInt("FuelMode").coerceIn(0, Fuel.entries.lastIndex)
                } else {
                    when (tag.getInt("ScrollValue")) {
                        2, 3 -> Fuel.STELLANE.ordinal
                        4, 5, 6 -> Fuel.PHLOGISTON.ordinal
                        else -> Fuel.BOG.ordinal
                    }
                }
                super.read(tag.copy().apply { putInt("ScrollValue", fuel) }, clientPacket)
            }
        }
        fuelSetting.requiresWrench()
        behaviours.add(fuelSetting)
    }

    override fun acceptsThinkable(side: Direction) = side == Direction.UP

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gas_throttle") else null

    private fun throttle(world: ServerLevel): Double {
        val linkedThrottle = DrivingControlLinks.get(world).linkedThrottle(world, blockPos)
        val source = ThinkableConnections.sourceAt(world, blockPos, Direction.UP, cableOnly = linkedThrottle != null)
            ?: return linkedThrottle ?: world.getBestNeighborSignal(blockPos) / 15.0
        val device = source.device ?: return 0.0
        val reading = if (device.thinkableReadings(source.side).any { it.id() == "throttle" }) "throttle" else ""
        return device.thinkableValue(source.side, reading)?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
    }

    private fun ambientAir(world: ServerLevel): AmbientAir? {
        val intake = blockPos.above()
        val position = world.toWorldCoordinates(Vec3.atCenterOf(intake))
        val atmosphere = world.shipObjectWorld.aerodynamicUtils
        val dimension = world.dimensionId
        val pressure = atmosphere.getAirPressureForY(position.y, dimension)
        if (!pressure.isFinite() || pressure <= 0.0) return null
        val outside = BlockPos.containing(position)
        if (!world.hasChunkAt(intake) || !world.getBlockState(intake).isAir ||
            !world.hasChunkAt(outside) || !world.getBlockState(outside).isAir
        ) return AmbientAir(pressure, 0.0, 0.0)

        val density = atmosphere.getAirDensityForY(position.y, dimension)
        val temperature = atmosphere.getAirTemperatureForY(position.y, dimension)
        if (!density.isFinite() || density <= 0.0 || !temperature.isFinite() || temperature <= 0.0)
            return AmbientAir(pressure, 0.0, 0.0)
        val mass = COMPRESSOR_AIR_PER_TICK * (density / SEA_LEVEL_DENSITY).coerceAtMost(1.0)
        return AmbientAir(pressure, mass, temperature)
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null) return
        val throttle = throttle(world)
        if (throttle <= 0.0) return

        val network = ClockworkMod.getKelvin(world)
        val position = getDuctNodePosition()
        val chamber = GasTransfer.port(network, position) ?: return
        if (chamber.pressure >= CHAMBER_TARGET_PRESSURE) return
        val selected = fuelSetting.get()
        val fuel = selected.gas()
        val air = GasTypeRegistry.getGasType("kelvin", "air") ?: return
        val ambient = ambientAir(world)
        var remainingAmbient = ambient?.mass ?: 0.0
        val fuelLimit = throttle * FUEL_PER_TICK
        var remainingFuel = fuelLimit
        var ignition = Double.POSITIVE_INFINITY

        for (mix in selected.mixtures) {
            if (remainingFuel <= GasTransfer.MIN_MASS) break
            val output = GasTransfer.port(network, position) ?: break
            val burn = burn(world, network, output, mix, fuel, air, ambient, remainingFuel, remainingAmbient) ?: continue
            remainingFuel -= burn.fuel
            remainingAmbient -= burn.ambientAir
            ignition = min(ignition, burn.ignition)
        }

        if (remainingFuel == fuelLimit && ignition.isFinite()) {
            val heat = min(STARTER_JOULES, (ignition + 5.0 - chamber.temperature) * chamber.capacity)
            val fraction = chamber.allowedFraction(emptyMap(), heat, CHAMBER_TARGET_PRESSURE, GasTransfer.MAX_TEMPERATURE)
            network.modHeatEnergy(position, heat * fraction)
        }
    }

    private fun burn(
        world: ServerLevel, network: DuctNetwork<*>, output: GasTransfer.Port, mix: Mixture,
        fuel: GasType, air: GasType, ambient: AmbientAir?, fuelLimit: Double, ambientLimit: Double,
    ): Burn? {
        val recipe = KelvinReactionDataLoader.gas_reactions[mix.recipeId] ?: return null
        if (abs(recipe.gasses.values.sum() - recipe.result.values.sum()) > 1e-9) return null
        val fuelPerBatch = recipe.gasses[fuel]?.takeIf { it > 0.0 } ?: return null
        val workingAirPerBatch = mix.workingAirPerFuel * fuelPerBatch
        val airPerBatch = workingAirPerBatch + (recipe.gasses[air] ?: 0.0)
        val ductOxidizers = recipe.gasses.filterKeys { it != fuel && it != air }
        val facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)

        val fuelInlet = GasTransfer.inlet(world, network, blockPos, facing.opposite) ?: return null
        val rightInlet = GasTransfer.inlet(world, network, blockPos, facing.clockWise)
        val oxidizerInlet = if (ductOxidizers.isEmpty()) null else rightInlet ?: return null
        if (fuelInlet.position == oxidizerInlet?.position) return null
        val feeds = buildList {
            add(Feed(fuelInlet, mapOf(fuel to fuelPerBatch)))
            if (oxidizerInlet != null) add(Feed(oxidizerInlet, ductOxidizers))
        }
        var batches = fuelLimit / fuelPerBatch
        for ((port, gases) in feeds) {
            if (port.pressure <= output.pressure) return null
            for ((gas, mass) in gases) batches = min(batches, (port.masses[gas] ?: return null) / mass)
        }

        val airInlets = if (airPerBatch > 0.0) {
            val leftInlet = GasTransfer.inlet(world, network, blockPos, facing.counterClockWise)
            val rightAir = rightInlet.takeIf { oxidizerInlet == null && air in recipe.gasses }
            listOfNotNull(leftInlet, rightAir).distinctBy { it.position }.mapNotNull { port ->
                if (port.position == fuelInlet.position || port.position == oxidizerInlet?.position) return@mapNotNull null
                val mass = port.masses[air] ?: return@mapNotNull null
                val available = when {
                    port.pressure > output.pressure -> mass
                    ambient != null && port.pressure > ambient.pressure &&
                        port.pressure * COMPRESSOR_PRESSURE_RATIO > output.pressure ->
                        mass * (1.0 - ambient.pressure / port.pressure)
                    else -> 0.0
                }
                if (available > GasTransfer.MIN_MASS) AirInlet(port, available) else null
            }
        } else emptyList()
        val ambientMass = if (ambient != null && ambient.pressure * COMPRESSOR_PRESSURE_RATIO > output.pressure)
            ambientLimit else 0.0
        if (airPerBatch > 0.0) batches = min(batches, (airInlets.sumOf { it.mass } + ambientMass) / airPerBatch)
        if (batches * fuelPerBatch <= GasTransfer.MIN_MASS) return null

        val ignition = recipe.requirements[DefaultKelvinRequirements.minTemperature]?.asDouble ?: 0.0
        if (output.temperature < ignition) return Burn(ignition = ignition)

        val state = output.reactionState()
        if (!recipe.requirements.all { (test, value) -> test.apply_requirement(world, state, value) }) return null
        val maxTemperature = recipe.requirements[DefaultKelvinRequirements.maxTemperature]?.asDouble
            ?: GasTransfer.MAX_TEMPERATURE
        val products = recipe.result.mapValues { it.value * batches }.toMutableMap()
        if (workingAirPerBatch > 0.0) products.merge(air, workingAirPerBatch * batches, Double::plus)

        var remainingAir = airPerBatch * batches
        val meteredFeeds = buildList {
            for ((port, gases) in feeds) add(Feed(port, gases.mapValues { it.value * batches }))
            for ((port, available) in airInlets) {
                val mass = min(remainingAir, available)
                if (mass <= 0.0) continue
                add(Feed(port, mapOf(air to mass)))
                remainingAir -= mass
            }
        }
        val ambientEnergy = if (ambient == null) 0.0
            else GasPhysics.mixtureCapacity(mapOf(air to remainingAir)) * ambient.temperature
        val energy = recipe.energy * batches + ambientEnergy +
            meteredFeeds.sumOf { GasPhysics.mixtureCapacity(it.gases) * it.port.temperature }
        val fraction = output.allowedFraction(
            products, energy, CHAMBER_PRESSURE_LIMIT, maxTemperature
        )
        val fuelBurned = fuelPerBatch * batches * fraction
        if (fuelBurned <= GasTransfer.MIN_MASS) return null

        for ((port, gases) in meteredFeeds) GasTransfer.remove(network, port, gases, fraction)
        GasTransfer.add(network, output, products, energy, fraction)
        return Burn(fuelBurned, remainingAir * fraction)
    }

    private data class AmbientAir(val pressure: Double, val mass: Double, val temperature: Double)
    private data class AirInlet(val port: GasTransfer.Port, val mass: Double)
    private data class Feed(val port: GasTransfer.Port, val gases: Map<GasType, Double>)
    private data class Burn(val fuel: Double = 0.0, val ambientAir: Double = 0.0, val ignition: Double = Double.POSITIVE_INFINITY)

    private class Mixture(recipe: String, val workingAirPerFuel: Double) {
        val recipeId = ClockworkMod.asResource(recipe)
    }

    private enum class Fuel(val gas: () -> GasType, vararg val mixtures: Mixture) : INamedIconOptions {
        BOG({ ClockworkGasses.METHANE }, Mixture("bog_combustion_boosted", 118.75), Mixture("bog_combustion", 57.0)),
        STELLANE({ ClockworkGasses.HYDROGEN }, Mixture("stellane_combustion_boosted", 0.0), Mixture("stellane_combustion", 0.0)),
        PHLOGISTON({ ClockworkGasses.PHLOGISTON }, Mixture("phlogiston_ozonite", 258.5),
            Mixture("phlogiston_air", 193.5), Mixture("phlogiston_passive", 130.25));

        override fun getIcon() = AllIcons.I_CONFIG_LOCKED
        override fun getTranslationKey() = "vs_clockwork.gas_combustor.fuel.${name.lowercase()}"
    }

    private class FuelSlot : ValueBoxTransform.Sided() {
        override fun isSideActive(state: BlockState, side: Direction) = side == Direction.UP

        override fun getSouthLocation() = Vec3(0.5, 0.5, 0.99)
    }

    companion object {
        private const val FUEL_PER_TICK = 0.008 / 20.0
        private const val CHAMBER_TARGET_PRESSURE = 300000.0
        private const val CHAMBER_PRESSURE_LIMIT = 400000.0
        private const val STARTER_JOULES = 5000.0
        private const val COMPRESSOR_AIR_PER_TICK = 0.006
        private const val COMPRESSOR_PRESSURE_RATIO = 3.0
        private const val SEA_LEVEL_DENSITY = 1.225
    }
}
