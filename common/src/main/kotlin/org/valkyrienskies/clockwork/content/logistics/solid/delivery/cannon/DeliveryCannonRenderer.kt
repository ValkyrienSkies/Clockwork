package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.schematics.cannon.SchematicannonRenderer
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
import kotlin.math.max
import kotlin.math.min

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

        val antenna = CachedBufferer.partial(ClockworkPartials.CANNON_ANTENNA,be.blockState)
        val base = CachedBufferer.partial(ClockworkPartials.CANNON_BASE,be.blockState)
        val mount = CachedBufferer.partial(ClockworkPartials.CANNON_MOUNT,be.blockState)
        val barrel = CachedBufferer.partial(ClockworkPartials.CANNON_BARREL,be.blockState)

        val vb = buffer.getBuffer(RenderType.cutout())

        renderBufferNeutral(be,antenna,ms,vb,light)
        renderBufferNeutral(be,base,ms,vb,light)
        renderBufferNeutral(be,mount,ms,vb,light)
        renderBufferNeutral(be,barrel,ms,vb,light)



        val msr = TransformStack.cast(ms)


        if (!be.transportStack.isEmpty) {
            be.rotate+=1

            val startVec = blockToVec(be.blockPos).add(Vec3(0.0,1.0,0.0))
            val endVec = blockToVec(be.location).add(Vec3(0.0,0.5,0.0))

            var delta = 0.0
            if (endVec.y>startVec.y) delta = min((endVec.y-startVec.y)/30+0.51,0.85)
            else delta = max(0.49-(startVec.y-endVec.y)/30,0.15)
            val og: Vec3 = be.last.lerp(blockToVec(be.blockPos).lerp(blockToVec(be.location),be.progress),partialTicks.toDouble())


            var y = 0.0
            if (startVec.x-endVec.x>startVec.z-endVec.z) y = Parabola(startVec.x,startVec.y,endVec.x,endVec.y,startVec.lerp(endVec,delta).x,og.x)
            else y = Parabola(startVec.z,startVec.y,endVec.z,endVec.y,startVec.lerp(endVec,delta).z,og.z)



            println(be.progress)
            println(y)
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




    fun blockToVec(pos: BlockPos): Vec3 {
        return Vec3(pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble())
    }

    fun rotateBufferTowards(buffer: SuperByteBuffer, target: Direction): SuperByteBuffer {
        return buffer.rotateCentered(Direction.UP, ((-target.toYRot() - 90) / 180 * Math.PI).toFloat())
    }

    fun renderBufferNeutral(be: DeliveryCannonBlockEntity, bf: SuperByteBuffer, ms: PoseStack?, vb: VertexConsumer, light: Int) {
        rotateBufferTowards(bf,be.blockState.getValue(HorizontalDirectionalBlock.FACING)).light(light).renderInto(ms,vb)
    }

    companion object {
        // This function solves a parabola using 2 points and the X of the vertex . Z is the value that gets fed into the resulting quadratic
        fun Parabola(x1: Double,y1: Double,x2: Double,y2: Double,m: Double, z:Double): Double {
            val a = (y1-y2)/(x1*x1-x2*x2-2*m*x1+2*m*x2)
            val b = -2*a*m
            val c = y1-b*x1-a*x1*x1

            return a*z*z+b*z+c
        }
    }
}