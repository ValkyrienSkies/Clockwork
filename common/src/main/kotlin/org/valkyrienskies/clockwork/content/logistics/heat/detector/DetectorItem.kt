package org.valkyrienskies.clockwork.content.logistics.heat.detector

import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity
import org.valkyrienskies.clockwork.platform.CWItem

class DetectorItem(properties: Properties) : CWItem(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level.isClientSide) return InteractionResult.PASS
        val blockPos = context.clickedPos
        if (level.getBlockState(blockPos).block !is HeatPipeBlock) return InteractionResult.PASS
        val be = level.getBlockEntity(blockPos) as? HeatPipeBlockEntity ?: return InteractionResult.PASS

        val temp = be.temperature
        val masses = be.gasMasses
        val jsonString = "{ Temperature: $temp, Gas Masses: $masses }"
        context.player?.displayClientMessage(TranslatableComponent(jsonString), true)
        return InteractionResult.SUCCESS
    }
}
