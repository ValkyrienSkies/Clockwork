package org.valkyrienskies.clockwork.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valkyrienskies.clockwork.ClockWorkMod;

import java.io.IOException;

public final class ShaderReference {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String name;
    private final VertexFormat format;
    private ShaderInstance shader;

    public ShaderReference(final String name, final VertexFormat format) {
        this.name = name;
        this.format = format;
    }

    public void reload(final ResourceProvider provider) {
        if (shader != null) {
            shader.close();
            shader = null;
        }

        try {
            shader = new ShaderInstance(location -> {
                try {
                    return provider.getResource(new ResourceLocation(ClockWorkMod.MOD_ID, location.getPath()));
                } catch (final IOException e) {
                    ClockWorkMod.LOGGER.warn("Shader is failing to load?", e);
                    return provider.getResource(location);
                }
            }, name, format);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public ShaderInstance getShader() {
        return shader;
    }
}
