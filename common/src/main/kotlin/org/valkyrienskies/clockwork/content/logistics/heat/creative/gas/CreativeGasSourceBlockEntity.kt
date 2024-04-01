package org.valkyrienskies.clockwork.content.logistics.heat.creative.gas

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3i
import org.valkyrienskies.clockwork.KelvinHandler
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.GasConnectionCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toJOML
import java.util.EnumMap

class CreativeGasSourceBlockEntity (type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) ,
    IHeatable {

    override var gasNodeID: GasNodeIdentifier? = null

    override val gasFlows: HashMap<GasNodeIdentifier, Double> = HashMap()



    override val gasMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)
    override var temperature: Double = 0.0
    override var currentPressure: Double = 0.0

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {

    }

    override fun initialize() {
        super.initialize()
        if (this.level == null || this.level!!.isClientSide) {
            return
        }
        val newID = gasNodeID ?: GasNodeIdentifier(this.worldPosition.toJOML(), 0)
        val createData = KelvinHandler.defaultGasNodeCreateData(newID.pos, initTemp = temperature)
        KelvinHandler.addNode(createData)
        this.gasNodeID = createData.identifier
        for (direction in Direction.values()) {
            if (canTransferHeat(direction)) {
                KelvinHandler.connectNodes(
                    GasConnectionCreateData(
                        createData.identifier,
                        KelvinHandler.getNodeFromPos(this.worldPosition.relative(direction).toJOML()) ?: continue,
                        0.125,
                        0.0
                    )
                )
            }
        }
    }

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return

        val currentGasMass = gasMasses.getOrDefault(GasType.PHLOGISTON, 0.0)

        val newGasMass = Mth.clamp(currentGasMass + 100.0, 0.0, getPressureLimit())
        gasMasses[GasType.PHLOGISTON] = newGasMass

        val directionalDeltaMasses: HashMap<GasNodeIdentifier, Double> = HashMap()

        Direction.values().filter { canTransferHeat(it) }.forEach { direction ->
            val id = KelvinHandler.getNodeFromPos(worldPosition.relative(direction).toJOML()) ?: return@forEach
            directionalDeltaMasses[id] = 100.0
        }

        val updatedNode = GasNodeChangesData(
            gasNodeID!!,
            gasMasses,
            temperature,
            directionalDeltaMasses
        )

        KelvinHandler.editNode(updatedNode)
    }

    override fun canTransferHeat(direction: Direction): Boolean {
        return blockState.hasProperty(AltMeterBlock.POWERED) && blockState.getValue(AltMeterBlock.POWERED)
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

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putLong("kelvin/nodeId", gasNodeID?.id ?: -1)
        val posAsIntArray = IntArray(3)
        posAsIntArray[0] = gasNodeID?.pos?.x() ?: 0
        posAsIntArray[1] = gasNodeID?.pos?.y() ?: 0
        posAsIntArray[2] = gasNodeID?.pos?.z() ?: 0
        tag.putIntArray("kelvin/nodePos", posAsIntArray)
        tag.putDouble("kelvin/temperature", temperature)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        temperature = tag.getDouble("kelvin/temperature")
        val posAsIntArray = tag.getIntArray("kelvin/nodePos")
        val pos = Vector3i(posAsIntArray[0], posAsIntArray[1], posAsIntArray[2])
        val id = tag.getLong("kelvin/nodeId")
        if (id.toInt() != -1) {
            this.gasNodeID = GasNodeIdentifier(pos, id)
        }
        super.read(tag, clientPacket)
    }
}