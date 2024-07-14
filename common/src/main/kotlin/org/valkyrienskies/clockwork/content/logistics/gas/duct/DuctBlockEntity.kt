package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeNode
import org.valkyrienskies.mod.common.util.toJOMLD
import java.time.Clock
import java.util.*

class DuctBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {

    val DIR_TO_CONNECTION_TYPE: EnumMap<Direction, ConnectionType> = EnumMap(Direction::class.java)

    init {
        for (dir in Direction.values()) {
            this.DIR_TO_CONNECTION_TYPE[dir] = ConnectionType.NONE
        }
    }


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehavioursDeferred(behaviours)
    }

    override fun onLoad() {
        super.onLoad()
        ClockworkMod.getKelvin().markLoaded(this.blockPos.toJOMLD())
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        ClockworkMod.getKelvin().markUnloaded(this.blockPos.toJOMLD())
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("currentTemperature")) {
            ClockworkMod.getKelvin().nodeInfo[this.blockPos.toJOMLD()]?.currentTemperature = tag.getDouble("currentTemperature")
        }
        for (dir in Direction.values()) {
            if (tag.contains("connectionType${dir.name}")) {
                this.DIR_TO_CONNECTION_TYPE[dir] = ConnectionType.values()[tag.getInt("connectionType${dir.name}")]
            }
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putDouble("currentTemperature", ClockworkMod.getKelvin().getTemperatureAt(this.blockPos.toJOMLD()))
        for (dir in Direction.values()) {
            if (this.DIR_TO_CONNECTION_TYPE[dir] != null) {
                tag.putInt("connectionType${dir.name}", this.DIR_TO_CONNECTION_TYPE[dir]!!.ordinal)
            }
        }
        super.write(tag, clientPacket)
    }

    override fun remove() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.remove()
    }

    override fun destroy() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.destroy()
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
    }

    fun cycleEdgeType(dir: Direction): ConnectionType {
        if (this.level?.isClientSide != false) {
            return ConnectionType.NONE
        }
        val currentType = this.DIR_TO_CONNECTION_TYPE[dir]!!

        if (currentType == ConnectionType.NONE) return ConnectionType.NONE

        val nextType = currentType.nextScrewdrivable()
        setEdgeType(dir, nextType, false)
        return nextType
    }

    fun setEdgeType(dir: Direction, edgeType: ConnectionType, clientPacket: Boolean, silent: Boolean = false) {
        if (this.level?.isClientSide != false && !clientPacket) {
            return
        }
        val previousType = this.DIR_TO_CONNECTION_TYPE[dir]!!
        this.DIR_TO_CONNECTION_TYPE[dir] = edgeType
        if (!clientPacket) {
            syncEdge(dir)
            if (previousType != edgeType && !silent) {
                ClockworkMod.getKelvin().removeEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD())
                if (edgeType != ConnectionType.NONE) {
                    val newEdge = createEdgeType(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD(), edgeType)
                    ClockworkMod.getKelvin().addEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD(), newEdge)
                }
            }
        }
    }

    fun clearEdgeType(dir: Direction) {
        if (this.level?.isClientSide != false) {
            return
        }
        this.DIR_TO_CONNECTION_TYPE[dir] = ConnectionType.NONE
        syncEdge(dir)
    }

    private fun syncEdge(direction: Direction) {
        if (this.level?.isClientSide != false) {
            return
        }
        val edgeType = this.DIR_TO_CONNECTION_TYPE[direction]!!

        ClockworkPackets.sendToNear(
            this.level!!,
            this.blockPos,
            64,
            DuctEdgeSyncPacket(this.blockPos, direction, edgeType)
        )
    }

}