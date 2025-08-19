package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.KNodeBlockEntity

class GasBacktankBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {



    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }



    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
    }

}