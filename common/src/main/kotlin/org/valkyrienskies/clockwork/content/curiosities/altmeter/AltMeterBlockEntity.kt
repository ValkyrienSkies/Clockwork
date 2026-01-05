package org.valkyrienskies.clockwork.content.curiosities.altmeter

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.absoluteValue

class AltMeterBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state) {
    internal var triggerHeight: Double = 0.0
    internal var signalStrength = 0
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    fun getSignalPower(): Int {
        return signalStrength
    }

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return

        // Copy so nullable checks are automated in if statements
        val triggerHeightCopy = triggerHeight

        val posInWorld = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
        val shipOn = level.getShipManagingPos(blockPos)

        shipOn?.transform?.shipToWorld?.transformPosition(posInWorld)
        val distance = posInWorld.y - triggerHeightCopy

        signalStrength = if (distance.absoluteValue < 15) (15 - distance.absoluteValue.toInt()).coerceIn(0, 15) else 0
        val currentPower = blockState.getValue(AltMeterBlock.POWER)

        if (currentPower != signalStrength) {
            level!!.setBlock(blockPos, blockState
                .setValue(AltMeterBlock.POWER, signalStrength)
                .setValue(AltMeterBlock.POWERED, signalStrength > 0), 3)
        }
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        val triggerHeightCopy = triggerHeight
        compound.putDouble(ClockworkConstants.Nbt.TRIGGER_HEIGHT, triggerHeightCopy)
        compound.putInt("Signal Strength", signalStrength)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        triggerHeight = compound.getDouble(ClockworkConstants.Nbt.TRIGGER_HEIGHT)
        signalStrength = compound.getInt("Signal Strength")
    }
}
