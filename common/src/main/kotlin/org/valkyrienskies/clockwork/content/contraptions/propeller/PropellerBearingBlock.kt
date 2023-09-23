package org.valkyrienskies.clockwork.content.contraptions.propeller

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Couple
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

class PropellerBearingBlock(properties: Properties) : BearingBlock(properties),
    IBE<PropellerBearingBlockEntity> {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(DIRECTION)
        super.createBlockStateDefinition(builder)
    }

    override fun use(
        state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(handIn)
                .isEmpty
        ) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn,
                    pos,
                    Consumer<PropellerBearingBlockEntity> withBlockEntityDo@{ te: PropellerBearingBlockEntity ->
                        if (te.running) {
                            te.shutDown()
                            return@withBlockEntityDo
                        }
                        te.assembleNextTick = true
                    })
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun getBlockEntityClass(): Class<PropellerBearingBlockEntity> {
        return PropellerBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PropellerBearingBlockEntity?> {
        return ClockworkBlockEntities.PROPELLER_BEARING.get()
    }

    override fun hasShaftTowards(
        world: LevelReader,
        pos: BlockPos,
        state: BlockState,
        face: net.minecraft.core.Direction
    ): Boolean {
        return face == state.getValue(FACING).opposite
    }

    override fun getRotationAxis(state: BlockState): net.minecraft.core.Direction.Axis {
        return state.getValue(FACING).axis
    }

    enum class Direction : StringRepresentable {
        PUSH,
        PULL;

        override fun getSerializedName(): String {
            return Lang.asId(name)
        }
    }

    companion object {
        val DIRECTION: EnumProperty<Direction> = EnumProperty.create(
            "direction",
            Direction::class.java
        )
        val speedRange: Couple<Int>
            get() = Couple.create(1, 16)

        fun getDirectionof(blockState: BlockState): Direction {
            return if (blockState.hasProperty(DIRECTION)) blockState.getValue(DIRECTION) else Direction.PULL
        }
    }
}