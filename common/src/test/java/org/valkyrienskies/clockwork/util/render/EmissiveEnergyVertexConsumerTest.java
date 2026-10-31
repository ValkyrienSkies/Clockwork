package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmissiveEnergyVertexConsumerTest {
    @Test
    void ribbonDefaultOverloadsKeepTheWrapperAndPopulateEveryEntityAttribute() {
        RecordingConsumer recorded = new RecordingConsumer();
        VertexConsumer energy = new EmissiveEnergyVertexConsumer(recorded);
        Matrix4f transform = new Matrix4f().translation(10, 20, 30);
        for (int i = 0; i < 2; i++) {
            energy.vertex(transform, 1, 2, 3).color(0.765f, 0.627f, 0.89f, 0.5f).uv(0.5f, 1).endVertex();
        }
        assertEquals(List.of("position", "color", "uv", "overlay", "light", "normal", "end",
                "position", "color", "uv", "overlay", "light", "normal", "end"), recorded.attributes);
        assertArrayEquals(new double[]{11, 22, 33}, recorded.position);
        assertArrayEquals(new int[]{195, 159, 226, 127}, recorded.color);
        assertEquals(OverlayTexture.NO_OVERLAY, recorded.overlay);
        assertEquals(LightTexture.FULL_BRIGHT, recorded.light);
    }

    private static class RecordingConsumer implements VertexConsumer {
        final List<String> attributes = new ArrayList<>();
        double[] position;
        int[] color;
        int overlay;
        int light;
        @Override public VertexConsumer vertex(double x, double y, double z) {
            attributes.add("position"); position = new double[]{x, y, z}; return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            attributes.add("color"); color = new int[]{r, g, b, a}; return this;
        }
        @Override public VertexConsumer uv(float u, float v) { attributes.add("uv"); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) {
            attributes.add("overlay"); overlay = u | v << 16; return this;
        }
        @Override public VertexConsumer uv2(int u, int v) {
            attributes.add("light"); light = u | v << 16; return this;
        }
        @Override public VertexConsumer normal(float x, float y, float z) { attributes.add("normal"); return this; }
        @Override public void endVertex() { attributes.add("end"); }
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    }
}
