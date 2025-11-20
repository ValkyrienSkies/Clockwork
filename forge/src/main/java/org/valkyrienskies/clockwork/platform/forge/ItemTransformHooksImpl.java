package org.valkyrienskies.clockwork.platform.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.ForgeHooksClient;

@SuppressWarnings("unused")
public class ItemTransformHooksImpl {
    public static void applyItemTransform(PoseStack poseStack, BakedModel model, ItemDisplayContext context, boolean applyLeftHandTransform) {
        ForgeHooksClient.handleCameraTransforms(poseStack, model, context, applyLeftHandTransform);
    }
}
