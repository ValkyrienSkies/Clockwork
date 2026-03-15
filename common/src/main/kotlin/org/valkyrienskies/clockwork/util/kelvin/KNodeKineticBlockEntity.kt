package org.valkyrienskies.clockwork.util.kelvin

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IClockworkNodeBE
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates

abstract class KNodeKineticBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos, state), IClockworkNodeBE {

    var dataToLoad: CompoundTag? = null

    override fun lazyTick() {
        super.lazyTick()
        if (level?.isClientSide != false) return
        //if (this.dataToLoad != null) println(this.dataToLoad)
        if (this.dataToLoad != null && KelvinMod.getKelvinByPlatform()!!.getNodeAt(this.getDuctNodePosition()) != null) {
            loadData(this.dataToLoad!!, this.getDuctNodePosition())
            //println("Loaded data for ${this.getDuctNodePosition()} from ${this.blockPos} at ${this.level!!.dimension()}")
            //println(this.dataToLoad)
            this.dataToLoad = null
        } else if (this.dataToLoad != null) {
            val nodeBlock: INodeBlock? = this.level!!.getBlockState(this.blockPos).block as? INodeBlock
            nodeBlock?.nodePlace(this.blockState, this.level!!, this.blockPos, Blocks.AIR.defaultBlockState(), false)
            ClockworkMod.getKelvin().markLoaded(this.getDuctNodePosition())
            return
        }

        if (this.level != null && ClockworkMod.getKelvin().getNodeAt(this.getDuctNodePosition()) != null) {
//            val pressureDiff = abs(ClockworkMod.getKelvin().getPressureAt(this.getDuctNodePosition()) - (ClockworkMod.getKelvin().nodeInfo[this.getDuctNodePosition()]?.previousPressure ?: 0.0))
//            if (pressureDiff > 0.01) {
                this.setChanged()
                val tag = CompoundTag()
                this.saveData(tag, this.getDuctNodePosition())
                ClockworkPackets.sendToNear(this.level, BlockPos.containing(level.toWorldCoordinates(this.worldPosition)), 30,
                    KNodeSyncPacket(this.getDuctNodePosition(), tag))
//            }
        }
    }

    override fun setLazyTickRate(slowTickRate: Int) {
        super.setLazyTickRate(ClockworkConfig.SERVER.kelvinNodeBlockEntityLazyTickRate)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (ensureNodeExists() && !clientPacket) {
            saveData(tag, this.getDuctNodePosition(), clientPacket)
        }
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (ensureNodeExists() && !clientPacket) {
            //if (!clientPacket) println("node exists, loading")
            loadData(tag, this.getDuctNodePosition(), clientPacket)
        } else {
            if (!clientPacket) dataToLoad = tag
        }
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun addToGoggleTooltip(
        tooltip: List<Component>?,
        isPlayerSneaking: Boolean
    ): Boolean {
        this.heatableGoggleTooltip(tooltip as MutableList? ?: mutableListOf(), isPlayerSneaking)
        super<KineticBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
        return true
    }

    override fun remove() {
        if (level?.isClientSide == true)
            ClockworkModClient.getKelvin().removeNode(getDuctNodePosition())
    }
}
