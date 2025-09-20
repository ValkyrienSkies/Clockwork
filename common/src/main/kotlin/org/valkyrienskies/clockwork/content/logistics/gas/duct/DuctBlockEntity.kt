package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.core.impl.shadow.ke
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import java.util.*

class DuctBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    val DIR_TO_CONNECTION_TYPE: EnumMap<Direction, DuctEdgeType> = EnumMap(Direction::class.java)

    var shouldUpdateEdges = false

    init {
        for (dir in Direction.values()) {
            this.DIR_TO_CONNECTION_TYPE[dir] = DuctEdgeType.NONE
        }
    }


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehavioursDeferred(behaviours)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        for (dir in Direction.values()) {
            if (tag.contains("DuctEdgeType${dir.name}")) {
                this.DIR_TO_CONNECTION_TYPE[dir] = DuctEdgeType.values()[tag.getInt("DuctEdgeType${dir.name}")]
                if (!clientPacket) shouldUpdateEdges = true

                if (clientPacket) continue
                val neighborDuct = level?.getBlockEntity(blockPos) as? DuctBlockEntity ?: continue
                val serializedEdge = tag.get("DuctEdgeType${dir.name}") as? CompoundTag ?: continue

                val kelvin = ClockworkMod.getKelvin()
                val edge = kelvin.getEdgeBetween(getDuctNodePosition(), neighborDuct.getDuctNodePosition()) ?: continue

                edge.deserialize(serializedEdge)
            }
        }
        if (clientPacket) {
            return
        }
        if (level != null) ClockworkMod.getKelvin().markLoaded(this.blockPos.toDuctNodePos(level!!.dimension().location()))
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        for (dir in Direction.values()) {
            if (this.DIR_TO_CONNECTION_TYPE[dir] != null) {
                tag.putInt("DuctEdgeType${dir.name}", this.DIR_TO_CONNECTION_TYPE[dir]!!.ordinal)

                if (clientPacket) continue
                val neighborDuct = level?.getBlockEntity(blockPos) as? DuctBlockEntity ?: continue

                val kelvin = ClockworkMod.getKelvin()
                val edge = kelvin.getEdgeBetween(getDuctNodePosition(), neighborDuct.getDuctNodePosition()) ?: continue

                val serializedEdge = edge.serialize(tag)
                tag.put("DuctEdge${dir.name}", serializedEdge)
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

    fun cycleEdgeType(dir: Direction): DuctEdgeType {
        if (this.level?.isClientSide != false) return DuctEdgeType.NONE

        val currentType = this.DIR_TO_CONNECTION_TYPE[dir]!!

        if (currentType == DuctEdgeType.NONE) return DuctEdgeType.NONE
        val otherDuctNodePos = (level!!.getBlockEntity(blockPos.relative(dir)) as? INodeBlockEntity)?.getDuctNodePosition() ?: return DuctEdgeType.NONE


        val nextType = currentType.nextScrewdrivable()
        setEdgeType(dir, otherDuctNodePos, nextType, false)
        return nextType
    }

    fun setEdgeType(dir: Direction, otherDuctNodePos: DuctNodePos, edgeType: DuctEdgeType, clientPacket: Boolean, silent: Boolean = false, forced: Boolean = false) {
        if (this.level?.isClientSide != false && !clientPacket) return

        val previousType = this.DIR_TO_CONNECTION_TYPE[dir]!!
        this.DIR_TO_CONNECTION_TYPE[dir] = edgeType
        if (!clientPacket) {
            syncEdge(dir)
            //println("SETTING EDGE TYPE: ${this.getDuctNodePosition()} to $otherDuctNodePos with $edgeType")
            if ((previousType != edgeType && !silent) || forced) {
                ClockworkMod.getKelvin().removeEdge(getDuctNodePosition(), otherDuctNodePos)
                if (edgeType != DuctEdgeType.NONE) {
                    val newEdge = DuctEdgeType.createEdgeType(getDuctNodePosition(), otherDuctNodePos, edgeType)
                    ClockworkMod.getKelvin().addEdge(getDuctNodePosition(), otherDuctNodePos, newEdge)
                }
            }
        }
    }

    fun clearEdgeType(dir: Direction) {
        if (this.level?.isClientSide != false) {
            return
        }
        this.DIR_TO_CONNECTION_TYPE[dir] = DuctEdgeType.NONE
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