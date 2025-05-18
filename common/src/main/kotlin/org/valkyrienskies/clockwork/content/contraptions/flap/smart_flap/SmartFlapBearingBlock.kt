package org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap

import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock

class SmartFlapBearingBlock(properties: Properties?): FlapBearingBlock(properties) {

    override fun getBlockEntityType(): BlockEntityType<out SmartFlapBearingBlockEntity> {
        return ClockworkBlockEntities.SMART_FLAP_BEARING.get()
    }
}