package org.valkyrienskies.clockwork.util.builder

import dev.engine_room.flywheel.api.visual.BlockEntityVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * The fact that we need this class should be banned under the Geneva Convention
 */
fun interface ClockworkSimpleBlockEntityVisualFactory<T : BlockEntity> {
    fun create(ctx: VisualizationContext, entity: T, partialTicks: Float): BlockEntityVisual<in T>
}