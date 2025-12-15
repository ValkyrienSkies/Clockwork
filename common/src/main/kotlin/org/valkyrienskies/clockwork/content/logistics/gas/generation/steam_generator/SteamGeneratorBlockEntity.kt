package org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math.clamp
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import java.lang.ref.WeakReference
import kotlin.math.max

class SteamGeneratorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    var source = WeakReference<FluidTankBlockEntity?>(null)

    val maxMass = 0.01
    val maxTemperature = 1400.0
    val steamGas = GasTypeRegistry.getGasType("vs_clockwork", "steam")

    fun getTank(): FluidTankBlockEntity? {
        var tank: FluidTankBlockEntity? = source.get()
        if (tank == null || tank.isRemoved()) {
            if (tank != null) source = WeakReference<FluidTankBlockEntity?>(null)
            val facing = SteamGeneratorBlock.getFacing(getBlockState())
            val be = level!!.getBlockEntity(worldPosition.relative(facing.getOpposite()))
            if (be is FluidTankBlockEntity) source = WeakReference<FluidTankBlockEntity?>(be.also { tank = it })
        }
        if (tank == null) return null
        return tank.getControllerBE()
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) { return }

    override fun tick() {
        super.tick()
        if (level?.isClientSide != false) return
        if (steamGas == null) return ClockworkMod.LOGGER.error("SteamGeneratorBlockEntity can't get GasType 'vs_clockwork:steam'. Did something go wrong?")

        val tank = getTank() ?: return

        val efficiency = clamp(tank.boiler.getEngineEfficiency(tank.totalTankSize), 0f, 1f)
        tank.boiler.activeHeat

        if (efficiency == 0f) return

        val network = ClockworkMod.getKelvin()

        val mass = maxMass * efficiency

        // TODO: Redo temperature calc. A max boiler producing 1800°C steam is kind of stupid.
        val temperature = maxTemperature * max(tank.boiler.activeHeat.toDouble(), 1.0) / 8.0

        network.addGasAtTemperature(getDuctNodePosition(), steamGas, mass, temperature)
    }
}
