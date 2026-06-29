package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sign

class SterlingEngineBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeKineticBlockEntity(type, pos, state), IConnectable {

    val heatLoss get() = totalEfficiency * 5000.0

    var totalEfficiency = 0.0f
    var reActivateSource = false

    override fun lazyTick() {
        super.lazyTick()

        val level = level ?: return
        if (level.isClientSide) return

        val facing = blockState.getValue(BlockStateProperties.FACING)
        updateConnection(level, blockPos, facing)
        updateConnection(level, blockPos, facing.opposite)

        val efficiency = GasEngineBlockEntity.tempToEfficiency(ClockworkMod.getKelvin(level).getTemperatureAt(getDuctNodePosition()))
        if (efficiency != totalEfficiency) {
            totalEfficiency = efficiency
            updateGeneratedRotation()
        }
    }

    override fun tick() {
        super.tick()

        val level = level ?: return
        if (level.isClientSide) return

        ClockworkMod.getKelvin(level).modHeatEnergy(getDuctNodePosition(), -heatLoss * totalEfficiency)

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
        val capacity = getEngineEfficiency() * BASE_STRESS_CAPACITY / getSpeedModifier(getEngineEfficiency())
        lastCapacityProvided = capacity
        return capacity
    }

    fun getEngineEfficiency(): Float {
        return min(totalEfficiency, 1f).coerceAtLeast(0f)
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
        val extraSpeed = if (efficiency >= 1f) 3 else min(2.0, floor(efficiency * 4.0)).toInt()
        return 1 + extraSpeed
    }

    override fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putFloat("TotalEfficiency", totalEfficiency)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        totalEfficiency = tag.getFloat("TotalEfficiency")
        super.read(tag, clientPacket)
    }

    companion object {
        const val BASE_STRESS_CAPACITY = 1024f
    }
}
