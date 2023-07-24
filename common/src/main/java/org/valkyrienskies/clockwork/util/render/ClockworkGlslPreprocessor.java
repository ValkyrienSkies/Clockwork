package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.ClockWorkMod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ClockworkGlslPreprocessor extends GlslPreprocessor {
    @Nullable
    @Override
    public String applyImport(boolean useFullPath, @NotNull String name) {
        ClockWorkMod.LOGGER.debug("Loading moj_import in EffectProgram: " + name);

        ResourceLocation id = new ResourceLocation(name);
        ResourceLocation id1 = new ResourceLocation(id.getNamespace(), "shaders/include/" + id.getPath() + ".glsl");

        try {
            InputStream resource1 = Minecraft.getInstance().getResourceManager().getResource(id1).getInputStream();

            String s2;
            try {
                s2 = IOUtils.toString(resource1, StandardCharsets.UTF_8);
            } catch (Throwable throwable1) {
                if (resource1 != null) {
                    try {
                        resource1.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (resource1 != null) {
                resource1.close();
            }

            return s2;

        } catch (IOException ioException) {
            ClockWorkMod.LOGGER.error("Could not open GLSL import {}: {}", name, ioException.getMessage());
            return "#error " + ioException.getMessage();
        }
    }
}
