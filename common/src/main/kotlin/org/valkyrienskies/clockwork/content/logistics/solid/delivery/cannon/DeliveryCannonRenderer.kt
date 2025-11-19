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
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotRenderer
import org.valkyrienskies.clockwork.util.EaseHelper
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

        val mult = if (Minecraft.getInstance().isPaused) 0 else if(be.gunPowderTicks>0) 3 else 1

        val xResult = turn(be.xLastRotation, be.xTargetRotation, 1.0*mult)
        val yResult = turn(be.yLastRotation, be.yTargetRotation, 0.75*mult)


        val xCurrentRotation: Double
        if (xResult.second) xCurrentRotation = Mth.lerp(partialTicks.toDouble(), be.xLastRotation, xResult.first)
        else xCurrentRotation = xResult.first

        val yCurrentRotation: Double
        if (xResult.second) yCurrentRotation = Mth.lerp(partialTicks.toDouble(), be.yLastRotation, yResult.first)
        else yCurrentRotation = yResult.first



        handleShootingAnim(be, partialTicks)

        val lookDir = VecHelper.rotate(be.blockState.getValue(HorizontalDirectionalBlock.FACING).normal.toJOMLD().toMinecraft(), 0.0, -xCurrentRotation, yCurrentRotation).normalize()

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
        if (!be.transportStack.isEmpty && be.maxProgress > 0) {


            if (!Minecraft.getInstance().isPaused) be.clientProgress=min(be.clientProgress+partialTicks.toDouble()/3.0,be.maxProgress)


            if (!be.didParticles) {
                for (i in 0..9) {
                    val r = Random()
                    val sX: Double = lookDir.x * .01f
                    val sY: Double = (lookDir.y + 1) * .01f
                    val sZ: Double = lookDir.z * .01f
                    val rX = r.nextFloat() - sX * 40f
                    val rY = r.nextFloat() - sY * 40f
                    val rZ = r.nextFloat() - sZ * 40f
                    be.level!!.addParticle(ParticleTypes.CLOUD, be.getRealPos().x - 0.5 - (lookDir.x*2.0) + rX, pivot.y + be.getRealPos().y + 1 + rY, be.getRealPos().z - 0.5 - (lookDir.z*2.0) + rZ, sX, sY, sZ)
                }
                be.didParticles = true
            }

            // Item Render code
            val og: Vec3 = be.getRealPos().lerp(be.realLocation,be.clientProgress/be.maxProgress)
            val y = getParabolaY(be,og)
            be.itemRotation+=partialTicks


            renderItem(Vec3(og.x,y,og.z),be,light,overlay,buffer, ms)


        } else {
            be.didParticles = false
            be.clientProgress = 0.0
            be.itemRotation=0.0
        }
    }

    fun handleShootingAnim(be: DeliveryCannonBlockEntity, partialTicks: Float) {
        if (be.maxProgress > 0) {
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
        if (be.ponder) new = ms
        else new = PoseStack()

        val msr = TransformStack.of(new)
        val cam = Minecraft.getInstance().gameRenderer.mainCamera

        new.pushPose()
        if (be.ponder) msr.translate(launchedItemPos.subtract(be.getRealPos()).add(0.5,1.25,0.5))
        else {
            msr.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(cam.xRot.toDouble()), 1f, 0f, 0f)))
            msr.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(cam.yRot + 180.0), 0f, 1f, 0f)))
            msr.translate(-cam.position.x,-cam.position.y,-cam.position.z)
            msr.translate(launchedItemPos.x,launchedItemPos.y+0.25,launchedItemPos.z)
        }



        val itemRotOffset = VecHelper.voxelSpace(0.0, 3.0, 0.0)
        msr.translate(itemRotOffset)
        msr.rotateYDegrees(be.itemRotation.toFloat()*3f)
        msr.rotateXDegrees(be.itemRotation.toFloat()*3f)
        msr.translateBack(itemRotOffset)
        Minecraft.getInstance()
            .itemRenderer
            .renderStatic(
                be.transportStack,
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

        fun getThirdPoint(startVec3: Vec3, endVec3: Vec3): Vec3 {
            val lerped = startVec3.lerp(endVec3,0.5)
            return Vec3(lerped.x, endVec3.y + 5, lerped.z)
        }


        fun getParabolaY(DBe: DeliveryCannonBlockEntity, vec: Vec3): Double {
            val startVec3 = DBe.getRealPos()
            val endVec3 = DBe.realLocation
            val vertVec3 = getThirdPoint(DBe.getRealPos(), DBe.realLocation)

            var sX = startVec3.x
            var eX = endVec3.x
            var vX = vertVec3.x
            var iX = vec.x

            // Picks an axis to use for the parabola.
            if (abs(endVec3.x-startVec3.x) < abs(endVec3.z-startVec3.z)) {

                sX = startVec3.z
                eX = endVec3.z
                vX = vertVec3.z
                iX = vec.z
            }

            return parabola(sX,startVec3.y,eX,endVec3.y,vX,vertVec3.y, iX)

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
