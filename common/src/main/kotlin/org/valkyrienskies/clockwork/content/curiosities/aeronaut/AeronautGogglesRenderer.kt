package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3d
import org.joml.Vector3dc

class AeronautGogglesRenderer: CustomRenderedItemModelRenderer() {
    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel,
        renderer: PartialItemModelRenderer,
        transformType: ItemDisplayContext,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // Item rendering has no wearer context. Equipped goggles are rendered by
        // AeronautArmorLayer using that entity's synchronized state.
        renderer.render(model.originalModel, light)
    }

    companion object {
        fun fromModelSpace(x: Double, y: Double, z: Double): Vector3dc {
            return Vector3d(x / 16.0, y / 16.0, z / 16.0)
        }

        fun toModelSpace(x: Double, y: Double, z: Double): Vector3dc {
            return Vector3d(x * 16.0, y * 16.0, z * 16.0)
        }

        fun Vector3dc.toModelSpace(destination: Vector3d = Vector3d()): Vector3d {
            return destination.set(this).mul(16.0)
        }

        fun Vector3dc.fromModelSpace(destination: Vector3d = Vector3d()): Vector3d {
            return destination.set(this).div(16.0)
        }
    }

}
