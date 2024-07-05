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
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.FrequencySlotRenderer
import org.valkyrienskies.clockwork.util.EaseHelper
import kotlin.math.*
import kotlin.random.Random

class DeliveryCannonRenderer(context: BlockEntityRendererProvider.Context?): FrequencySlotRenderer<DeliveryCannonBlockEntity>(context) {




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


        // lerp rotation to make it look smoother
        val xCurrentRotation = be.xLastRotation + (be.xRotation-be.xLastRotation) * partialTicks
        val yCurrentRotation = be.yLastRotation + (be.yRotation-be.yLastRotation) * partialTicks

        // X Axis rotation
        // doing it like this is easier than using AngleHelper.rad()
        mount.rotateCentered(Direction.UP, ((-xCurrentRotation - 90) / 180 * Math.PI).toFloat())
        base.rotateCentered(Direction.UP, ((-xCurrentRotation - 90) / 180 * Math.PI).toFloat())
        barrel.rotateCentered(Direction.UP, ((-xCurrentRotation - 90) / 180 * Math.PI).toFloat())
        antenna.rotateCentered(Direction.UP, ((-xCurrentRotation - 90) / 180 * Math.PI).toFloat())

        // Y Axis rotation
        base = rotateToAngle(base,yCurrentRotation)
        antenna = rotateToAngle(antenna,yCurrentRotation)
        barrel = rotateToAngle(barrel,yCurrentRotation)

        be.xLastRotation = xCurrentRotation
        be.yLastRotation = yCurrentRotation

        val vb = buffer.getBuffer(RenderType.cutout())


        if (be.shootingTicks in 1..6) {

            if (be.shootingTicks<=2) barrel=barrel.translate(Vec3(0.0,0.0,be.shootingTicks/16.0))
            else barrel=barrel.translate(Vec3(0.0,0.0,(3-be.shootingTicks/2)/16.0))
        }

        render(mount,base,barrel,antenna,ms,vb,light)
        if (!be.transportStack.isEmpty && be.progress > 0) {

            // Item Render code
            val og: Vec3 = be.last.lerp(blockToVec(be.blockPos).lerp(blockToVec(be.location),be.progress ),partialTicks.toDouble())
            val y = get_Parabola_Y(be,og)
            be.rotate+=partialTicks

            val msr = TransformStack.cast(ms)
            val launchedItemLocation = Vec3(og.x,y,og.z)
            be.last = launchedItemLocation
            ms.pushPose()
            msr.translate(launchedItemLocation.subtract(Vec3.atLowerCornerOf(be.blockPos).add(Vec3(-0.5,-0.5,-0.5))))
            val itemRotOffset = VecHelper.voxelSpace(0.0, 3.0, 0.0)
            msr.translate(itemRotOffset)
            msr.rotateY(be.rotate*3)
            msr.rotateX(be.rotate*3)
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

            be.last = blockToVec(be.blockPos)
            be.rotate=0.0
        }



    }

    val pivot = Vec3(0/16.0,16/16.0,8/16.0)
    fun rotateToAngle(buffer: SuperByteBuffer, angle: Double): SuperByteBuffer {
        var buffer = buffer.translate(pivot);
        buffer = buffer.rotate(Direction.EAST,AngleHelper.rad(angle))
        buffer = buffer.translate(pivot.multiply(-1.0,-1.0,-1.0))
        return buffer
    }

    fun render(mount: SuperByteBuffer, base: SuperByteBuffer, barrel: SuperByteBuffer, antenna: SuperByteBuffer, ms: PoseStack, vb: VertexConsumer, light: Int) {
        mount.light(light).renderInto(ms,vb)
        base.light(light).renderInto(ms,vb)
        barrel.light(light).renderInto(ms,vb)
        antenna.light(light).renderInto(ms,vb)
    }



    fun rotateBufferTowards(buffer: SuperByteBuffer, target: Direction): SuperByteBuffer {
        return buffer.rotateCentered(Direction.UP, ((-target.toYRot() - 90) / 180 * Math.PI).toFloat())
    }

    fun rotatedBuffer(be: DeliveryCannonBlockEntity, bf: SuperByteBuffer, ms: PoseStack?, vb: VertexConsumer, light: Int): SuperByteBuffer {
        return rotateBufferTowards(bf,be.blockState.getValue(HorizontalDirectionalBlock.FACING))
    }

    fun antiRotatedBuffer(be: DeliveryCannonBlockEntity, bf: SuperByteBuffer, ms: PoseStack?, vb: VertexConsumer, light: Int): SuperByteBuffer {
        return rotateBufferTowards(bf,be.blockState.getValue(HorizontalDirectionalBlock.FACING).opposite)
    }



    companion object {
        fun blockToVec(pos: BlockPos): Vec3 {
            return Vec3(pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble())
        }

        // This function solves a parabola using 2 points and the X of the vertex . Z is the value that gets fed into the resulting quadratic
        fun Parabola(x1: Double,y1: Double,x2: Double,y2: Double,m: Double, z:Double): Double {
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
            val endVec = blockToVec(be.location).add(Vec3(0.0, 0.5, 0.0))

            val delta: Double
            if (endVec.y > startVec.y) delta = min((endVec.y - startVec.y) / 30 + 0.51, 0.85)
            else delta = max(0.49 - (startVec.y - endVec.y) / 30, 0.15)

            return delta
        }

        fun get_Parabola_Y(be: DeliveryCannonBlockEntity, input_vector: Vec3): Double {
            val startVec = blockToVec(be.blockPos).add(Vec3(0.0,0.75,0.0))
            val endVec = blockToVec(be.location).add(Vec3(0.0,0.5,0.0))

            val delta = get_delta(be)

            var y = 0.0
            if (abs(startVec.x-endVec.x)>abs(startVec.z-endVec.z)) y = Parabola(startVec.x,startVec.y,endVec.x,endVec.y,startVec.lerp(endVec,delta).x,input_vector.x)
            else y = Parabola(startVec.z,startVec.y,endVec.z,endVec.y,startVec.lerp(endVec,delta).z,input_vector.z)

            return y
        }
    }
}