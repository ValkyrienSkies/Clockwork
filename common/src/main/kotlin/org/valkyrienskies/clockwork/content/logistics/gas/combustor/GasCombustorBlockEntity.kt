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
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.recipe.DefaultKelvinRequirements
import org.valkyrienskies.kelvin.impl.recipe.KelvinReactionDataLoader
import org.valkyrienskies.kelvin.util.GasPhysics
import kotlin.math.min

class GasCombustorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable, IThinkable {

    private lateinit var mode: ScrollOptionBehaviour<Mode>

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        mode = object : ScrollOptionBehaviour<Mode>(
            Mode::class.java, Component.translatable("vs_clockwork.gas_combustor.mode"), this, ModeSlot()
        ) {
            override fun read(tag: CompoundTag, clientPacket: Boolean) {
                super.read(tag, clientPacket)
                value = value.coerceIn(0, Mode.entries.lastIndex)
            }
        }
        mode.requiresWrench()
        behaviours.add(mode)
    }

    override fun acceptsThinkable(side: Direction) = side == Direction.UP

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gas_throttle") else null

    private fun throttle(world: ServerLevel): Double {
        val source = ThinkableConnections.inputSource(world, blockPos, Direction.UP)
            ?: return world.getBestNeighborSignal(blockPos) / 15.0
        val device = source.device ?: return 0.0
        val reading = if (device.thinkableReadings(source.side).any { it.id() == "throttle" }) "throttle" else ""
        return device.thinkableValue(source.side, reading)?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null) return
        val throttle = throttle(world)
        if (throttle <= 0.0) return

        val selected = mode.get()
        val recipe = KelvinReactionDataLoader.gas_reactions[selected.recipeId] ?: return
        val fuel = selected.fuel()
        val fuelPerBatch = recipe.gasses[fuel]?.takeIf { it > 0.0 } ?: return
        val oxidizers = recipe.gasses.filterKeys { it != fuel }
        val facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val network = ClockworkMod.getKelvin(world)
        val output = GasTransfer.port(network, getDuctNodePosition()) ?: return
        val fuelInlet = GasTransfer.inlet(world, network, blockPos, facing.opposite) ?: return
        if (fuelInlet.pressure <= output.pressure) return
        var batches = min(throttle * FUEL_PER_TICK, fuelInlet.masses[fuel] ?: return) / fuelPerBatch
        val oxidizerInlet = if (oxidizers.isEmpty()) null else {
            val inlet = GasTransfer.inlet(world, network, blockPos, facing.clockWise) ?: return
            if (inlet.position == fuelInlet.position || inlet.pressure <= output.pressure) return
            for ((gas, mass) in oxidizers) batches = min(batches, (inlet.masses[gas] ?: return) / mass)
            inlet
        }
        if (batches * fuelPerBatch <= GasTransfer.MIN_MASS || output.pressure >= CHAMBER_PRESSURE) return

        val ignition = recipe.requirements[DefaultKelvinRequirements.minTemperature]?.asDouble ?: 0.0
        if (output.temperature < ignition) {
            val heat = min(STARTER_JOULES, (ignition + 5.0 - output.temperature) * output.capacity)
            val fraction = output.allowedFraction(emptyMap(), heat, CHAMBER_PRESSURE, GasTransfer.MAX_TEMPERATURE)
            network.modHeatEnergy(output.position, heat * fraction)
            return
        }

        val state = output.reactionState()
        if (!recipe.requirements.all { (test, value) -> test.apply_requirement(world, state, value) }) return
        val maxTemperature = recipe.requirements[DefaultKelvinRequirements.maxTemperature]?.asDouble
            ?: GasTransfer.MAX_TEMPERATURE
        val fuelMass = mapOf(fuel to fuelPerBatch * batches)
        val oxidizerMasses = oxidizers.mapValues { it.value * batches }
        val products = recipe.result.mapValues { it.value * batches }
        val energy = recipe.energy * batches + GasPhysics.mixtureCapacity(fuelMass) * fuelInlet.temperature +
            GasPhysics.mixtureCapacity(oxidizerMasses) * (oxidizerInlet?.temperature ?: 0.0)
        val fraction = output.allowedFraction(products, energy, CHAMBER_PRESSURE, maxTemperature)
        if (fuelPerBatch * batches * fraction <= GasTransfer.MIN_MASS) return

        GasTransfer.remove(network, fuelInlet, fuelMass, fraction)
        if (oxidizerInlet != null) GasTransfer.remove(network, oxidizerInlet, oxidizerMasses, fraction)
        GasTransfer.add(network, output, products, energy, fraction)
    }

    private enum class Mode(recipe: String, val fuel: () -> GasType) : INamedIconOptions {
        BOG_AIR("bog_combustion", { ClockworkGasses.METHANE }),
        BOG_OZONITE("bog_combustion_boosted", { ClockworkGasses.METHANE }),
        STELLANE_AIR("stellane_combustion", { ClockworkGasses.HYDROGEN }),
        STELLANE_OZONITE("stellane_combustion_boosted", { ClockworkGasses.HYDROGEN }),
        PHLOGISTON_SOLO("phlogiston_passive", { ClockworkGasses.PHLOGISTON }),
        PHLOGISTON_AIR("phlogiston_air", { ClockworkGasses.PHLOGISTON }),
        PHLOGISTON_OZONITE("phlogiston_ozonite", { ClockworkGasses.PHLOGISTON });

        val recipeId = ClockworkMod.asResource(recipe)

        override fun getIcon() = AllIcons.I_CONFIG_LOCKED
        override fun getTranslationKey() = "vs_clockwork.gas_combustor.mode.${name.lowercase()}"
    }

    private class ModeSlot : ValueBoxTransform.Sided() {
        override fun isSideActive(state: BlockState, side: Direction): Boolean =
            side == state.getValue(BlockStateProperties.HORIZONTAL_FACING).counterClockWise

        override fun getSouthLocation() = Vec3(0.5, 0.5, 0.91)
    }

    companion object {
        private const val FUEL_PER_TICK = 0.008 / 20.0
        private const val CHAMBER_PRESSURE = 300000.0
        private const val STARTER_JOULES = 5000.0
    }
}
