package org.valkyrienskies.clockwork.util

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates

abstract class KNodeKineticBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {

    override fun lazyTick() {
        super.lazyTick()
        if (level?.isClientSide != false) return
        val tag = CompoundTag()
        this.saveData(tag, this.getDuctNodePosition())

        ClockworkPackets.sendToNear(this.level, BlockPos.containing(level.toWorldCoordinates(this.worldPosition)), 30,
            KNodeSyncPacket(this.getDuctNodePosition(), tag))
    }

    override fun setLazyTickRate(slowTickRate: Int) {
        super.setLazyTickRate(10)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (tag != null) {
            saveData(tag, this.getDuctNodePosition())
        }
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag != null) {
            loadData(tag, this.getDuctNodePosition())
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