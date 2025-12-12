package org.valkyrienskies.clockwork.content.curiosities.sensor.rotation

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.belt.BeltHelper
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import java.util.*

class LodefocusRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<LodefocusBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: LodefocusBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (blockEntity.inventory[0].isEmpty || blockEntity.level == null) {
            return
        }
        ms.pushPose()
        val itemPosition = VecHelper.getCenterOf(blockEntity.getBlockPos())
        val stack = blockEntity.inventory[0]
        renderItem(
            ms,
            buffer,
            light,
            overlay,
            stack,
            90,
            blockEntity.level!!.random,
            itemPosition
        )
        ms.popPose()
    }

    fun renderItem(
        ms: PoseStack, buffer: MultiBufferSource?, light: Int, overlay: Int, itemStack: ItemStack,
        angle: Int, r: RandomSource, itemPosition: Vec3
    ) {
        val itemRenderer = Minecraft.getInstance()
            .itemRenderer
        val msr = TransformStack.of(ms)
        val count = (Mth.log2((itemStack.count))) / 2
        val renderUpright = BeltHelper.isItemUpright(itemStack) || itemStack.`is`(Items.COMPASS)
        val bakedModel = itemRenderer.getModel(itemStack, null, null, 0)
        val blockItem = bakedModel.isGui3d

        ms.pushPose()
        msr.rotateYDegrees(angle.toFloat())

        if (renderUpright) {
            val renderViewEntity = Minecraft.getInstance().cameraEntity
            if (renderViewEntity != null) {
                val positionVec = renderViewEntity.position()
                val vectorForOffset = itemPosition
                val diff = vectorForOffset.subtract(positionVec)
                val yRot = (Mth.atan2(diff.x, diff.z) + Math.PI).toFloat()
                ms.mulPose(Quaternionf(AxisAngle4f(yRot, 0f, 1f, 0f)))
            }
            ms.translate(0.0, 3 / 32.0, (-1 / 16f).toDouble())
        }

        for (i in 0..count) {
            ms.pushPose()
            if (blockItem) ms.translate(
                (r.nextFloat() * .0625f * i).toDouble(),
                0.0,
                (r.nextFloat() * .0625f * i).toDouble()
            )
            ms.scale(.5f, .5f, .5f)
            itemRenderer.render(
                itemStack,
                ItemDisplayContext.FIXED,
                false,
                ms,
                buffer,
                light,
                overlay,
                bakedModel
            )
            ms.popPose()

            if (!renderUpright) {
                if (!blockItem) msr.rotateYDegrees(10.0f)
                ms.translate(0.0, if (blockItem) 1 / 64.0 else 1 / 16.0, 0.0)
            } else ms.translate(0.0, 0.0, (-1 / 16f).toDouble())
        }

        ms.popPose()
    }
}
