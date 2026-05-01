package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.magnitudeSqr
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import java.util.*

class DuctBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    val DIR_TO_CONNECTION_TYPE: EnumMap<Direction, DuctEdgeType> = EnumMap(Direction::class.java)
    val edgeData = HashMap<EdgePos, CompoundTag>()
    var objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

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
                val edgeType = DuctEdgeType.entries[tag.getInt("DuctEdgeType${dir.name}")]
                this.DIR_TO_CONNECTION_TYPE[dir] = edgeType
                if (clientPacket) continue
                shouldUpdateEdges = true
            }
        }
        if (clientPacket) return
        if (level != null) ClockworkMod.getKelvin(level).markLoaded(this.blockPos.toDuctNodePos(level!!.dimension().location()))

        val edgeDataTag = tag.get("edgeData") as? CompoundTag ?: return
        edgeDataTag.allKeys.forEach {
            val edgePos = objectMapper.readValue(it, EdgePos::class.java)
            val tag = edgeDataTag.get(it) as? CompoundTag ?: return
            edgeData[edgePos] = tag
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        for (dir in Direction.values()) {
            if (this.DIR_TO_CONNECTION_TYPE[dir] != null) {
                tag.putInt("DuctEdgeType${dir.name}", this.DIR_TO_CONNECTION_TYPE[dir]!!.ordinal)
            }
        }

        if (!clientPacket) {
            val edgeDataTag = CompoundTag()
            edgeData.forEach { edgeDataTag.put(objectMapper.writeValueAsString(it.key), it.value) }
            tag.put("edgeData", edgeDataTag)
        }


        super.write(tag, clientPacket)
    }

    override fun destroy() {
        if (level != null && !level!!.isClientSide) ClockworkMod.getKelvin(level).removeNode(this.blockPos.toDuctNodePos(level!!.dimension().location()))
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
                ClockworkMod.getKelvin(level).removeEdge(getDuctNodePosition(), otherDuctNodePos)
                if (edgeType != DuctEdgeType.NONE) {

                    // Edge directionality is important for oneway
                    // which is why it's enforced by axis direction
                    val nodeA: DuctNodePos
                    val nodeB: DuctNodePos
                    if (edgeType.isOneWay()) {
                        if (getDuctNodePosition().magnitudeSqr() > otherDuctNodePos.magnitudeSqr()) {
                            nodeA = getDuctNodePosition()
                            nodeB = otherDuctNodePos
                        } else {
                            nodeA = otherDuctNodePos
                            nodeB = getDuctNodePosition()
                        }
                    } else {
                        nodeA = getDuctNodePosition()
                        nodeB = otherDuctNodePos
                    }

                    val newEdge = DuctEdgeType.createEdgeType(nodeA, nodeB, edgeType)

                    if (edgeData[EdgePos(nodeA, nodeB)] != null)
                        newEdge.deserialize(edgeData[EdgePos(nodeA, nodeB)]!!)

                    ClockworkMod.getKelvin(level).addEdge(nodeA, nodeB, newEdge)
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

    override fun createRenderBoundingBox(): AABB? {
        return super.createRenderBoundingBox().inflate(1.0/16.0)
    }


    data class EdgePos(val first: DuctNodePos, val second: DuctNodePos)
}
