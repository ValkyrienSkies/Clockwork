package org.valkyrienskies.clockwork.content.physicalities.reactionwheel

import com.jozufozu.flywheel.api.MaterialManager
import com.jozufozu.flywheel.api.instance.DynamicInstance
import com.jozufozu.flywheel.core.materials.model.ModelData
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.core.Direction
import kotlin.math.abs

class ReactionWheelInstance(materialManager: MaterialManager?, blockEntity: ReactionWheelBlockEntity?) : KineticBlockEntityInstance<ReactionWheelBlockEntity>(
    materialManager, blockEntity
), DynamicInstance {

    protected var shaft: RotatingData? = null
    protected var wheel: ModelData? = null
    protected var lastAngle: Float = Float.NaN

    init {
        shaft = setup(
            rotatingMaterial.getModel(shaft())
                .createInstance()
        )
        wheel = transformMaterial.getModel(blockState)
            .createInstance()

        animate(blockEntity!!.angle.toFloat())
    }

    override fun beginFrame() {
        val partialTicks = AnimationTickHolder.getPartialTicks()

        val speed: Float = blockEntity.clientSpeed.getValue(partialTicks) * 3 / 10f
        val angle = (blockEntity.angle + speed * partialTicks).toFloat()

        if (abs((angle - lastAngle).toDouble()) < 0.001) return

        animate(angle)

        lastAngle = angle
    }

    private fun animate(angle: Float) {
        val ms = PoseStack()
        val msr = TransformStack.cast(ms)

        msr.translate(instancePosition)
        msr.centre()
            .rotate(Direction.get(Direction.AxisDirection.POSITIVE, axis), AngleHelper.rad(angle.toDouble()))
            .unCentre()

        wheel!!.setTransform(ms)
    }

    override fun update() {
        updateRotation(shaft)
    }

    override fun updateLight() {
        relight(pos, shaft, wheel)
    }

    public override fun remove() {
        shaft!!.delete()
        wheel!!.delete()
    }
}