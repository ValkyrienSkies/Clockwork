package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.AllBlocks
import com.simibubi.create.api.stress.BlockStressValues
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.kelvin.KelvinParticleHelper
import kotlin.math.min

class GasEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    val heatLoss get() = totalEfficiency * ClockworkConfig.SERVER.gasEngine.gasEngineMaxHeatLoss

    var attachedEngines = 0
    var totalEfficiency = 0.0f
    var temperatureEfficiency = 0.0f
    var flowEfficiency = 0.0f
    var flowRate = 0.0

    override fun lazyTick() {
        super.lazyTick()

        if (level!!.isClientSide) return
        val components = GasEngineLogic.calculateEfficiencyComponents(
            level!!,
            blockPos,
            getDuctNodePosition(),
            blockState.getValue(BlockStateProperties.AXIS),
            ClockworkConfig.SERVER.gasEngine.gasEngineFlowForFullEfficiency,
            ClockworkConfig.SERVER.gasEngine.gasEngineMinimumFlowRate,
            ClockworkConfig.SERVER.gasEngine.gasEngineFlowRateIncrement,
            ClockworkConfig.SERVER.gasEngine.gasEngineTemperatureIncrement
        )
        val nextEfficiency = GasEngineLogic.smoothEfficiency(
            totalEfficiency,
            components.totalEfficiency,
            ClockworkConfig.SERVER.gasEngine.gasEngineEfficiencySmoothing
        )

        if (
            nextEfficiency != totalEfficiency ||
            components.temperatureEfficiency != temperatureEfficiency ||
            components.flowEfficiency != flowEfficiency ||
            components.flowRate != flowRate
        ) {
            totalEfficiency = nextEfficiency
            temperatureEfficiency = components.temperatureEfficiency
            flowEfficiency = components.flowEfficiency
            flowRate = components.flowRate
            sendData()
        }
    }

    override fun tick() {
        if (level!!.isClientSide) return super.tick()
        if (attachedEngines > 0 && heatLoss > 0.0) {
            ClockworkMod.getKelvin(level).modHeatEnergy(getDuctNodePosition(), -heatLoss)
        }
        super.tick()
    }

    fun getEngineEfficiency(): Float {
        val efficiency = if (attachedEngines == 0) 0f else min(totalEfficiency / attachedEngines, 1f)
        return GasEngineLogic.roundEfficiencyForWholeStress(
            efficiency,
            BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())
        )
    }

    //todo: this doesnt work on dedicated servers you moron
    fun spawnParticles(level: Level, pos: Vector3dc, speed: Vector3dc) {
        KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, getDuctNodePosition(), pos, speed)
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        EngineGoggleTooltip.addGasEngineTooltip(tooltip as MutableList<Component>, temperatureEfficiency, flowEfficiency)
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putInt("AttachedEngines", attachedEngines)
        tag.putFloat("TotalEfficiency", totalEfficiency)
        tag.putFloat("TemperatureEfficiency", temperatureEfficiency)
        tag.putFloat("FlowEfficiency", flowEfficiency)
        tag.putDouble("FlowRate", flowRate)

        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        attachedEngines = tag.getInt("AttachedEngines")
        totalEfficiency = tag.getFloat("TotalEfficiency")
        temperatureEfficiency = tag.getFloat("TemperatureEfficiency")
        flowEfficiency = tag.getFloat("FlowEfficiency")
        flowRate = tag.getDouble("FlowRate")

        super.read(tag, clientPacket)
    }

    companion object {
        fun tempToEfficiency(temperature: Double): Float {
            return GasEngineLogic.tempToEfficiency(temperature, ClockworkConfig.SERVER.gasEngine.gasEngineTemperatureIncrement)
        }
    }
}
