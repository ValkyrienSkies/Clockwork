package org.valkyrienskies.clockwork.content.logistics.gas.gas_crafter

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.KNodeBlockEntity

class GasCrafterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {


    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) { }
}