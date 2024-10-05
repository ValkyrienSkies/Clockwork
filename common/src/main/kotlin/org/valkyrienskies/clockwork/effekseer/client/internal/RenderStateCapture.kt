package org.valkyrienskies.clockwork.effekseer.client.internal

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Matrix4f
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack

class RenderStateCapture {
    var hasCapture: Boolean = false
    val pose: PoseStack = PoseStack()
    val projection: Matrix4f = Matrix4f()

    /**
     * Hand Only
     */
    var item: ItemStack? = null

    /**
     * Level Only
     */
    var camera: Camera? = null

    companion object {
        @JvmStatic
        val LEVEL: RenderStateCapture = RenderStateCapture()
        @JvmStatic
        val CAPTURED_WORLD_DEPTH_BUFFER: RenderTarget = TextureTarget(
            Minecraft.getInstance().window.width,
            Minecraft.getInstance().window.height,
            true, Minecraft.ON_OSX
        )
        @JvmStatic
        val CAPTURED_HAND_DEPTH_BUFFER: RenderTarget = TextureTarget(
            Minecraft.getInstance().window.width,
            Minecraft.getInstance().window.height,
            true, Minecraft.ON_OSX
        )
    }
}