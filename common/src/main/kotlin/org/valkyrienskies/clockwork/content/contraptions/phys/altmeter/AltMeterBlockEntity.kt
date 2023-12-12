package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getShipManagingPos

class AltMeterBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state) {
    internal var triggerHeight: Double = 0.0
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return

        // Copy so nullable checks are automated in if statements
        val triggerHeightCopy = triggerHeight

        val posInWorld = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
        val shipOn = level.getShipManagingPos(blockPos)
        shipOn?.transform?.shipToWorld?.transformPosition(posInWorld)
        val shouldBePowered = posInWorld.y() >= triggerHeightCopy

        val isCurrentlyPowered = blockState.getValue(AltMeterBlock.POWERED)
        if (shouldBePowered != isCurrentlyPowered) {
            if (shouldBePowered) {
                // Same flags as a redstone torch update
                level!!.setBlock(blockPos, blockState.setValue(AltMeterBlock.POWERED, true), 3)
            } else {
                level!!.setBlock(blockPos, blockState.setValue(AltMeterBlock.POWERED, false), 3)
            }
        }
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        val triggerHeightCopy = triggerHeight
        compound.putDouble("triggerHeight", triggerHeightCopy)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        triggerHeight = compound.getDouble("triggerHeight")
    }
}
