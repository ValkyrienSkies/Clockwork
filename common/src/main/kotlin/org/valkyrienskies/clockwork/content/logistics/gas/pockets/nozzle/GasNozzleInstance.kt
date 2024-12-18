package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.jozufozu.flywheel.api.Instancer
import com.jozufozu.flywheel.api.MaterialManager
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Rotation
import org.valkyrienskies.clockwork.ClockworkPartials
import java.util.function.Supplier

class GasNozzleInstance(materialManager: MaterialManager?, blockEntity: GasNozzleBlockEntity?
) : SingleRotatingInstance<GasNozzleBlockEntity>(
    materialManager, blockEntity
) {

    override fun getModel(): Instancer<RotatingData> {
        //val referenceState = blockState.rotate(Rotation.CLOCKWISE_180)
        val facing = blockState.getValue(HorizontalDirectionalBlock.FACING)
        return rotatingMaterial.getModel(ClockworkPartials.NOZZLE_AXIS, blockState, facing, rotateToFace(facing))
    }

    private fun rotateToFace(facing: Direction): Supplier<PoseStack> {
        return Supplier {
            val stack = PoseStack()
            val stacker = TransformStack.cast(stack)
                .centre()

            if (facing.axis === Direction.Axis.X) stacker.rotateY(90.0)

            stacker.unCentre()
            stack
        }
    }
}