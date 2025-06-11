package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import kotlin.math.abs

class AirCompressorBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {
    var isActivated: Boolean = false
        private set

    val maxGas: Double = 100.0
    val baselineSpeed: Double = 0.1

    var clientParticles: Boolean = false
    var clientSize: Float = 0.0f

    private val airGas = GasTypeRegistry.getGasType("kelvin", "air")

    override fun tick() {
        super.tick()

        if (airGas == null) return ClockworkMod.LOGGER.error("Could not get GasType `kelvin:air`. Is Gas Registry broken?")

        if (level!!.isClientSide) return
        val kelvin = ClockworkMod.getKelvin()
        kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return
        val speed = abs(getSpeed())
        val currentAirVolume = kelvin.getGasMassAt(blockPos.toDuctNodePos(level!!.dimension().location()))[airGas]?: 0.0


        if (speed>0 && currentAirVolume<maxGas) {
            if (!isActivated) {
                isActivated = true
                sendData()
            }

            val deltaVolume = Mth.clamp(maxGas-currentAirVolume,0.0001, baselineSpeed*speed)
            kelvin.modGasMassOfTemperature(getDuctNodePosition(),airGas, deltaVolume, 300.0)
        } else if (isActivated) {
            isActivated = false;
            sendData()
        }
    }


    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        isActivated = tag.getBoolean("isActivated")
        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putBoolean("isActivated",isActivated)
        super.write(tag, clientPacket)
    }


    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        return super<IHeatableBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }


    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }


}