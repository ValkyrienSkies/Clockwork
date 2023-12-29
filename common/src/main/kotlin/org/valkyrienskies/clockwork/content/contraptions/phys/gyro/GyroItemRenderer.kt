package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData

class GyroItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack?,
                        model: CustomRenderedItemModel?,
                        renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext?,
                        ms: PoseStack,
                        buffer: MultiBufferSource?,
                        light: Int,
                        overlay: Int) {

        val model = ClockworkPartials.GYRO_BASE
        val obj = ClockworkPartials.GYRO_OBJ
        renderer.renderSolid(obj.get(), light)

        val innerData = TransformData(Vector3f(0f,0f,0f), Vector3f(0f,0f,0f))
        val data = TransformData(Vector3f(0f,0f,0f), Vector3f(0f,0f,0f))
        RenderUtil.renderCubeMatrix(ms, renderer, innerData, data, 1.5f, light)


    }
}