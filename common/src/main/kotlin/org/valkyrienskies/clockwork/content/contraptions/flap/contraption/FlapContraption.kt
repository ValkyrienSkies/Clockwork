package org.valkyrienskies.clockwork.content.contraptions.flap.contraption

import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ContraptionType
import com.simibubi.create.content.contraptions.bearing.AnchoredLighter
import com.simibubi.create.content.contraptions.render.ContraptionLighter
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkContraptions
import java.util.*

class FlapContraption : Contraption() {
    var offset = 0
    protected var facing: Direction? = null

    @Throws(AssemblyException::class)
    override fun assemble(world: Level, pos: BlockPos): Boolean {
        return searchMovedStructure(world, pos, facing)
    }

    @Throws(AssemblyException::class)
    override fun searchMovedStructure(world: Level, pos: BlockPos, direction: Direction?): Boolean {
        return super.searchMovedStructure(world, pos.relative(direction, offset + 1), null)
    }

    @Throws(AssemblyException::class)
    override fun moveBlock(
            world: Level, direction: Direction?, frontier: Queue<BlockPos>, visited: Set<BlockPos>
    ): Boolean {
        return super.moveBlock(world, direction, frontier, visited)
    }

    override fun writeNBT(spawnPacket: Boolean): CompoundTag {
        val tag = super.writeNBT(spawnPacket)
        tag.putInt("facing", facing!!.get3DDataValue())
        tag.putInt("offset", offset)
        return tag
    }

    override fun readNBT(world: Level, tag: CompoundTag, spawnData: Boolean) {
        facing = Direction.from3DDataValue(tag.getInt("facing"))
        offset = tag.getInt("offset")
        super.readNBT(world, tag, spawnData)
    }

    override fun getType(): ContraptionType {
        return ClockworkContraptions.FLAP
    }

    override fun isAnchoringBlockAt(pos: BlockPos): Boolean {
        return pos == anchor.relative(facing!!.opposite, offset + 1)
    }

    override fun canBeStabilized(facing: Direction, localPos: BlockPos): Boolean {
        return if (BlockPos.ZERO == localPos || BlockPos.ZERO == localPos.relative(facing)) false else facing.axis === this.facing!!.axis
    }

    @Environment(EnvType.CLIENT)
    override fun makeLighter(): ContraptionLighter<*> {
        return AnchoredLighter(this)
    }

    companion object {
        @Throws(AssemblyException::class)
        fun assembleFlap(world: Level, pos: BlockPos, direction: Direction): FlapContraption? {
            val contraption = FlapContraption()
            val flapBlocks = 0
            contraption.facing = direction
            if (!contraption.assemble(world, pos)) {
                return null
            }
            for (i in 0..15) {
                val offsetPos = BlockPos.ZERO.relative(direction, i)
                if (contraption.getBlocks().containsKey(offsetPos)) continue
            }
            contraption.startMoving(world)
            contraption.expandBoundsAroundAxis(direction.axis)
            return contraption
        }
    }
}
