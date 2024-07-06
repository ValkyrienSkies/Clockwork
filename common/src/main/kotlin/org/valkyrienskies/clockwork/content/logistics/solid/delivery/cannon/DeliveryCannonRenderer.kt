package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.Mth
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotRenderer
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min

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

        var antenna = CachedBufferer.partial(ClockworkPartials.CANNON_ANTENNA,be.blockState)
        var base = CachedBufferer.partial(ClockworkPartials.CANNON_BASE,be.blockState)
        var mount = CachedBufferer.partial(ClockworkPartials.CANNON_MOUNT,be.blockState)
        var barrel = CachedBufferer.partial(ClockworkPartials.CANNON_BARREL,be.blockState)

        val xResult = turn(be.xLastRotation, be.xTargetRotation, 1.0)
        val yResult = turn(be.yLastRotation, be.yTargetRotation, 0.75)

        val xCurrentRotation: Double
        if (xResult.second) xCurrentRotation = Mth.lerp(partialTicks.toDouble(), be.xLastRotation, xResult.first)
        else xCurrentRotation = xResult.first

        val yCurrentRotation = Mth.lerp(partialTicks.toDouble(), be.yLastRotation, yResult.first)

        handleShootingAnim(be, partialTicks)

        val lookDir = VecHelper.rotate(be.blockState.getValue(HorizontalDirectionalBlock.FACING).normal.toJOMLD().toMinecraft(), be.yRotation, be.xRotation, 0.0).normalize()

        // X Axis rotation
        mount = rotateCentered(mount, xCurrentRotation)
        base = rotateCentered(base, xCurrentRotation)
        barrel = rotateCentered(barrel, xCurrentRotation)
        antenna = rotateCentered(antenna, xCurrentRotation)

        // Y Axis rotation
        var clientCannonRotOffsetRad = be.clientCannonRotationOffset * 15.0
        var clientAntennaRotOffsetRad = be.clientAntennaRotationOffset * 25.0
        base = rotateToAngle(base,yCurrentRotation + clientCannonRotOffsetRad)
        antenna = rotateToAngle(antenna,yCurrentRotation + clientCannonRotOffsetRad)
        barrel = rotateToAngle(barrel,yCurrentRotation + clientCannonRotOffsetRad)

        antenna = rotateAntenna(antenna,yCurrentRotation + clientAntennaRotOffsetRad)
        barrel.translate(Vec3(0.0,0.0,(be.clientBarrelOffset*2.0)/16.0))
        be.xLastRotation = xCurrentRotation
        be.yLastRotation = yCurrentRotation

        val vb = buffer.getBuffer(RenderType.cutout())

        render(mount,base,barrel,antenna,ms,vb,light)
        if (!be.transportStack.isEmpty && be.progress > 0) {

            if (!be.didParticles) {
                for (i in 0..9) {
                    val r: java.util.Random = be.level!!.getRandom()
                    val sX: Double = lookDir.x * .01f
                    val sY: Double = (lookDir.y + 1) * .01f
                    val sZ: Double = lookDir.z * .01f
                    val rX = r.nextFloat() - sX * 40f
                    val rY = r.nextFloat() - sY * 40f
                    val rZ = r.nextFloat() - sZ * 40f
                    be.level!!.addParticle(ParticleTypes.CLOUD, be.blockPos.x.toDouble() + (lookDir.x*2.0) + rX, pivot.y + be.blockPos.y.toDouble() + 1.0 + (lookDir.y*2.0) + rY, be.blockPos.z.toDouble() + (lookDir.z*2.0) + rZ, sX, sY, sZ)
                }
                be.didParticles = true
            }

            // Item Render code
            val og: Vec3 = be.itemLastPos.lerp(blockToVec(be.blockPos).lerp(be.realLocation,be.progress),partialTicks.toDouble())
            val y = get_Parabola_Y(be,og)
            be.itemRotation+=partialTicks

            val msr = TransformStack.cast(ms)
            val launchedItemLocation = Vec3(og.x,y,og.z)
            be.itemLastPos = launchedItemLocation
            ms.pushPose()
            msr.translate(launchedItemLocation.subtract(Vec3.atLowerCornerOf(be.blockPos).add(Vec3(-0.5,-0.5,-0.5))))
            val itemRotOffset = VecHelper.voxelSpace(0.0, 3.0, 0.0)
            msr.translate(itemRotOffset)
            msr.rotateY(be.itemRotation*3)
            msr.rotateX(be.itemRotation*3)
            msr.translateBack(itemRotOffset)
            Minecraft.getInstance()
                .itemRenderer
                .renderStatic(
                    be.transportStack,
                    ItemTransforms.TransformType.GROUND,
                    light,
                    overlay,
                    ms,
                    buffer,
                    0
                )
            ms.popPose()
        } else {
            be.didParticles = false
            be.itemLastPos = blockToVec(be.blockPos)
            be.itemRotation=0.0
        }
    }

    fun handleShootingAnim(be: DeliveryCannonBlockEntity, partialTicks: Float) {
        if (be.progress > 0) {
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

    fun rotateToAngle(buffer: SuperByteBuffer, angle: Double): SuperByteBuffer {
        var buffer = buffer.translate(pivot);
        buffer = buffer.rotate(Direction.EAST,AngleHelper.rad(angle))
        buffer = buffer.translate(pivot.scale(-1.0))
        return buffer
    }

    // doing it like this is easier than using AngleHelper.rad()
    fun rotateCentered(buffer: SuperByteBuffer, angle: Double): SuperByteBuffer {

        return buffer.rotateCentered(Direction.UP, ((-angle - 90.0) / 180.0 * Math.PI).toFloat())
    }

    fun rotateAntenna(buffer: SuperByteBuffer, angle: Double): SuperByteBuffer {
        var buffer = buffer.translate(antennaPivot);
        buffer = buffer.rotate(Direction.WEST,AngleHelper.rad(angle))
        buffer = buffer.translate(antennaPivot.scale(-1.0))
        return buffer
    }

    fun render(mount: SuperByteBuffer, base: SuperByteBuffer, barrel: SuperByteBuffer, antenna: SuperByteBuffer, ms: PoseStack, vb: VertexConsumer, light: Int) {
        mount.light(light).renderInto(ms,vb)
        base.light(light).renderInto(ms,vb)
        barrel.light(light).renderInto(ms,vb)
        antenna.light(light).renderInto(ms,vb)
    }


    override fun shouldRenderOffScreen(blockEntity: DeliveryCannonBlockEntity): Boolean {
        return true
    }

    companion object {
        fun blockToVec(pos: BlockPos): Vec3 {
            return Vec3(pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble())
        }

        // This function solves a parabola using 2 points and the X of the vertex . Z is the value that gets fed into the resulting quadratic
        fun parabola(x1: Double, y1: Double, x2: Double, y2: Double, m: Double, z:Double): Double {
            val a = (y1-y2)/(x1*x1-x2*x2-2*m*x1+2*m*x2)
            val b = -2*a*m
            val c = y1-b*x1-a*x1*x1

            return a*z*z+b*z+c
        }

        fun euler_angle(x: Double,y: Double): Double {
            var rad = atan(y/x);   // arcus tangent in radians
            var deg = rad*180/Math.PI;  // converted to degrees
            if (x<0) deg += 180;        // fixed mirrored angle of arctan
            var eul = (270+deg)%360;    // folded to [0,360) domain
            return eul;
        }

        fun get_delta(be: DeliveryCannonBlockEntity): Double {
            val startVec = blockToVec(be.blockPos).add(Vec3(0.0, 0.75, 0.0))
            val endVec = be.realLocation.add(Vec3(0.0, 0.5, 0.0))

            val delta: Double
            if (endVec.y > startVec.y) delta = min((endVec.y - startVec.y) / 30 + 0.51, 0.85)
            else delta = max(0.49 - (startVec.y - endVec.y) / 30, 0.15)

            return delta
        }

        fun get_Parabola_Y(be: DeliveryCannonBlockEntity, input_vector: Vec3): Double {
            val startVec = blockToVec(be.blockPos).add(Vec3(0.0,0.75,0.0))
            val endVec = be.realLocation.add(Vec3(0.0,0.5,0.0))

            val delta = get_delta(be)

            val y: Double
            if (abs(startVec.x-endVec.x)>abs(startVec.z-endVec.z)) y = parabola(startVec.x,startVec.y,endVec.x,endVec.y,startVec.lerp(endVec,delta).x,input_vector.x)
            else y = parabola(startVec.z,startVec.y,endVec.z,endVec.y,startVec.lerp(endVec,delta).z,input_vector.z)

            return y
        }

        fun turn(rotation: Double, targetRotation: Double, turnSpeed: Double): Pair<Double, Boolean> {

            var shouldLerp = true
            var rotation = rotation

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