package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs

class AirCompressorBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {
    var isOn: Boolean = false

    val maxGas: Double = 100.0
    val baselineSpeed: Double = 0.1

    var clientParticles: Boolean = false
    var clientSize: Float = 0.0f

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return
        val node = ClockworkMod.getKelvin().getNodeAt(blockPos.toJOMLD()) ?: return
        val speed = abs(getSpeed())
        val currentAirVolume = node.network.getGasVolumesAt(blockPos.toJOMLD())[GasType.AIR]?: 0.0


        if (speed>0 && currentAirVolume<maxGas) {
            if (!isOn) syncOn(true)
            isOn = true

            val deltaVolume = Mth.clamp(maxGas-currentAirVolume,0.0001, baselineSpeed*speed)
            node.network.modGasVolumeOfTemperature(getDuctNodePosition(),GasType.AIR, deltaVolume, 300.0)
        } else {
            if (isOn) syncOn(false)
            isOn = false
        }
    }

    fun syncOn(newIsOn: Boolean) {
        ClockworkPackets.sendToNear(level!!,blockPos,100,AirCompressorPacket(newIsOn,blockPos))
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        return super<IHeatableBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }


    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
    }


}