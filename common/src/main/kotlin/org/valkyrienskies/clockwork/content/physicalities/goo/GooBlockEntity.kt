package org.valkyrienskies.clockwork.content.physicalities.goo

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class GooBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}
}