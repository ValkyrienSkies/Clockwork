package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.block.IBE
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode


class CreativeGeneratorBlock(properties: Properties) : Block(properties), INodeBlock, IBE<CreativeGeneratorBlockEntity> {




    override fun createNode(pos: DuctNodePos, network: DuctNetwork): DuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.PIPE, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        _updateShape(state, direction, neighborState, level, currentPos, neighborPos)
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        _onPlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        _onRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<CreativeGeneratorBlockEntity> {
        return  CreativeGeneratorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CreativeGeneratorBlockEntity> {
        return ClockworkBlockEntities.CREATIVE_GENERATOR.get()
    }



}