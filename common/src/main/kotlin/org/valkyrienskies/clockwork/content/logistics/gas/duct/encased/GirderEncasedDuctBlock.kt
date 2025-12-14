package org.valkyrienskies.clockwork.content.logistics.gas.duct.encased

import com.simibubi.create.AllBlocks
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement
import com.simibubi.create.content.decoration.girder.GirderBlock
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock

class GirderEncasedDuctBlock(properties: Properties) : DuctBlock(properties), SpecialBlockItemRequirement {

    init {
        registerDefaultState(defaultBlockState()!!.setValue(TOP, false)!!.setValue(BOTTOM, false)!!.setValue(AXIS, Direction.Axis.X))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(
            builder.add(
                TOP,
                BOTTOM,
                AXIS
            )
        )
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction.axis != (level.getBlockState(self)?.getValue(AXIS) ?: Direction.Axis.Y)) return false
        return super.canConnectTo(self, other, direction, level)
    }

    override fun getRequiredItems(
        state: BlockState?,
        blockEntity: BlockEntity?
    ): ItemRequirement? {
        return ItemRequirement.of(ClockworkBlocks.DUCT.defaultState, blockEntity)
            .union(ItemRequirement.of(AllBlocks.METAL_GIRDER.defaultState, blockEntity))
    }

    companion object {
        val TOP: BooleanProperty = GirderBlock.TOP
        val BOTTOM: BooleanProperty = GirderBlock.BOTTOM
        val AXIS = BlockStateProperties.AXIS
    }

}