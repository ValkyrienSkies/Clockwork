package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toJOMLD

class AirCompressorBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {
    var isOn: Boolean = false

    val maxGas: Double = 100.0
    val baselineSpeed: Double = 1.0

    var clientParticles: Boolean = false

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return
        val node = ClockworkMod.getKelvin().getNodeAt(blockPos.toJOMLD()) ?: return
        val speed = getSpeed()
        val currentAirVolume = node.network.getGasVolumesAt(blockPos.toJOMLD())[GasType.AIR]?: 0.0

        print(speed)
        print(" ")
        print(node.network.getGasVolumesAt(blockPos.toJOMLD())[GasType.AIR])
        if (speed>0 && currentAirVolume<maxGas) {
            isOn = true

            val deltaVolume = Mth.clamp(maxGas-currentAirVolume,0.0001, baselineSpeed*speed)
            node.network.addGasVolumeOfTemperature(getDuctNodePosition(),GasType.AIR, deltaVolume, 300.0)
        } else {
            isOn = false
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        return super<IHeatableBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }


    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
    }


}