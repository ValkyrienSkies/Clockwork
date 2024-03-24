package org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.KelvinHandler
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.GasConnectionCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.datastructures.ShipConnDataAttachment
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import java.util.*

class GasNozzleBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos,
    state
), IHeatable {

    override var gasNodeID: GasNodeIdentifier? = null

    override var currentPressure: Double = 0.0
    override var temperature: Double = 273.0
    override val gasMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)
    override val gasFlows: HashMap<GasNodeIdentifier, Double> = HashMap()

    private var pocketId: Int? = null
    var pocketSize: Int = 0

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

        if (level!!.isClientSide) {
            return
        }

        val slevel = level!! as ServerLevel

        if (slevel.getShipObjectManagingPos(this.worldPosition) != null) {
            val ship: LoadedServerShip = slevel.getShipObjectManagingPos(this.worldPosition)!!
            val connData = ship.getAttachment(ShipConnDataAttachment::class.java)
            if (connData != null) {
                // do bloon stuff
                val positionToCheck = this.worldPosition.above()

                //ClockworkMod.LOGGER.info("gas nozzle checking for pocket!")

                if (connData.getAirPocketFromPoint(positionToCheck.toJOML()) != null) {
                    this.pocketId = connData.getAirPocketFromPoint(positionToCheck.toJOML())?.id

                    ClockworkMod.LOGGER.info("gas nozzle has pocket!")
                    ClockworkMod.LOGGER.info("pocket size: ${this.pocketSize}")
                }

                if (this.pocketId != null) {
                    val airPocket = connData.getAirPocket(this.pocketId!!)
                    if (airPocket != null) {
                        this.pocketSize = airPocket.pocket.size;
                        ClockworkPackets.sendToNear(
                            level!!,
                            this.worldPosition,
                            64,
                            TempGasNozzleSyncPacket(this)
                        )
                    }
                } else {
                    this.pocketSize = 0
                    ClockworkPackets.sendToNear(
                        level!!,
                        this.worldPosition,
                        64,
                        TempGasNozzleSyncPacket(this)
                    )
                }
            }
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        var add = super.addToGoggleTooltip(tooltip, isPlayerSneaking)
        if (this.pocketSize == 0) {
            return add
        }
        tooltip.add(Component.nullToEmpty("Pocket Size: $pocketSize"))
        return true
    }

    fun syncPocketSize(int: Int) {
        this.pocketSize = int
    }

    override fun canTransferHeat(direction: Direction): Boolean {
        return (direction == Direction.DOWN)
    }

    override fun getAttachedNeighbors(): EnumMap<Direction, IHeatable> {
        val neighbors: EnumMap<Direction, IHeatable> = EnumMap(net.minecraft.core.Direction::class.java)

        if (level!!.getBlockEntity(worldPosition.below()) is IHeatable) {
            val heatable = level!!.getBlockEntity(worldPosition.below()) as IHeatable
            if (heatable.canTransferHeat(Direction.UP)) {
                neighbors[Direction.DOWN] = heatable
            }
        }

        return neighbors
    }

    override fun getNeighborFlowRate(direction: Direction): Int {
        TODO("Not yet implemented")
    }

    override fun getNeighborFlowDir(direction: Direction): MutableSet<Direction> {
        TODO("Not yet implemented")
    }

    override fun isNeighborPipe(direction: Direction): Boolean {
        return (level!!.getBlockEntity(worldPosition.relative(direction)) is HeatPipeBlockEntity)
    }

    override fun getHeatLimit(): Double {
        return 1500.0
    }

    override fun getPressureLimit(): Double {
        return 500.0
    }
}