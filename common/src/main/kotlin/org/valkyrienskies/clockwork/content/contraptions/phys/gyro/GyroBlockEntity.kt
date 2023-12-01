package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class GyroBlockEntity(typeIn: BlockEntityType<GyroBlockEntity>, pos: BlockPos, state: BlockState) : SmartBlockEntity(typeIn, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {

    }
}