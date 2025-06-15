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
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeNode
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
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
        if (this.level?.isClientSide != false) {
            return
        }
        ClockworkMod.getKelvin().markLoaded(this.blockPos.toDuctNodePos(level!!.dimension().location()))
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        for (dir in Direction.values()) {
            if (tag.contains("connectionType${dir.name}")) {
                this.DIR_TO_CONNECTION_TYPE[dir] = ConnectionType.values()[tag.getInt("connectionType${dir.name}")]
            }
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        for (dir in Direction.values()) {
            if (this.DIR_TO_CONNECTION_TYPE[dir] != null) {
                tag.putInt("connectionType${dir.name}", this.DIR_TO_CONNECTION_TYPE[dir]!!.ordinal)
            }
        }
        super.write(tag, clientPacket)
    }

    override fun remove() {
        if (level != null && !level!!.isClientSide) ClockworkMod.getKelvin().removeNode(this.blockPos.toDuctNodePos(level!!.dimension().location()))
        super.remove()
    }

    override fun destroy() {
        if (level != null && !level!!.isClientSide) ClockworkMod.getKelvin().removeNode(this.blockPos.toDuctNodePos(level!!.dimension().location()))
        super.destroy()
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    fun cycleEdgeType(dir: Direction): ConnectionType {
        if (this.level?.isClientSide != false) return ConnectionType.NONE

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
                val dimension = level!!.dimension().location()
                ClockworkMod.getKelvin().removeEdge(this.blockPos.toDuctNodePos(dimension), this.blockPos.relative(dir).toDuctNodePos(dimension))
                if (edgeType != ConnectionType.NONE) {
                    val newEdge = createEdgeType(this.blockPos.toDuctNodePos(dimension), this.blockPos.relative(dir).toDuctNodePos(dimension), edgeType)
                    ClockworkMod.getKelvin().addEdge(this.blockPos.toDuctNodePos(dimension), this.blockPos.relative(dir).toDuctNodePos(dimension), newEdge)
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