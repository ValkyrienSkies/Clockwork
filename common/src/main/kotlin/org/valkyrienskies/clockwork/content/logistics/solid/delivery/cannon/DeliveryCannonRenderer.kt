package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotRenderer
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.Random
import kotlin.math.*

class DeliveryCannonRenderer(context: BlockEntityRendererProvider.Context?): FrequencySlotRenderer<DeliveryCannonBlockEntity>(context) {

    val pivot = Vec3(0/16.0,16/16.0,8/16.0)
    val antennaPivot = Vec3(12/16.0,21/16.0,12/16.0)

    override fun renderSafe(
        be: DeliveryCannonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        var antenna = CachedBuffers.partial(ClockworkPartials.CANNON_ANTENNA,be.blockState)
        var base = CachedBuffers.partial(ClockworkPartials.CANNON_BASE,be.blockState)
        var mount = CachedBuffers.partial(ClockworkPartials.CANNON_MOUNT,be.blockState)
        var barrel = CachedBuffers.partial(ClockworkPartials.CANNON_BARREL,be.blockState)

        val xCurrentRotation = be.xRot.getValue(partialTicks).toDouble()
        val yCurrentRotation = be.yRot.getValue(partialTicks).toDouble()

        handleShootingAnim(be, partialTicks)

        val lookDir = VecHelper.rotate(be.blockState.getValue(HorizontalDirectionalBlock.FACING).normal.toJOMLD().toMinecraft(), 0.0, -xCurrentRotation, yCurrentRotation).normalize()

        // X Axis rotation
        mount = rotateCentered(mount, xCurrentRotation)
        base = rotateCentered(base, xCurrentRotation)
        barrel = rotateCentered(barrel, xCurrentRotation)
        antenna = rotateCentered(antenna, xCurrentRotation)

        // Y Axis rotation
        val clientCannonRotOffsetRad = be.clientCannonRotationOffset * 15.0
        val clientAntennaRotOffsetRad = be.clientAntennaRotationOffset * 25.0
        base = rotateToAngle(base,yCurrentRotation + clientCannonRotOffsetRad)
        antenna = rotateToAngle(antenna,yCurrentRotation + clientCannonRotOffsetRad)
        barrel = rotateToAngle(barrel,yCurrentRotation + clientCannonRotOffsetRad)


        antenna = rotateAntenna(antenna,yCurrentRotation + clientAntennaRotOffsetRad)
        barrel.translate(Vec3(0.0,0.0,(be.clientBarrelOffset*2.0)/16.0))

        val vb = buffer.getBuffer(RenderType.cutout())



        render(mount,base,barrel,antenna,ms,vb,light)
        if (!be.midAirStack.isEmpty && be.shootingAtChute != null) {

            if (!be.fired) {
                for (i in 0..9) {
                    val r = Random()
                    val sX: Double = lookDir.x * .01f
                    val sY: Double = (lookDir.y + 1) * .01f
                    val sZ: Double = lookDir.z * .01f
                    val rX = r.nextFloat() - sX * 40f
                    val rY = r.nextFloat() - sY * 40f
                    val rZ = r.nextFloat() - sZ * 40f
                    be.level!!.addParticle(ParticleTypes.CLOUD, be.realPos.x- 0.5 - (lookDir.x*2.0) + rX, pivot.y + be.realPos.y + 1 + rY, be.realPos.z - 0.5 - (lookDir.z*2.0) + rZ, sX, sY, sZ)
                }
                be.fired = true
            }

            // Item Render code
            val realChute = ClockworkUtils.getRealPos(Minecraft.getInstance().level!!, be.shootingAtChute!!)
            val og = be.realPos.lerp(realChute, (be.distance.getValue(partialTicks)/be.distance.chaseTarget).toDouble())
            val y = be.getParabolaY(og)
            be.clientItemRotation+=partialTicks



            renderItem(Vec3(og.x,y,og.z),be,light,overlay,buffer, ms)


        } else {
            be.fired = false
            be.clientItemRotation=0.0
        }
    }

    fun handleShootingAnim(be: DeliveryCannonBlockEntity, partialTicks: Float) {
        if (!be.midAirStack.isEmpty) {
            be.clientShotProgress = Mth.clamp(be.clientShotProgress + partialTicks, 0.0, 12.0)

            if (be.clientShotProgress<=4.0) {

                be.clientAntennaRotationOffset = EaseHelper.easeOutElastic(be.clientShotProgress.toFloat()/4f)
                be.clientCannonRotationOffset = EaseHelper.easeOutQuad(be.clientShotProgress.toFloat()/4f)
            }
            else if (be.clientShotProgress <= 8.0){
                be.clientAntennaRotationOffset = 0.5f-(EaseHelper.easeOutElastic((be.clientShotProgress.toFloat()-5f)/7f))
                be.clientCannonRotationOffset = 1f - EaseHelper.easeInOutSine((be.clientShotProgress.toFloat()-5f)/7f)
            } else {
                be.clientAntennaRotationOffset = Mth.lerp(partialTicks/2f,be.clientAntennaRotationOffset,0f)
                be.clientCannonRotationOffset = 1f - EaseHelper.easeInOutSine((be.clientShotProgress.toFloat()-5f)/7f)
            }
            if (be.clientShotProgress <= 3.0) {
                be.clientBarrelOffset = EaseHelper.easeOutOvershoot(be.clientShotProgress.toFloat()/3f)
            } else {
                be.clientBarrelOffset = 1f - EaseHelper.easeInOutQuad((be.clientShotProgress.toFloat()-4f)/8f)

            }

        } else {
            be.clientShotProgress = 0.0
            be.clientBarrelOffset = 0.0f
            be.clientAntennaRotationOffset = 0.0f
            be.clientCannonRotationOffset = 0.0f
        }
    }

    fun renderItem(launchedItemPos: Vec3, be: DeliveryCannonBlockEntity, light: Int, overlay: Int, buffer: MultiBufferSource, ms: PoseStack) {

        val new: PoseStack
        if (be.isVirtual) new = ms
        else new = PoseStack()

        val msr = TransformStack.of(new)
        val cam = Minecraft.getInstance().gameRenderer.mainCamera

        new.pushPose()
        if (be.isVirtual) msr.translate(launchedItemPos.subtract(be.realPos.toMinecraft()).add(0.5,1.25,0.5))
        else {
            msr.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(cam.xRot.toDouble()), 1f, 0f, 0f)))
            msr.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(cam.yRot + 180.0), 0f, 1f, 0f)))
            msr.translate(-cam.position.x,-cam.position.y,-cam.position.z)
            msr.translate(launchedItemPos.x,launchedItemPos.y+0.25,launchedItemPos.z)
        }



        val itemRotOffset = VecHelper.voxelSpace(0.0, 3.0, 0.0)
        msr.translate(itemRotOffset)
        msr.rotateYDegrees(be.clientItemRotation.toFloat()*3f)
        msr.rotateXDegrees(be.clientItemRotation.toFloat()*3f)
        msr.translateBack(itemRotOffset)
        Minecraft.getInstance()
            .itemRenderer
            .renderStatic(
                be.midAirStack,
                ItemDisplayContext.GROUND,
                light,
                overlay,
                new,
                buffer,
                be.level,
                0
            )
        new.popPose()
    }

    fun rotateToAngle(superByteBuffer: SuperByteBuffer, angle: Double): SuperByteBuffer {
        var buffer = superByteBuffer.translate(pivot);
        buffer = buffer.rotate(AngleHelper.rad(angle), Direction.EAST)
        buffer = buffer.translate(pivot.scale(-1.0))
        return buffer
    }

    // doing it like this is easier than using AngleHelper.rad()
    fun rotateCentered(buffer: SuperByteBuffer, angle: Double): SuperByteBuffer {

        return buffer.rotateCentered(((-angle - 90.0) / 180.0 * Math.PI).toFloat(), Direction.UP)
    }

    fun rotateAntenna(superByteBuffer: SuperByteBuffer, angle: Double): SuperByteBuffer {
        var buffer = superByteBuffer.translate(antennaPivot);
        buffer = buffer.rotate(AngleHelper.rad(angle), Direction.WEST)
        buffer = buffer.translate(antennaPivot.scale(-1.0))
        return buffer
    }

    fun render(mount: SuperByteBuffer, base: SuperByteBuffer, barrel: SuperByteBuffer, antenna: SuperByteBuffer, ms: PoseStack, vb: VertexConsumer, light: Int) {
        mount.light<SuperByteBuffer>(light).renderInto(ms,vb)
        base.light<SuperByteBuffer>(light).renderInto(ms,vb)
        barrel.light<SuperByteBuffer>(light).renderInto(ms,vb)
        antenna.light<SuperByteBuffer>(light).renderInto(ms,vb)
    }


    override fun shouldRenderOffScreen(blockEntity: DeliveryCannonBlockEntity): Boolean {
        return true
    }



    companion object {



        // This function solves a parabola using 3 points. Z is the value that gets fed into the resulting quadratic
        fun parabola(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double,  z:Double): Double {
            val denom = (x1 - x2) * (x1 - x3) * (x2 - x3)
            val A = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom
            val B = (x3 * x3 * (y1 - y2) + x2 * x2 * (y3 - y1) + x1 * x1 * (y2 - y3)) / denom
            val C = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom


            return A*z.pow(2) + B*z + C
        }

        fun getThirdPoint(start: Vector3d, end: Vector3d): Vector3d {
            val lerped = start.lerp(end,0.5)
            return Vector3d(lerped.x, end.y + 5, lerped.z)
        }




        fun euler_angle(x: Double,y: Double): Double {
            val rad = atan(y/x)   // arcus tangent in radians
            var deg = rad*180/Math.PI  // converted to degrees
            if (x<0) deg += 180        // fixed mirrored angle of arctan
            val eul = (270+deg)%360    // folded to [0,360) domain
            return eul
        }



        fun turn(currentRotation: Double, targetRotation: Double, turnSpeed: Double): Pair<Double, Boolean> {

            var shouldLerp = true
            var rotation = currentRotation

            if (360+rotation-targetRotation<abs(targetRotation-rotation)) {

                rotation -=  turnSpeed
                if (rotation<0) {
                    rotation += 360
                    shouldLerp = false
                }
            }
            else if (360-rotation+targetRotation<abs(targetRotation-rotation)) {

                rotation +=  turnSpeed
                if (rotation>=360) {
                    rotation -= 360
                    shouldLerp = false
                }
            }
            else
            rotation +=  Mth.clamp(targetRotation-rotation, -turnSpeed, turnSpeed)
            return Pair(rotation,shouldLerp)
        }
    }
}
