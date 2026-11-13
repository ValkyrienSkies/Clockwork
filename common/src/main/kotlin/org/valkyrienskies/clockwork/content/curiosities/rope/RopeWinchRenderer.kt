package org.valkyrienskies.clockwork.content.curiosities.rope

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials
import kotlin.math.PI
import java.util.WeakHashMap

class RopeWinchRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<RopeWinchBlockEntity>(context) {
    private val spools = WeakHashMap<RopeWinchBlockEntity, RopeWinchAnimation>()

    override fun getRenderedBlockState(be: RopeWinchBlockEntity) =
        shaft(be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))

    override fun shouldRenderOffScreen(be: RopeWinchBlockEntity) = true
    override fun getViewDistance() = 192

    override fun renderSafe(be: RopeWinchBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        renderRotatingKineticBlock(be, getRenderedBlockState(be), ms, buffers.getBuffer(RenderType.solid()), light)
        val link = RopeKnotRenderer.winchLink(be.blockPos)
        val tint = link?.let { RopeKnotRenderer.stressColor(it, partialTicks) } ?: 0xFFFFFF
        val axis = be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)
        val angle = if (link != null) {
            val time = (be.level?.gameTime ?: 0L).toDouble() + partialTicks
            val spool = spools.getOrPut(be) { RopeWinchAnimation(link.length) }
            (-spool.sample(link.length, time) * 3.2 % (2 * PI)).toFloat()
        } else {
            spools.remove(be)
            getAngleForBe(be, be.blockPos, axis) * if (axis == Direction.Axis.Z) -1f else 1f
        }
        ms.pushPose()
        if (axis == Direction.Axis.Z) {
            ms.translate(0.5, 0.5, 0.5)
            ms.mulPose(Axis.YP.rotationDegrees(90f))
            ms.translate(-0.5, -0.5, -0.5)
        }
        val buffer = buffers.getBuffer(RenderType.cutoutMipped())
        CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_DRUM, be.blockState)
            .rotateCentered(angle, Direction.EAST).light<SuperByteBuffer>(light)
            .overlay<SuperByteBuffer>(overlay).color<SuperByteBuffer>(tint).renderInto(ms, buffer)
        if (link != null) {
            CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_ROPE, be.blockState)
                .rotateCentered(angle, Direction.EAST).light<SuperByteBuffer>(light)
                .overlay<SuperByteBuffer>(overlay).color<SuperByteBuffer>(tint).renderInto(ms, buffer)
            CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_LEAD, be.blockState)
                .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).color<SuperByteBuffer>(tint).renderInto(ms, buffer)
        }
        ms.popPose()
    }
}

internal class RopeWinchAnimation(length: Double) {
    private var from = length
    private var target = length
    private var changedAt = 0.0

    fun sample(length: Double, time: Double): Double {
        val progress = ((time - changedAt) / 2.0).coerceIn(0.0, 1.0)
        val current = from + (target - from) * progress
        if (length != target) {
            from = current
            target = length
            changedAt = time
        }
        return current
    }
}
