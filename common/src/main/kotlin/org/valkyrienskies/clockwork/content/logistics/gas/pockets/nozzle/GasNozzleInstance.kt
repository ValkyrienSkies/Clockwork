package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.RotatingInstance
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual
import dev.engine_room.flywheel.api.instance.Instancer
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Rotation
import org.valkyrienskies.clockwork.ClockworkPartials
import java.util.function.Supplier

//todo: do visuals for v6
class GasNozzleInstance() {

//class GasNozzleInstance(materialManager: VisualizationContext, blockEntity: GasNozzleBlockEntity
//) : SingleAxisRotatingVisual<GasNozzleBlockEntity>(
//    materialManager, blockEntity
//) {

//    override fun getModel(): Instancer<RotatingInstance> {
//        //val referenceState = blockState.rotate(Rotation.CLOCKWISE_180)
//        val facing = blockState.getValue(HorizontalDirectionalBlock.FACING)
//        return rotatingMaterial.getModel(ClockworkPartials.NOZZLE_AXIS, blockState, facing, rotateToFace(facing))
//    }
//
//    private fun rotateToFace(facing: Direction): Supplier<PoseStack> {
//        return Supplier {
//            val stack = PoseStack()
//            val stacker = TransformStack.of(stack)
//                .centre()
//
//            if (facing.axis === Direction.Axis.X) stacker.rotateY(90.0)
//
//            stacker.unCentre()
//            stack
//        }
//    }
}
