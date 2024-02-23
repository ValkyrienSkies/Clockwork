package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.jozufozu.flywheel.api.Material
import com.jozufozu.flywheel.api.MaterialManager
import com.jozufozu.flywheel.api.instance.DynamicInstance
import com.jozufozu.flywheel.core.PartialModel
import com.jozufozu.flywheel.core.materials.model.ModelData
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import com.simibubi.create.content.kinetics.base.BackHalfShaftInstance
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.mod.common.util.toMinecraft


class SmartPropellerInstance(modelManager: MaterialManager?, blockEntity: SmartPropellerBearingBlockEntity) :
    BackHalfShaftInstance<SmartPropellerBearingBlockEntity>(modelManager, blockEntity), DynamicInstance {

    val bearing: SmartPropellerBearingBlockEntity = blockEntity
    private val topData: ModelData

    private val pistonNWData: ModelData
    private val pistonNEData: ModelData
    private val pistonSWData: ModelData
    private val pistonSEData: ModelData

    private val rotationAxis: Vector3f
    private val blockOrientation: Quaternionf

    init {
        val facing = blockState.getValue(BlockStateProperties.FACING)
        rotationAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis).step()
        bearing.blockNormalVector = Vec3(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())
        blockOrientation = getBlockStateOrientation(facing)

        val top: PartialModel = ClockworkPartials.SMART_PROP_TOP
        val pistonNW: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NW
        val pistonNE: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NE
        val pistonSW: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SW
        val pistonSE: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SE

        val mat: Material<ModelData> = transformMaterial
        topData = mat.getModel(top, blockState).createInstance()
        pistonNWData = mat.getModel(pistonNW, blockState).createInstance()
        pistonNEData = mat.getModel(pistonNE, blockState).createInstance()
        pistonSWData = mat.getModel(pistonSW, blockState).createInstance()
        pistonSEData = mat.getModel(pistonSE, blockState).createInstance()
    }

    override fun beginFrame() {
        val interpolatedAngle: Float = bearing.getInterpolatedAngle(AnimationTickHolder.getPartialTicks() - 1)
        val rotQuaternion = Quaternionf(0.0F, 0.0F, 0.0F, 1.0F)

        rotQuaternion.mul(bearing.tiltQuaternion)

        val quat = rotationAxis.rotationDegrees(interpolatedAngle)
        rotQuaternion.mul(quat.i(), quat.j(), quat.k(), quat.r())

        rotQuaternion.mul(blockOrientation)

        val matrices = PoseStack()
        val transformStack: TransformStack = TransformStack.cast(matrices)

        transformStack.translate(instancePosition)
        transformStack.centre()
        transformStack.pushPose()
        transformStack.translate(bearing.blockNormalVector!!.scale(0.1))
        transformStack.multiply(bearing.tiltQuaternion.toMinecraft())
        transformStack.translate(bearing.blockNormalVector!!.scale(-0.1))
        transformStack.multiply(blockOrientation.toMinecraft())

        transformStack.multiply(rotationAxis.rotationDegrees(interpolatedAngle))

        transformStack.unCentre()
        topData.setTransform(matrices)
        transformStack.popPose()


        transformStack.pushPose()
        //transformStack.centre()
        transformStack.unCentre()
        pistonNWData.setTransform(matrices)
        pistonNEData.setTransform(matrices)
        pistonSWData.setTransform(matrices)
        pistonSEData.setTransform(matrices)
        //transformStack.unCentre()
        transformStack.centre()
        transformStack.popPose()
    }

    override fun updateLight() {
        super.updateLight()
        relight(pos, topData)
        relight(pos, pistonNEData)
        relight(pos, pistonNWData)
        relight(pos, pistonSEData)
        relight(pos, pistonSWData)
    }

    override fun remove() {
        super.remove()
        topData.delete()
        pistonNEData.delete()
        pistonNWData.delete()
        pistonSEData.delete()
        pistonSWData.delete()
    }

    companion object {
        fun getBlockStateOrientation(facing: Direction): Quaternionf {
            val quaternion = Quaternionf(0.0F, 0.0F, 0.0F, 1.0F).toMinecraft()
            val rot = Vector3f.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing))
            quaternion.mul(rot)
            return Quaternionf(quaternion.i(), quaternion.j(), quaternion.k(), quaternion.r())
        }
    }
}