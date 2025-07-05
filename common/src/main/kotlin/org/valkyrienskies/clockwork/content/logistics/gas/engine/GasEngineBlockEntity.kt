package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.kelvin.KelvinMod

class GasEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    val heatLoss get() = totalEfficiency * 1000.0

    var attachedEngines = 0
    var totalEfficiency = 0.0f


    override fun tick() {
        if (level?.isClientSide != false) return

        val network = KelvinMod.getKelvin()
        val temperature = network.getTemperatureAt(getDuctNodePosition())
        totalEfficiency = tempToEfficiency(temperature)

        network.modHeatEnergy(getDuctNodePosition(), -heatLoss)
        super.tick()
    }

    fun getEngineEfficiency(): Float {
        val value = if (attachedEngines == 0) 0f else totalEfficiency / attachedEngines
        println(attachedEngines)
        return value
    }

    fun spawnParticles(level: ClientLevel, pos: Vector3dc, speed: Vector3dc) {
        KelvinParticleHelper.spawnParticleWithRatio(level, getDuctNodePosition(), pos, speed)
    }

    companion object {
        fun tempToEfficiency(temperature: Double): Float {
            return when {
                temperature < 350 -> 0.0f
                temperature < 700 -> 1.0f
                temperature < 1050 -> 2.0f
                temperature < 1400 -> 3.0f
                temperature < 1750 -> 4.0f
                temperature < 2100 -> 5.0f
                else -> 5.0f
            }
        }
    }
}