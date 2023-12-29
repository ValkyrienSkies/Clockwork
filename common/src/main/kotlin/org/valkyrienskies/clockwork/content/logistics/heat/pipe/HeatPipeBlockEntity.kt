package org.valkyrienskies.clockwork.content.logistics.heat.pipe


import com.simibubi.create.content.contraptions.ITransformableBlockEntity
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable

class HeatPipeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), ITransformableBlockEntity, IHeatable {

    override var heat: Int = 0
    override val maxHeat: Int = 1000
    override var flowDir: MutableSet<Direction> = mutableSetOf()
    override var flowRate: Int = 100

    override fun transform(transform: StructureTransform?) {
        val bracketBehaviour = getBehaviour(BracketedBlockEntityBehaviour.TYPE)
        bracketBehaviour?.transformBracket(transform)
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {
        //TODO Add a heat behaviour to render the ends of the pipe
    }

    override fun canTransferHeat(direction: Direction): Boolean {
        return (level!!.getBlockEntity(worldPosition.relative(direction)) is IHeatable)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getAttachedNeighbors(): List<IHeatable> {
        val neighbors: MutableList<IHeatable> = mutableListOf()
        for (direction in Direction.entries) {
            level!!.getBlockEntity(worldPosition.relative(direction))?.let {
                if (it is IHeatable) {
                    neighbors.add(it)
                }
            }
        }
        return neighbors
    }

    override fun getNeighborFlowRate(direction: Direction): Int {
        level!!.getBlockEntity(worldPosition.relative(direction))?.let {
            if (it is IHeatable) {
                return it.flowRate
            }
        }
        return 0
    }

    override fun getNeighborFlowDir(direction: Direction): MutableSet<Direction> {
        level!!.getBlockEntity(worldPosition.relative(direction))?.let {
            if (it is IHeatable) {
                return it.flowDir
            }
        }
        return mutableSetOf()
    }

    override fun isNeighborPipe(direction: Direction): Boolean {
        return (level!!.getBlockEntity(worldPosition.relative(direction)) is HeatPipeBlockEntity)
    }
}