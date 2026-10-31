package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Supplies the entity attributes missing from the position/color/UV ribbon renderer. */
public final class EmissiveEnergyVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;

    public EmissiveEnergyVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }

    @Override
    public void endVertex() {
        // Ribbons are camera-facing and their positions have already been transformed to view space.
        delegate.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1).endVertex();
    }
}
