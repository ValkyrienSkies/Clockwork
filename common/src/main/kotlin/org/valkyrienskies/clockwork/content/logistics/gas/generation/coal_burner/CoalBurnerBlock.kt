package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.google.common.collect.ImmutableMap
import com.simibubi.create.foundation.block.IBE
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.FurnaceBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock.Companion.GAS_HEAT_LEVEL
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.DOWN_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.EAST_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.NORTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.SOUTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.UP_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.WEST_CONNECTION
import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.function.Function


class CoalBurnerBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IHeatableBlock, INodeBlock, IBE<CoalBurnerBlockEntity> {



    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false))
    }


    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as CoalBurnerBlockEntity? ?: return  InteractionResult.PASS
        val item = player.getItemInHand(hand)


        if (FuelRegistry.get(item)>0 && !player.isShiftKeyDown) {
            be.fuelTicks+=FuelRegistry.get(item)*item.count
            player.setItemInHand(hand, ItemStack.EMPTY)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    override fun createNode(pos: DuctNodePos, network: DuctNetwork): PipeDuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.COAL_BURNER, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }


    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (self.distSqr(other) > 1.0) return false
        val selfState = level.getBlockState(self)
        val otherState = level.getBlockState(other)

        if (otherState.block !is IDuct) return false

        return true
    }




    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        _onPlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        _onRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<CoalBurnerBlockEntity> {
        return  CoalBurnerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CoalBurnerBlockEntity> {
        return ClockworkBlockEntities.COAL_BURNER.get()
    }


    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, LIT)


        super.createBlockStateDefinition(builder)
    }


    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val level: Level = ctx.level
        val pos: BlockPos = ctx.clickedPos
        return this.defaultBlockState().setValue(
            FACING, ctx .horizontalDirection
                .opposite
        )
    }

    companion object {
        val LIT = BlockStateProperties.LIT;
    }
}