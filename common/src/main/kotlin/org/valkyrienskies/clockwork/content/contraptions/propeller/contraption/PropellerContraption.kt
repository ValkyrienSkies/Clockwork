package org.valkyrienskies.clockwork.content.contraptions.propeller.contraption

import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.BlockMovementChecks
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ContraptionType
import com.simibubi.create.content.contraptions.bearing.AnchoredLighter
import com.simibubi.create.content.contraptions.render.ContraptionLighter
import com.simibubi.create.foundation.utility.UniqueLinkedList
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.phys.AABB
import org.apache.commons.lang3.tuple.Pair
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkContraptions
import org.valkyrienskies.clockwork.util.ClockworkConstants
import java.util.*

class PropellerContraption : Contraption {
    var offset = 0
    var sailBlocks = 0
        protected set
    var facing: Direction? = null
        protected set

    var brass = false

    constructor()
    constructor(facing: Direction?) {
        this.facing = facing
    }

    //    @Override
    //    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
    //        BlockPos offset = pos.relative(facing);
    //        if (!searchMovedStructure(world, offset, null))
    //            return false;
    //        startMoving(world);
    //        expandBoundsAroundAxis(facing.getAxis());
    //        if (sailBlocks < 2)
    //            throw AssemblyException.notEnoughSails(sailBlocks);
    //        if (blocks.isEmpty())
    //            return false;
    //        return true;
    //    }
    @Throws(AssemblyException::class)
    override fun assemble(world: Level, pos: BlockPos): Boolean {
        return searchMovedStructure(world, pos, facing)
    }

    @Throws(AssemblyException::class)
    override fun moveBlock(
        world: Level,
        direction: Direction?,
        frontier: Queue<BlockPos>,
        visited: Set<BlockPos>
    ): Boolean {
        return super.moveBlock(world, direction, frontier, visited)
    }

    @Throws(AssemblyException::class)
    override fun searchMovedStructure(world: Level, pos: BlockPos, direction: Direction?): Boolean {
        if (brass) return super.searchMovedStructure(world, pos.relative(direction, offset + 1), null)

        anchor = pos
        val propellerBlock = world.getBlockState(pos.relative(facing!!, offset + 1))

        if (propellerBlock.`is`(ClockworkBlocks.BLADE_CONTROLLER.get())) {
            val blockFacing = propellerBlock.getValue(BlockStateProperties.FACING)

            return blockFacing == facing
        }
        throw notProp()
    }

    override fun getType(): ContraptionType {
        return ClockworkContraptions.PROPELLER
    }

    override fun isAnchoringBlockAt(pos: BlockPos): Boolean {
        return pos == anchor.relative(facing!!.opposite, offset + 1)
    }

    public override fun addBlock(pos: BlockPos, capture: Pair<StructureTemplate.StructureBlockInfo, BlockEntity>) {
        val localPos = pos.subtract(anchor)
        if (!getBlocks().containsKey(localPos) && AllTags.AllBlockTags.WINDMILL_SAILS.matches(capture.key.state)) sailBlocks++
        super.addBlock(pos, capture)
    }

    override fun writeNBT(spawnPacket: Boolean): CompoundTag {
        val tag = super.writeNBT(spawnPacket)
        tag.putInt(ClockworkConstants.Nbt.SAILS, sailBlocks)
        tag.putInt(ClockworkConstants.Nbt.FACING, facing!!.get3DDataValue())
        tag.putInt(ClockworkConstants.Nbt.OFFSET, offset)
        return tag
    }

    override fun readNBT(world: Level, tag: CompoundTag, spawnData: Boolean) {
        sailBlocks = tag.getInt(ClockworkConstants.Nbt.SAILS)
        facing = Direction.from3DDataValue(tag.getInt(ClockworkConstants.Nbt.FACING))
        offset = tag.getInt(ClockworkConstants.Nbt.OFFSET)
        super.readNBT(world, tag, spawnData)
    }

    override fun canBeStabilized(facing: Direction, localPos: BlockPos): Boolean {
        return if (facing.opposite == this.facing && BlockPos.ZERO == localPos) false else facing.axis === this.facing!!.axis
    }

    @Environment(EnvType.CLIENT)
    override fun makeLighter(): ContraptionLighter<*> {
        return AnchoredLighter(this)
    }

    companion object {
        @Throws(AssemblyException::class)
        fun assembleProp(world: Level, pos: BlockPos, direction: Direction, brass: Boolean): PropellerContraption? {
            val contraption = PropellerContraption()
            val flapBlocks = 0
            contraption.facing = direction
            contraption.brass = brass
            if (!contraption.assemble(world, pos)) {
                return null
            }
            contraption.startMoving(world)
            contraption.expandBoundsAroundAxis(direction.axis)
            return contraption
        }

        @JvmStatic
        fun notProp(): AssemblyException  {
            return AssemblyException(TranslatableComponent("contraptions.propeller.not_prop"))
        }
    }
}