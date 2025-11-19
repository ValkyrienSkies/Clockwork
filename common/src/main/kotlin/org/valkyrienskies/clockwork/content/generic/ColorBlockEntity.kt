package org.valkyrienskies.clockwork.content.generic

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.levelWrappers.SchematicLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.physicalities.wing.BlockEntityColorPacket

class ColorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    SmartBlockEntity(type, pos, state) {
    private var color = -1
    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {}
    public override fun write(tag: CompoundTag, client: Boolean) {
        if (color != -1) tag.putInt("Clockwork\$color", color)
        super.writeSafe(tag)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("Clockwork\$color")) color = tag.getInt("Clockwork\$color")
    }

    fun getColor(): Int {
        return color
    }

    fun setColor(rgb: Int) {
        color = rgb
        this.setChanged()
    }

    fun clearColor() {
        color = -1
        this.setChanged()
    }

    override fun setChanged() {
        super.setChanged()
        if (getLevel() != null && !getLevel()!!.isClientSide() && getLevel() !is SchematicLevel) {
            ClockworkPackets.sendToNear(getLevel(), blockPos, 64, BlockEntityColorPacket(this))
        }
    }
}
