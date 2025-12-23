package org.valkyrienskies.clockwork.content.contraptions.flap

import com.mojang.math.Axis
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingVisual
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.visual.DynamicVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.OrientedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials
import java.util.function.Consumer

class FlapBearingVisual<B>(context: VisualizationContext, blockEntity: B, partialTick: Float) : OrientedRotatingVisual<B>(
    context, blockEntity, partialTick, Direction.SOUTH, blockEntity.blockState.getValue(
        BlockStateProperties.FACING
    ).opposite, Models.partial(AllPartialModels.SHAFT_HALF)
), SimpleDynamicVisual where B : KineticBlockEntity, B : IBearingBlockEntity {
    val topInstance: OrientedInstance

    val rotationAxis: Axis
    val blockOrientation: Quaternionf

    init {
        val facing = blockState.getValue(BlockStateProperties.FACING)
        rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE, rotationAxis()).step())

        blockOrientation = getBlockStateOrientation(facing)

        val top = ClockworkPartials.BEARING_TOP_FLAP

        topInstance = instancerProvider().instancer<OrientedInstance>(InstanceTypes.ORIENTED, Models.partial(top))
            .createInstance()

        topInstance.position(visualPosition)
            .rotation(blockOrientation)
            .setChanged()
    }

    override fun beginFrame(ctx: DynamicVisual.Context) {
        val interpolatedAngle = blockEntity!!.getInterpolatedAngle(ctx.partialTick() - 1)
        val rot = rotationAxis.rotationDegrees(interpolatedAngle)

        rot.mul(blockOrientation)

        topInstance.rotation(rot)
            .setChanged()
    }

    override fun updateLight(partialTick: Float) {
        super.updateLight(partialTick)
        relight(topInstance)
    }

    override fun _delete() {
        super._delete()
        topInstance.delete()
    }

    override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
        super.collectCrumblingInstances(consumer)
        consumer.accept(topInstance)
    }

    companion object {
        fun getBlockStateOrientation(facing: Direction): Quaternionf {
            val orientation: Quaternionf

            if (facing.axis.isHorizontal) {
                orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()))
            } else {
                orientation = Quaternionf()
            }

            orientation.mul(Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)))
            return orientation
        }
    }
}