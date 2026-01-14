package org.valkyrienskies.clockwork.content.logistics.gas.duct_bearing;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.KNodeBlockEntity

class DuctBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState)
    : KNodeBlockEntity(type, pos, state) {

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) {}


}
