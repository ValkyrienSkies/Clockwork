package org.valkyrienskies.clockwork.util

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates

abstract class KNodeKineticBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {

    var dataToLoad: CompoundTag? = null

    override fun lazyTick() {
        super.lazyTick()
        if (level?.isClientSide != false) return
        if (this.dataToLoad != null && KelvinMod.getKelvinByPlatform()!!.getNodeAt(this.getDuctNodePosition()) != null) {
            loadData(this.dataToLoad!!, this.getDuctNodePosition())
            this.dataToLoad = null
        } else if (this.dataToLoad != null) {
            if (this.level != null) {
                val nodeBlock: INodeBlock? = this.level!!.getBlockState(this.blockPos).block as? INodeBlock
                nodeBlock?.nodePlace(this.blockState, this.level!!, this.blockPos, Blocks.AIR.defaultBlockState(), false)
                //if (nodeBlock is DuctBlock) {
                //    level!!.setBlockAndUpdate(this.blockPos, nodeBlock.getConnectedState(this.level!!, this.blockState, this.blockPos)!!)
                //}
                return
            }
        }
        val tag = CompoundTag()
        this.saveData(tag, this.getDuctNodePosition())

        ClockworkPackets.sendToNear(this.level, BlockPos.containing(level.toWorldCoordinates(this.worldPosition)), 30,
            KNodeSyncPacket(this.getDuctNodePosition(), tag))
    }

    override fun setLazyTickRate(slowTickRate: Int) {
        super.setLazyTickRate(10)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (ensureNodeExists()) {
            saveData(tag, this.getDuctNodePosition(), clientPacket)
        }
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (ensureNodeExists()) {
            loadData(tag, this.getDuctNodePosition(), clientPacket)
        } else {
            if (!clientPacket) dataToLoad = tag
        }
    }

    override fun addToGoggleTooltip(
        tooltip: List<Component>?,
        isPlayerSneaking: Boolean
    ): Boolean {
        this.heatableGoggleTooltip(tooltip as MutableList? ?: mutableListOf(), isPlayerSneaking)
        return super<KineticBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }
}