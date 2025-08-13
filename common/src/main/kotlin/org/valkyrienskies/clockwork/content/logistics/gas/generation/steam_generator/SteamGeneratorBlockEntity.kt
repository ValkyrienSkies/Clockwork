package org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.util.KNodeBlockEntity

class SteamGeneratorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {



    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) { return }

}