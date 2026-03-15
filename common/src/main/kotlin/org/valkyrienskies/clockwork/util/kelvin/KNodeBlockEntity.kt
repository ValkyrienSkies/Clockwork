package org.valkyrienskies.clockwork.util.kelvin

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
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
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates

abstract class KNodeBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state), IClockworkNodeBE {

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
            if (this.level != null) {
                val nodeBlock: INodeBlock? = this.level!!.getBlockState(this.blockPos).block as? INodeBlock
                nodeBlock?.nodePlace(this.blockState, this.level!!, this.blockPos, Blocks.AIR.defaultBlockState(), false)
                if (this is DuctBlockEntity) {
                    this.level!!.setBlockAndUpdate(
                        this.blockPos,
                        (this.blockState.block as DuctBlock).getConnectedState(this.level!!, this.blockState, this.blockPos) ?: this.blockState
                    )
                }
                ClockworkMod.getKelvin().markLoaded(this.getDuctNodePosition())
            }
            return
        }
        if (this.level != null && ClockworkMod.getKelvin().getNodeAt(this.getDuctNodePosition()) != null) {
            //val pressureDiff = abs(ClockworkMod.getKelvin().getPressureAt(this.getDuctNodePosition()) - (ClockworkMod.getKelvin().nodeInfo[this.getDuctNodePosition()]?.previousPressure ?: 0.0))
            //if (pressureDiff > 0.01) {
                this.setChanged()
                val tag = CompoundTag()
                this.saveData(tag, this.getDuctNodePosition())
                ClockworkPackets.sendToNear(this.level, BlockPos.containing(level.toWorldCoordinates(this.worldPosition)), 30,
                    KNodeSyncPacket(this.getDuctNodePosition(), tag))
            //}
        }
    }

    override fun setLazyTickRate(slowTickRate: Int) {
        super.setLazyTickRate(ClockworkConfig.SERVER.kelvinNodeBlockEntityLazyTickRate)
    }

    override fun addToGoggleTooltip(
        tooltip: List<Component>?,
        isPlayerSneaking: Boolean
    ): Boolean {
        return this.heatableGoggleTooltip(tooltip as MutableList? ?: mutableListOf(), isPlayerSneaking)
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

    override fun remove() {

        if (level?.isClientSide == true)
            ClockworkModClient.getKelvin().removeNode(getDuctNodePosition())
    }
}