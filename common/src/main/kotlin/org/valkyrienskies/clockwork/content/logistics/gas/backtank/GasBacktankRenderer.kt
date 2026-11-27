package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import dev.architectury.platform.Platform
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.kelvin.api.DuctNodePos

class GasBacktankRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<GasBacktankBlockEntity>(context) {

    override fun renderSafe(
        be: GasBacktankBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val blockState = be.blockState
        val facing = blockState.getValue(HorizontalDirectionalBlock.FACING)
        val fill = be.gaugeNeedle.getValue(partialTicks)
        val renderTime = be.level?.let { AnimationTickHolder.getRenderTime(it) } ?: AnimationTickHolder.getRenderTime()
        val phase = ((be.blockPos.x * 31 + be.blockPos.y * 17 + be.blockPos.z * 13) and 0xFF) * 0.1f

        renderNeedle(
            blockState = blockState,
            facing = facing,
            fill = fill,
            pressure = be.clientPressure,
            renderTime = renderTime,
            phase = phase,
            ms = ms,
            vc = buffer.getBuffer(RenderType.cutout()),
            light = light,
            overlay = overlay
        )
    }

    companion object {
        fun getClientPressure(level: Level, pos: DuctNodePos): Double? {
            val kelvin = if (level is PonderLevel || (Minecraft.getInstance().isLocalServer && Platform.isFabric())) {
                ClockworkMod.getKelvin(level)
            } else {
                ClockworkModClient.getKelvin()
            }

            val info = kelvin.nodeInfo[pos] ?: return null
            return info.currentPressure
        }

        fun renderNeedle(
            blockState: BlockState,
            facing: Direction,
            fill: Float,
            pressure: Double,
            renderTime: Float,
            phase: Float = 0f,
            ms: PoseStack,
            vc: VertexConsumer,
            light: Int,
            overlay: Int? = null
        ) {
            val needle = CachedBuffers.partial(ClockworkPartials.GAS_BACKTANK_NEEDLE, blockState)
            val facingYRot = GasBacktankGauge.facingYRotationDegrees(facing)
            val needleAngle = GasBacktankGauge.needleAngleDegrees(fill, pressure, renderTime, phase)

            needle.center()
                .rotateYDegrees(facingYRot)
                .uncenter()
                .translate(0f, GasBacktankGauge.NEEDLE_PIVOT_Y, GasBacktankGauge.NEEDLE_PIVOT_Z)
                .rotateXDegrees(needleAngle)
                .translate(0f, -GasBacktankGauge.NEEDLE_PIVOT_Y, -GasBacktankGauge.NEEDLE_PIVOT_Z)
                .light<SuperByteBuffer>(light)
            if (overlay != null) {
                needle.overlay<SuperByteBuffer>(overlay)
            }
            needle.renderInto(ms, vc)
        }
    }
}
