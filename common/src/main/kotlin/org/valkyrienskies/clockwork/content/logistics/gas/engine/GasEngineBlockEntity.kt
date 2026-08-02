package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.AllBlocks
import com.simibubi.create.api.stress.BlockStressValues
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.platform.Platform
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.client.Minecraft
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
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.kelvin.KelvinParticleHelper
import kotlin.math.ceil
import kotlin.math.floor

class GasEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    val heatLoss get() = GasEngineLogic.totalEfficiencyFraction(totalEfficiency) *
        ClockworkConfig.SERVER.gasEngine.gasEngineMaxHeatLoss

    var attachedEngines = 0
    var totalEfficiency = 0.0f
    var temperatureEfficiency = 0.0f
    var flowEfficiency = 0.0f
    var flowRate = 0.0
    var rawFlowRate = 0.0

    override fun lazyTick() {
        super.lazyTick()

        if (level!!.isClientSide) return
        val components = GasEngineLogic.calculateEfficiencyComponents(
            level!!,
            blockPos,
            getDuctNodePosition(),
            blockState.getValue(BlockStateProperties.AXIS),
            ClockworkConfig.SERVER.gasEngine.gasEngineFlowRateIncrement,
            ClockworkConfig.SERVER.gasEngine.gasEngineTemperatureIncrement
        )

        if (components.totalEfficiency != totalEfficiency ||
            components.temperatureEfficiency != temperatureEfficiency ||
            components.flowEfficiency != flowEfficiency ||
            components.flowRate != flowRate ||
            components.rawFlowRate != rawFlowRate) {

            totalEfficiency = components.totalEfficiency
            temperatureEfficiency = components.temperatureEfficiency
            flowEfficiency = components.flowEfficiency
            flowRate = components.flowRate
            rawFlowRate = components.rawFlowRate
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
        val efficiency = GasEngineLogic.efficiencyPerAttachedEngine(totalEfficiency, attachedEngines)
        return GasEngineLogic.roundEfficiencyForWholeStress(
            efficiency,
            BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())
        )
    }

    private fun getTotalStressCapacity(): Double {
        val fullEngineCapacity = 16.0 * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())
        return floor(totalEfficiency.coerceAtLeast(0f) * fullEngineCapacity)
    }

    private fun getRequiredEngineCount(stressCapacity: Double): Int {
        val fullEngineCapacity = 16.0 * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())
        if (stressCapacity <= 0.0 || fullEngineCapacity <= 0.0) return 0
        return ceil(stressCapacity / fullEngineCapacity).toInt()
    }

    //todo: this doesnt work on dedicated servers you moron
    fun spawnParticles(level: Level, pos: Vector3dc, speed: Vector3dc) {
        KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, getDuctNodePosition(), pos, speed)
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        val stressCapacity = getTotalStressCapacity()
        EngineGoggleTooltip.addGasEngineTooltip(
            tooltip as MutableList<Component>,
            temperatureEfficiency,
            flowEfficiency,
            isPlayerSneaking,
            getTooltipTemperature(),
            ClockworkConfig.SERVER.gasEngine.gasEngineTemperatureIncrement,
            rawFlowRate,
            ClockworkConfig.SERVER.gasEngine.gasEngineFlowRateIncrement,
            stressCapacity,
            getRequiredEngineCount(stressCapacity)
        )
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }

    private fun getTooltipTemperature(): Double {
        val beLevel = level ?: return 0.0
        val kelvin = if (beLevel is PonderLevel) ClockworkMod.getKelvin(beLevel)
            else if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin()
            else ClockworkModClient.getKelvin()
        return kelvin.getTemperatureAt(getDuctNodePosition())
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putInt("AttachedEngines", attachedEngines)
        tag.putFloat("TotalEfficiency", totalEfficiency)
        tag.putFloat("TemperatureEfficiency", temperatureEfficiency)
        tag.putFloat("FlowEfficiency", flowEfficiency)
        tag.putDouble("FlowRate", flowRate)
        tag.putDouble("RawFlowRate", rawFlowRate)

        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        attachedEngines = tag.getInt("AttachedEngines")
        totalEfficiency = tag.getFloat("TotalEfficiency")
        temperatureEfficiency = tag.getFloat("TemperatureEfficiency")
        flowEfficiency = tag.getFloat("FlowEfficiency")
        flowRate = tag.getDouble("FlowRate")
        rawFlowRate = tag.getDouble("RawFlowRate")

        super.read(tag, clientPacket)
    }

    companion object {
        fun tempToEfficiency(temperature: Double): Float {
            return GasEngineLogic.tempToEfficiency(temperature, ClockworkConfig.SERVER.gasEngine.gasEngineTemperatureIncrement)
        }
    }
}
