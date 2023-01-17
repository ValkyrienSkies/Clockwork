package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class BluperGlueRenderer extends EntityRenderer<BluperGlueEntity> {

    public BluperGlueRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BluperGlueEntity entity) {
        return null;
    }

    @Override
    public boolean shouldRender(BluperGlueEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

}

