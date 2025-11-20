package org.valkyrienskies.clockwork.platform


import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.ItemDisplayContext

@Suppress("unused")
object ItemTransformHooks {
    @ExpectPlatform
    @JvmStatic
    fun applyItemTransform(
        poseStack: PoseStack?,
        model: BakedModel?,
        context: ItemDisplayContext,
        applyLeftHandTransform: Boolean
    ) {
        throw AssertionError()
    }
}
