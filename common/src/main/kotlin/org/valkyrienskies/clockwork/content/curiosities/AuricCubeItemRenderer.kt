package org.valkyrienskies.clockwork.content.curiosities

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData

class AuricCubeItemRenderer(val renderMatrix: Boolean) : CustomRenderedItemModelRenderer() {
    override fun render(
            stack: ItemStack,
            model: CustomRenderedItemModel?,
            renderer: PartialItemModelRenderer?,
            transformType: ItemDisplayContext?,
            ms: PoseStack,
            buffer: MultiBufferSource,
            light: Int,
            overlay: Int
    ) {

        ms.pushPose()
        val data = TransformData(Vector3f(0f, -0.65f, 0f), Vector3f(35f, -25f, 0f))

        if (renderMatrix) {
            var innerData = TransformData(Vector3f(0f, -0.65f, 0f), Vector3f(35f, -25f, 0f))
            RenderUtil.renderCubeMatrix(ms, renderer!!, innerData, data, 2.5f, light)
        } else {
            RenderUtil.renderCube(ms, renderer!!, data, 2.5f, light)
        }

        ms.popPose()
    }
}