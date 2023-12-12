package org.valkyrienskies.clockwork.content.contraptions.phys.gyro;


import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.core.Direction;

class GyroInstance(materialManager: MaterialManager, blockEntity: GyroBlockEntity) :
    KineticBlockEntityInstance<GyroBlockEntity>(materialManager, blockEntity), DynamicInstance {

    private val shaft: RotatingData = setup(getRotatingMaterial().getModel(shaft()).createInstance())
    private val wheel: ModelData = getTransformMaterial().getModel(blockState).createInstance()
    private var lastAngle: Float = Float.NaN

    init {
        animate(blockEntity.angle)
    }

    override fun beginFrame() {
        val partialTicks: Float = AnimationTickHolder.getPartialTicks()

        val speed: Float = blockEntity.visualSpeed.getValue(partialTicks) * 3 / 10f
        val angle: Float = blockEntity.angle + speed * partialTicks

        if (Math.abs(angle - lastAngle) < 0.001) {
            return
        }

        animate(angle)

        lastAngle = angle
    }

    private fun animate(angle: Float) {
        val ms = PoseStack()
        val msr = TransformStack.cast(ms)

        msr.translate(instancePosition)
        msr.centre().rotate(Direction.get(Direction.AxisDirection.POSITIVE, axis), AngleHelper.rad(angle.toDouble())).unCentre()

        wheel.setTransform(ms)
    }

    override fun update() {
        updateRotation(shaft)
    }

    override fun updateLight() {
        relight(pos, shaft, wheel)
    }

    override fun remove() {
        shaft.delete()
        wheel.delete()
    }
}
