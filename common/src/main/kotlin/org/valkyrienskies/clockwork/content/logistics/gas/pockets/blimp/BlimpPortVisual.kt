package org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.base.ShaftVisual
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.visual.DynamicVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.DirectionalBlock
import org.valkyrienskies.clockwork.ClockworkPartials
import java.util.function.Consumer

open class BlimpPortVisual(
    dispatcher: VisualizationContext,
    blockEntity: BlimpPortBlockEntity,
    partialTick: Float
) : ShaftVisual<BlimpPortBlockEntity>(dispatcher, blockEntity, partialTick), SimpleDynamicVisual {

    protected var pointer: TransformedInstance
    protected var settled: Boolean

    protected val xRot: Float
    protected val yRot: Float
    protected val pointerRotationOffset: Int

    init {
        val pointerFace = BlimpPortBlock.getPointerFace(blockState)
        yRot = AngleHelper.horizontalAngle(pointerFace)
        xRot = when (pointerFace) {
            Direction.UP -> 0f
            Direction.DOWN -> 180f
            else -> 90f
        }

        val ductAxis = blockState.getValue(DirectionalBlock.FACING).axis
        val shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity)
        val twist = (ductAxis.isHorizontal && shaftAxis === Direction.Axis.X) || ductAxis.isVertical
        pointerRotationOffset = if (twist) 90 else 0
        settled = false

        pointer = instancerProvider().instancer<TransformedInstance>(
            InstanceTypes.TRANSFORMED,
            Models.partial(ClockworkPartials.VALVE_DUCT_POINTER)
        ).createInstance()

        transformPointer(partialTick)
    }

    override fun beginFrame(ctx: DynamicVisual.Context) {
        if (blockEntity.pointer.settled() && settled) return
        transformPointer(ctx.partialTick())
    }

    private fun transformPointer(partialTick: Float) {
        val value = blockEntity.pointer.getValue(partialTick)
        val pointerRotation = Mth.lerp(value, 0f, -90f)
        settled = (value == 0f || value == 1f) && blockEntity.pointer.settled()

        pointer.setIdentityTransform()
            .translate(visualPosition)
            .center()
            .rotateYDegrees(yRot)
            .rotateXDegrees(xRot)
            .rotateYDegrees(pointerRotationOffset + pointerRotation)
            .uncenter()
            .setChanged()
    }

    override fun updateLight(partialTick: Float) {
        super.updateLight(partialTick)
        relight(pointer)
    }

    override fun _delete() {
        super._delete()
        pointer.delete()
    }

    override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
        super.collectCrumblingInstances(consumer)
        consumer.accept(pointer)
    }
}
