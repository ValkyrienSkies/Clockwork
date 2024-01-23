package org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.datastructures.ShipConnDataAttachment
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import java.util.*

class GasNozzleBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos,
    state
), IHeatable {

    private var pocketId: Int? = null
    var pocketSize: Int = 0

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
        TODO("Not yet implemented")
    }

    override fun getNeighborFlowRate(direction: Direction): Int {
        TODO("Not yet implemented")
    }

    override fun getNeighborFlowDir(direction: Direction): MutableSet<Direction> {
        TODO("Not yet implemented")
    }

    override fun isNeighborPipe(direction: Direction): Boolean {
        TODO("Not yet implemented")
    }
}