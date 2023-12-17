package org.valkyrienskies.clockwork.util.render

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.renderer.RenderType
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes

object RenderUtil {

    val CRYSTAL_MATRIX = ClockworkMod.asResource("textures/block/empty.png");
    val PURPLE_HUE = ClockworkMod.asResource("textures/block/purple_hue.png")

    fun renderCubeMatrix(
        matrices: PoseStack,
        renderer: PartialItemModelRenderer,
        innerData: TransformData,
        data: TransformData,
        light: Int
    ) {
        renderAndTransform(
            matrices,
            ClockworkPartials.CRYSTAL_INNER,
            RenderType.endPortal(),
            renderer,
            innerData.offset.add(Vector3f(0f, -4.5f / 16.0f, 0f)),
            innerData.rotation,
            light
        )
        renderAndTransform(
            matrices,
            ClockworkPartials.CRYSTAL,
            ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX),
            renderer,
            data.offset.add(Vector3f(0f, -4.5f / 16.0f, 0f)),
            data.rotation,
            light
        )
        renderAndTransform(
            matrices,
            ClockworkPartials.CRYSTAL_OUTER,
            RenderType.entityTranslucent(PURPLE_HUE),
            renderer,
            data.offset.add(Vector3f(0f, -4.5f / 16.0f, 0f)),
            data.rotation,
            light
        )
    }

    fun renderAndTransform(
        matrices: PoseStack,
        model: PartialModel,
        renderType: RenderType,
        renderer: PartialItemModelRenderer,
        offset: Vector3f,
        rotationVec: Vector3f,
        light: Int
    ) {
        matrices.pushPose()
        matrices.translate(-offset.x, -offset.y, -offset.z)
        matrices.mulPose(Axis.YP.rotationDegrees(rotationVec.y))
        matrices.mulPose(Axis.XP.rotationDegrees(rotationVec.x))
        matrices.mulPose(Axis.ZP.rotationDegrees(rotationVec.z))
        matrices.translate(offset.x, offset.y, offset.z)
        renderer.render(model.get(), renderType, light)
        matrices.popPose()
    }
}