package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.datafixers.TypeRewriteRule.All
import com.simibubi.create.AllParticleTypes
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData
import com.simibubi.create.foundation.particle.AirParticle
import com.simibubi.create.foundation.particle.AirParticleData
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.EaseHelper
import kotlin.math.abs
import kotlin.math.roundToInt


class AirCompressorRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<AirCompressorBlockEntity>(context) {
    override fun renderSafe(
        be: AirCompressorBlockEntity?,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource?,
        light: Int,
        overlay: Int
    ) {
        //super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val vb = buffer!!.getBuffer(RenderType.cutoutMipped())
        val lightBelow = LevelRenderer.getLightColor(be!!.level, be.blockPos.below())
        val lightAbove = LevelRenderer.getLightColor(be.level, be.blockPos.above())

        // Shaft renderer
        val axis = CachedBufferer.partial(ClockworkPartials.COMPRESSOR_AXIS, be!!.blockState)
        standardKineticRotationTransform(axis,be,lightBelow).renderInto(ms, vb)


        val fabric = CachedBufferer.partial(ClockworkPartials.COMPRESSOR_FABRIC, be.blockState)
        val top = CachedBufferer.partial(ClockworkPartials.COMPRESSOR_TOP, be.blockState)



        val mult = if (Minecraft.getInstance().isPaused) 0 else if (be.clientParticles || !be.isOn) -1 else 1
        be.clientSize += partialTicks*be.speed*mult/1500f
        be.clientSize = Mth.clamp(be.clientSize,0f,1f)

        if (be.clientSize>=1f && !be.clientParticles) {
            be.clientParticles = true
            for (i in 0..36) {
                val r: java.util.Random = be.level!!.getRandom()

                val rX = 0.5 - r.nextFloat()
                val rY = r.nextFloat()
                val rZ = 0.5 - r.nextFloat()
                be.level!!.addParticle(AirParticleData(0.00f,0.005f), be.blockPos.x + 0.5 + rX, be.blockPos.y + 1.6 + rY, be.blockPos.z + 0.5 + rZ, 0.0, 0.05, 0.0)
            }
        }

        if (be.clientSize<=0f) be.clientParticles = false




        val size = EaseHelper.easeInOutQuad(be.clientSize)/2
        fabric.scale(1.0f,0.5f+size,1.0f)
        fabric.translate(0.0,(0.5-size)/2,0.0)
        top.translate(0.0,(size-0.5)*1.5,0.0)

        fabric.light(lightAbove).renderInto(ms,vb)
        top.light(lightAbove).renderInto(ms,vb)
    }


}