package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.smart_propeller.SmartPropellerBearingBlockEntity

class DebugWandItem(properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val blockPos = context.clickedPos
        val rend = level.random

        if (!level.isClientSide && level.getBlockState(blockPos).`is`(ClockworkBlocks.SMART_PROPELLER_BEARING.get())) {
            if (level.getBlockEntity(blockPos) is SmartPropellerBearingBlockEntity) {
                val be = level.getBlockEntity(blockPos) as SmartPropellerBearingBlockEntity

                if (rend.nextBoolean()) {
                    be.setTiltTarget(Vec3(-0.33, 0.0, -0.67))
                } else {
                    be.setTiltTarget(Vec3(0.5, 0.0, 0.5))
                }


                return InteractionResult.SUCCESS
            }
        }

        return super.useOn(context)
    }
}