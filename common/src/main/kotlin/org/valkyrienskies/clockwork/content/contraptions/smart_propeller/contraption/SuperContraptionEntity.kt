package org.valkyrienskies.clockwork.content.contraptions.smart_propeller.contraption

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IControlContraption
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkEntities
import org.valkyrienskies.clockwork.util.MathUtil
import org.valkyrienskies.mod.common.util.toMinecraft


class SuperContraptionEntity(entityTypeIn: EntityType<*>?, worldIn: Level?) :
    ControlledContraptionEntity(entityTypeIn, worldIn) {

    var tiltQuaternion: Quaternionf = Quaternionf(0.0F, 0.0F, 0.0F, 1.0F)
    var superDirection = Direction.UP

    override fun applyRotation(localPos: Vec3, partialTicks: Float): Vec3 {
        var newLocalPos: Vec3 = localPos
        newLocalPos = VecHelper.rotate(newLocalPos, getAngle(partialTicks).toDouble(), rotationAxis)
        newLocalPos = MathUtil.reverseRotateVecWithQuat(newLocalPos, tiltQuaternion)
        return newLocalPos
    }

    fun setControllerPos(controllerPos: BlockPos?) {
        this.controllerPos = controllerPos
    }

    public override fun setContraption(contraption: Contraption?) {
        super.setContraption(contraption)
    }

    override fun reverseRotation(localPos: Vec3, partialTicks: Float): Vec3 {
        var newLocalPos: Vec3 = localPos
        newLocalPos = MathUtil.rotateVecWithQuat(newLocalPos, tiltQuaternion)
        newLocalPos = VecHelper.rotate(newLocalPos, -getAngle(partialTicks).toDouble(), rotationAxis)
        return newLocalPos
    }

    override fun applyLocalTransforms(matrixStack: PoseStack?, partialTicks: Float) {
        val angle = getAngle(partialTicks)
        val axis = getRotationAxis()
        var normal = Vec3(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())
        normal = normal.scale(1 / 16.0)
        val pivotOffset = Vec3(0.0, -0.9, 0.0)

        TransformStack.cast(matrixStack)
            .nudge(id)
            .centre()
            .translate(pivotOffset.x, pivotOffset.y, pivotOffset.z)
            .translate(normal.scale(-1.0))
            .multiply(tiltQuaternion.toMinecraft())
            .translate(normal)
            .translate(-pivotOffset.x, -pivotOffset.y, -pivotOffset.z)
            .rotate(angle.toDouble(), axis)
            .unCentre()
    }

    companion object {
        fun create(level: Level?, controller: IControlContraption, contraption: Contraption?): SuperContraptionEntity {
            val entity = SuperContraptionEntity(ClockworkEntities.SUPER_CONTRAPTION.get(), level)
            entity.setControllerPos(controller.blockPosition)
            entity.setContraption(contraption)
            return entity
        }
    }
}