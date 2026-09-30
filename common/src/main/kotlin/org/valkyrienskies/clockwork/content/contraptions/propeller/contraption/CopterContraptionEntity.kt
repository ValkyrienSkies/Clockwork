package org.valkyrienskies.clockwork.content.contraptions.propeller.contraption

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IControlContraption
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkEntities
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerTransform
import org.valkyrienskies.clockwork.util.MathFunctions

class CopterContraptionEntity(type: EntityType<*>, world: Level?) : ControlledContraptionEntity(type, world) {

    var tiltQuaternion: Quaternionf = Quaternionf(0.0F, 0.0F, 0.0F, 1.0F)
    var superDirection = Direction.UP

    fun setControllerPos(controllerPos: BlockPos) {
        this.controllerPos = controllerPos
    }

    override fun applyRotation(localPos: Vec3, partialTicks: Float): Vec3 {
        var newLocalPos: Vec3 = localPos
        newLocalPos = VecHelper.rotate(newLocalPos, getAngle(partialTicks).toDouble(), rotationAxis)
        newLocalPos = MathFunctions.reverseRotateVecWithQuat(newLocalPos, tiltQuaternion)
        return newLocalPos
    }

    override fun reverseRotation(localPos: Vec3, partialTicks: Float): Vec3 {
        var newLocalPos: Vec3 = localPos
        newLocalPos = MathFunctions.rotateVecWithQuat(newLocalPos, tiltQuaternion)
        newLocalPos = VecHelper.rotate(newLocalPos, -getAngle(partialTicks).toDouble(), rotationAxis)
        return newLocalPos
    }

    override fun applyLocalTransforms(matrixStack: PoseStack?, partialTicks: Float) {
        val angle = getAngle(partialTicks)
        // The contraption already knows its facing before the bearing's first client tick.
        val direction = (contraption as? PropellerContraption)?.facing ?: superDirection
        val baseAxis = Vector3d(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())
        val tilt = Quaterniond(tiltQuaternion).normalize()
        val displacement = PropellerTransform.hubDisplacement(baseAxis, tilt)
        val orientation = PropellerTransform.orientation(baseAxis, tilt, angle.toDouble())

        TransformStack.of(matrixStack)
            .nudge(id)
            .center()
            .translate(displacement.x, displacement.y, displacement.z)
            .rotate(Quaternionf(orientation))
            .uncenter()
    }

    companion object {
        fun create(level: Level?, controller: IControlContraption, contraption: Contraption?): CopterContraptionEntity {
            val entity = CopterContraptionEntity(ClockworkEntities.COPTER_CONTRAPTION.get(), level)
            entity.setControllerPos(controller.blockPosition)
            entity.setContraption(contraption)
            return entity
        }
    }
}
