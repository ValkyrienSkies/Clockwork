package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.util.toJOML

class SlickerMovementBehavior : MovementBehaviour {

    private val DISTANCE_BUFFER = 1.05
    var isStopped = true

    override fun renderAsNormalBlockEntity(): Boolean {
        return true
    }

    override fun tick(context: MovementContext) {
        if (context.world == null || context.world.isClientSide) return
        if (!isStopped) doUpdateConstraint(context, null, null)
        //LOGGER.warn("tick");
    }

    private fun doUpdateConstraint(context: MovementContext, nothing: Nothing?, nothing1: Nothing?) {

    }


}