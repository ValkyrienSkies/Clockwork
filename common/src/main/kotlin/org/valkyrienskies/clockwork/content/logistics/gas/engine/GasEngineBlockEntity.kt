package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNetwork

class GasEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    val heatLoss get() = totalEfficiency * 100.0

    var attachedEngines = 0
    var totalEfficiency = 0.0f


    override fun tick() {
        if (level!!.isClientSide) return super.tick()

        val network = KelvinMod.getKelvin()
        val temperature = network.getTemperatureAt(getDuctNodePosition())
        totalEfficiency = tempToEfficiency(temperature)

        network.modHeatEnergy(getDuctNodePosition(), -heatLoss*totalEfficiency)
        super.tick()
    }

    fun getEngineEfficiency(): Float {
        return if (attachedEngines == 0) 0f else totalEfficiency / attachedEngines
    }

    fun spawnParticles(level: ClientLevel, pos: Vector3dc, speed: Vector3dc) {
        KelvinParticleHelper.spawnParticleWithRatio(level, getDuctNodePosition(), pos, speed)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putInt("AttachedEngines", attachedEngines)
        tag.putFloat("TotalEfficiency",totalEfficiency)

        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        attachedEngines = tag.getInt("AttachedEngines")
        totalEfficiency = tag.getFloat("TotalEfficiency")

        super.read(tag, clientPacket)
    }

    companion object {
        fun tempToEfficiency(temperature: Double): Float {
            return 0.5f * when {
                temperature < 350 -> 0.0f
                temperature < 700 -> 1.0f
                temperature < 1050 -> 2.0f
                temperature < 1400 -> 3.0f
                temperature < 1750 -> 4.0f
                temperature < 2100 -> 5.0f
                else -> 6.0f
            }
        }
    }
}