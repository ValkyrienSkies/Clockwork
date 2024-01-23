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
import org.valkyrienskies.clockwork.KelvinHandler
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.kelvin.GasConnectionCreateData
import org.valkyrienskies.kelvin.GasNodeIdentifier
import org.valkyrienskies.kelvin.GasNodeResultData
import org.valkyrienskies.kelvin.GasType
import org.valkyrienskies.mod.common.util.toJOML
import java.util.*
import kotlin.collections.HashMap

class HeatPipeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state), ITransformableBlockEntity, IHeatable {

    override var gasNodeID: GasNodeIdentifier? = null

    override val gasFlows: HashMap<GasNodeIdentifier, Double> = HashMap()
    override val gasMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)
    override var temperature: Double = 0.0
    override var currentPressure: Double = 0.0

    override fun initialize() {
        super.initialize()
        val createData = KelvinHandler.defaultGasNodeCreateData(this.worldPosition.toJOML())
        KelvinHandler.addNode(createData)
        for (direction in Direction.values()) {
            if (canTransferHeat(direction)) {
                KelvinHandler.connectNodes(GasConnectionCreateData(
                    createData.identifier,
                    KelvinHandler.getNodeFromPos(this.worldPosition.relative(direction).toJOML()) ?: continue,
                    0.125,
                    0.0
                ))
            }
        }
    }

    override fun transform(transform: StructureTransform?) {
        val bracketBehaviour = getBehaviour(BracketedBlockEntityBehaviour.TYPE)
        bracketBehaviour?.transformBracket(transform)
    }
    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {
        //TODO Add a heat behaviour to render the ends of the pipe
    }

    override fun canTransferHeat(direction: Direction): Boolean {
        // todo : implement ways to restrict pipe directionality
        return (level!!.getBlockEntity(worldPosition.relative(direction)) is IHeatable)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getAttachedNeighbors(): EnumMap<Direction, IHeatable> {
        val neighbors: EnumMap<Direction, IHeatable> = EnumMap(net.minecraft.core.Direction::class.java)
        for (direction in Direction.entries) {
            level!!.getBlockEntity(worldPosition.relative(direction))?.let {
                if (it is IHeatable && canTransferHeat(direction) && it.canTransferHeat(direction.opposite)) {
                    neighbors[direction] = it
                }
            }
        }
        return neighbors
    }

    override fun getNeighborFlowRate(direction: Direction): Int {
        level!!.getBlockEntity(worldPosition.relative(direction))?.let {
            if (it is IHeatable) {
                return 0 //todo heat network
            }
        }
        return 0
    }

    override fun getNeighborFlowDir(direction: Direction): MutableSet<Direction> {
        level!!.getBlockEntity(worldPosition.relative(direction))?.let {
            if (it is IHeatable) {
                return mutableSetOf() //todo heat network
            }
        }
        return mutableSetOf()
    }

    override fun isNeighborPipe(direction: Direction): Boolean {
        return (level!!.getBlockEntity(worldPosition.relative(direction)) is HeatPipeBlockEntity)
    }

    override fun getHeatLimit(): Double {
        return 1803.0
    }

    override fun getPressureLimit(): Double {
        return 100.0
    }
}