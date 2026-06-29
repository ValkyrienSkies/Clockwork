package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

class SterlingEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeKineticBlockEntity(type, pos, state), IConnectable {

    val heatLoss get() = totalEfficiency * ClockworkConfig.SERVER.gasEngine.sterlingEngineMaxHeatLoss

    var totalEfficiency = 0.0f
    var temperatureEfficiency = 0.0f
    var reActivateSource = false
    private var lastFacing = state.getValue(BlockStateProperties.FACING)

    override fun lazyTick() {
        super.lazyTick()

        val level = level ?: return
        if (level.isClientSide) return

        val facing = blockState.getValue(BlockStateProperties.FACING)
        updateConnection(level, blockPos, facing)
        updateConnection(level, blockPos, facing.opposite)

        val targetEfficiency = GasEngineLogic.calculateTemperatureEfficiency(
            level,
            getDuctNodePosition(),
            ClockworkConfig.SERVER.gasEngine.sterlingEngineTemperatureIncrement
        )
        val nextEfficiency = GasEngineLogic.smoothEfficiency(
            totalEfficiency,
            targetEfficiency,
            ClockworkConfig.SERVER.gasEngine.sterlingEngineEfficiencySmoothing
        )
        val facingChanged = facing != lastFacing
        if (facingChanged) lastFacing = facing

        if (nextEfficiency != totalEfficiency || targetEfficiency != temperatureEfficiency || facingChanged) {
            totalEfficiency = nextEfficiency
            temperatureEfficiency = targetEfficiency
            updateGeneratedRotation()
        }
    }

    override fun tick() {
        super.tick()

        val level = level ?: return
        if (level.isClientSide) return

        if (heatLoss > 0.0) {
            ClockworkMod.getKelvin(level).modHeatEnergy(getDuctNodePosition(), -heatLoss)
        }

        val facing = blockState.getValue(BlockStateProperties.FACING)
        if (facing != lastFacing) {
            lastFacing = facing
            updateGeneratedRotation()
        } else if (!hasSource() && speed == 0f && getGeneratedSpeed() != 0f) {
            updateGeneratedRotation()
        }

        if (reActivateSource) {
            updateGeneratedRotation()
            reActivateSource = false
        }
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehavioursDeferred(behaviours)
    }

    override fun removeSource() {
        if (hasSource() && isSource()) reActivateSource = true
        super.removeSource()
    }

    override fun setSource(source: BlockPos) {
        super.setSource(source)

        val sourceBE = level?.getBlockEntity(source) as? KineticBlockEntity ?: return
        if (reActivateSource && abs(sourceBE.getSpeed()) >= abs(getGeneratedSpeed())) {
            reActivateSource = false
        }
    }

    override fun getGeneratedSpeed(): Float {
        val efficiency = getEngineEfficiency()
        if (efficiency <= 0) return 0f

        val facing = blockState.getValue(BlockStateProperties.FACING)
        return KineticBlockEntity.convertToDirection(1f, facing) * 16f * getSpeedModifier(efficiency)
    }

    override fun calculateAddedStressCapacity(): Float {
        val efficiency = getEngineEfficiency()
        val generatedSpeed = 16.0 * getSpeedModifier(efficiency)
        val wholeStress = floor(efficiency * ClockworkConfig.SERVER.gasEngine.sterlingEngineStressCapacity * 16.0)
        val capacity = if (generatedSpeed <= 0.0) 0f else (wholeStress / generatedSpeed).toFloat()
        lastCapacityProvided = capacity
        return capacity
    }

    fun getEngineEfficiency(): Float {
        return totalEfficiency.coerceIn(0f, 1f)
    }

    fun updateGeneratedRotation() {
        val generatedSpeed = getGeneratedSpeed()
        val previousSpeed = speed

        val level = level ?: return
        if (level.isClientSide) return

        if (previousSpeed != generatedSpeed) {
            if (!hasSource()) {
                val levelBefore = SpeedLevel.of(speed)
                val levelAfter = SpeedLevel.of(generatedSpeed)
                if (levelBefore != levelAfter) effects.queueRotationIndicators()
            }

            applyNewSpeed(previousSpeed, generatedSpeed)
        }

        if (hasNetwork() && generatedSpeed != 0f) {
            val network = getOrCreateNetwork()
            network.updateCapacityFor(this, calculateAddedStressCapacity())
            network.updateStressFor(this, calculateStressApplied())
            network.updateStress()
        }

        onSpeedChanged(previousSpeed)
        sendData()
    }

    private fun applyNewSpeed(previousSpeed: Float, generatedSpeed: Float) {
        if (generatedSpeed == 0f) {
            if (hasSource()) {
                getOrCreateNetwork().updateCapacityFor(this, 0f)
                getOrCreateNetwork().updateStressFor(this, calculateStressApplied())
                return
            }
            detachKinetics()
            speed = 0f
            setNetwork(null)
            return
        }

        if (previousSpeed == 0f) {
            speed = generatedSpeed
            setNetwork(createNetworkId())
            attachKinetics()
            return
        }

        if (hasSource()) {
            if (abs(previousSpeed) >= abs(generatedSpeed)) {
                if (sign(previousSpeed) != sign(generatedSpeed)) level?.destroyBlock(blockPos, true)
                return
            }

            detachKinetics()
            speed = generatedSpeed
            source = null
            setNetwork(createNetworkId())
            attachKinetics()
            return
        }

        detachKinetics()
        speed = generatedSpeed
        attachKinetics()
    }

    private fun createNetworkId(): Long {
        return blockPos.asLong()
    }

    private fun getSpeedModifier(efficiency: Float): Int {
        return GasEngineLogic.getSpeedModifier(efficiency)
    }

    override fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putFloat("TotalEfficiency", totalEfficiency)
        tag.putFloat("TemperatureEfficiency", temperatureEfficiency)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        totalEfficiency = tag.getFloat("TotalEfficiency")
        temperatureEfficiency = tag.getFloat("TemperatureEfficiency")
        super.read(tag, clientPacket)
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        EngineGoggleTooltip.addSterlingEngineTooltip(tooltip as MutableList<Component>, temperatureEfficiency)
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}
