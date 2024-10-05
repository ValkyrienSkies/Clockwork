package org.valkyrienskies.clockwork.platform


import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType
import net.minecraft.client.resources.model.BakedModel

@Suppress("unused")
object ItemTransformHooks {
    @ExpectPlatform
    @JvmStatic
    fun applyItemTransform(
        poseStack: PoseStack?,
        model: BakedModel?,
        context: TransformType,
        applyLeftHandTransform: Boolean
    ) {
        throw AssertionError()
    }
}