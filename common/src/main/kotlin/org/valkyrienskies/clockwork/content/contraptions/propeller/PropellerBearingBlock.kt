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
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import java.util.function.Consumer

class PropellerBearingBlock(properties: Properties) : BearingBlock(properties), IBE<PropellerBearingBlockEntity> {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(SPIN_DIRECTION)
        super.createBlockStateDefinition(builder)
    }


    override fun use(
        state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn,
                    pos,
                    Consumer<PropellerBearingBlockEntity> withBlockEntityDo@{ te: PropellerBearingBlockEntity ->
                        if (te.running) {
                            te.shutDown()
                            return@withBlockEntityDo
                        }
                        if (te.assembleCooldown <= 0) {
                            te.assembleNextTick = true
                        }
                    })
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun newBlockEntity(p_153215_: BlockPos, p_153216_: BlockState): BlockEntity? {
        val isBrass = this.descriptionId == "block.vs_clockwork.brass_propeller_bearing"
        if (isBrass) {
            return PropellerBearingBlockEntity(ClockworkBlockEntities.PROPELLER_BEARING.get(), p_153215_, p_153216_, true)
        }
        return super.newBlockEntity(p_153215_, p_153216_)
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
        face: Direction
    ): Boolean {
        return face == state.getValue(FACING).opposite
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    enum class SpinDirection : StringRepresentable {
        PUSH,
        PULL;

        override fun getSerializedName(): String {
            return Lang.asId(name)
        }
    }

    companion object {
        val SPIN_DIRECTION: EnumProperty<SpinDirection> = EnumProperty.create(
            "direction",
            SpinDirection::class.java
        )
        val speedRange: Couple<Int>
            get() = Couple.create(1, 16)

        fun getDirectionof(blockState: BlockState): SpinDirection {
            return if (blockState.hasProperty(SPIN_DIRECTION)) blockState.getValue(SPIN_DIRECTION) else SpinDirection.PULL
        }
    }
}