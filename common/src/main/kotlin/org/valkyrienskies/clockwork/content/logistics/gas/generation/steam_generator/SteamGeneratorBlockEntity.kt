package org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math.clamp
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.pow

class SteamGeneratorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    var source = WeakReference<FluidTankBlockEntity?>(null)

    val maxMass = 0.1
    val maxPressurePerLevel = 17000000/8.0
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

        if (efficiency == 0f) return
        if (tank.boiler.activeHeat == 0 && !tank.boiler.passiveHeat) return

        val network = ClockworkMod.getKelvin()

        val mass = maxMass * efficiency

        val temperature = 80*(max(0.25, tank.boiler.activeHeat.toDouble()) * 100).pow(0.34)

        if (network.getPressureAt(getDuctNodePosition()) > maxPressurePerLevel * max(1, tank.boiler.activeHeat)) return

        network.addGasAtTemperature(getDuctNodePosition(), steamGas, mass, temperature)
    }
}
