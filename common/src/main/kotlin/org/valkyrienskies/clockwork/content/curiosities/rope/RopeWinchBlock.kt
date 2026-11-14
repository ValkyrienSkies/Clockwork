package org.valkyrienskies.clockwork.content.curiosities.rope

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class RopeWinchBlock(properties: Properties) : HorizontalAxisKineticBlock(properties), IBE<RopeWinchBlockEntity> {
    override fun getBlockEntityClass() = RopeWinchBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.ROPE_WINCH.get()
}
