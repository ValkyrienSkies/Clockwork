package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class PlottingTableBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    var plans = emptyList<PlottingPlan>()
        private set

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        if (plans.isNotEmpty()) tag.put("Plans", ListTag().apply { plans.forEach { add(it.write()) } })
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("Plans")) plans = tag.getList("Plans", Tag.TAG_COMPOUND.toInt())
            .take(PlottingPlan.MAX_PLANS).map { PlottingPlan.read(it as CompoundTag) }
    }
}
