package org.valkyrienskies.clockwork.content.logistics.heat.creative.source

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3i
import org.valkyrienskies.clockwork.KelvinHandler
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.GasConnectionCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangeFromGame
import org.valkyrienskies.clockwork.kelvin.api.GasNodeCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toJOML
import java.util.EnumMap
import kotlin.math.max

class CreativeHeatSourceBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos,
    state
), IHeatable {

    override var gasNodeID: GasNodeIdentifier? = null
    override val gasMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)
    override var temperature: Double = 273.0
    override var currentPressure: Double = 0.0

    protected var generatedHeat: ScrollValueBehaviour? = null

    override fun initialize() {
        super.initialize()
        if (this.level == null || this.level!!.isClientSide) {
            return
        }
        val newID = gasNodeID ?: GasNodeIdentifier(this.worldPosition.toJOML(), 0)
        val createData = GasNodeCreateData(newID, EnumMap(GasType::class.java), 1.0, temperature)
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

    override fun canTransferHeat(direction: Direction): Boolean {
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
        return 200000.0
    }

    override fun getPressureLimit(): Double {
        return 100000.0
    }

    fun updateConnections() {
        if (this.level != null && !this.level!!.isClientSide) {
            for (direction in Direction.values()) {
                if (canTransferHeat(direction)) {
                    KelvinHandler.connectNodes(
                        GasConnectionCreateData(
                            this.gasNodeID!!,
                            KelvinHandler.getNodeFromPos(this.worldPosition.relative(direction).toJOML()) ?: continue,
                            0.125,
                            0.0
                        )
                    )
                }
            }
        }
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        generatedHeat = ScrollValueBehaviour(Lang.translateDirect("logistics.creative_heat_source.heat"), this, HeatValueBox())
        generatedHeat!!.between(MIN_HEAT, MAX_HEAT)
        generatedHeat!!.value = (DEFAULT_HEAT)
        generatedHeat!!.withCallback { updateGeneratedHeat() }


        behaviours.add(generatedHeat!!)
    }

    var currentTargetHeat = 273.0

    private fun updateGeneratedHeat() {
        val heat = generatedHeat!!.value
        if (heat.toDouble() != currentTargetHeat) {
            currentTargetHeat = heat.toDouble()
        }
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

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return

        this.currentTargetHeat = generatedHeat!!.value.toDouble()

        val currentGasMass = gasMasses.getOrDefault(GasType.PHLOGISTON, 0.0)

        // TODO: Implement adding heat correctly
        var deltaThermalEnergy = 0.0

        if (currentGasMass > 0.0) {
            deltaThermalEnergy = (currentTargetHeat - temperature) * currentGasMass * 4.0
        }

        val updatedNode = GasNodeChangeFromGame(
            gasNodeID!!,
            EnumMap<GasType, Double>(GasType::class.java).apply {
                put(GasType.PHLOGISTON, 0.0)
            },
            deltaThermalEnergy,
        )

        KelvinHandler.editNode(updatedNode)

        /*
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
         */
    }


    private class HeatValueBox : CenteredSideValueBoxTransform() {

    }
    companion object {
        const val MAX_HEAT = 2273
        const val MIN_HEAT = 0
        const val DEFAULT_HEAT = 273
    }
}