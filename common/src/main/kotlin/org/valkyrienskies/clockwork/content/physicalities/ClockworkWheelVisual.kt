package org.valkyrienskies.clockwork.content.physicalities

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.visual.DynamicVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.Direction
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.util.function.Consumer
import kotlin.math.abs

class ClockworkWheelVisual(context: VisualizationContext, blockEntity : KineticBlockEntity, partialTick : Float) : KineticBlockEntityVisual<KineticBlockEntity>(context, blockEntity, partialTick), SimpleDynamicVisual {

	val wheel: TransformedInstance
	var lastAngle = Double.NaN

    val baseTransform = Matrix4f()

    init {
        val axis = rotationAxis()

        wheel = instancerProvider().instancer<TransformedInstance>(
            InstanceTypes.TRANSFORMED,
            Models.block(blockState)
        )
            .createInstance()


        val align = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)

        wheel.translate(visualPosition)
            .center()
            .rotate(
                Quaternionf().rotateTo(
                    0f,
                    1f,
                    0f,
                    align.stepX.toFloat(),
                    align.stepY.toFloat(),
                    align.stepZ.toFloat()
                )
            )

        baseTransform.set(wheel.pose)

        animate((blockEntity as IClockworkWheelBE).angle)
    }

	@Override
	override fun beginFrame(ctx: DynamicVisual.Context) {

        assert(blockEntity is IClockworkWheelBE) { "ClockworkWheelVisual used with BE that doesn't implement IClockworkWheel" }

		var partialTicks = ctx.partialTick();

		var speed = (blockEntity as IClockworkWheelBE).visualSpeed.getValue(partialTicks) * 3 / 10f;
		var angle = (blockEntity as IClockworkWheelBE).angle + speed * partialTicks;

		if (abs(angle - lastAngle) < 0.001)
			return;

		animate(angle);

		lastAngle = angle;
	}

	private fun animate(angle: Double) {
		wheel.setTransform(baseTransform)
			.rotateY(AngleHelper.rad(angle))
			.uncenter()
			.setChanged();
	}


    override fun updateLight(partialTick: Float) {
        relight(wheel)
    }

    override fun _delete() {
        wheel.delete()
    }

    override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
        consumer.accept(wheel)
    }
}
