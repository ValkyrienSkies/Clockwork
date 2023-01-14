package org.valkyrienskies.clockwork;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.valkyrienskies.clockwork.client.render.ShaderReference;

import java.util.ArrayList;
import java.util.List;

public class ClockWorkShaders {
    private static final List<ShaderReference> SHADERS = new ArrayList<>();

    public static final ShaderReference
        SCAN_EFFECT = shader("scan_effect", DefaultVertexFormat.POSITION_TEX)

            ;


    private static ShaderReference shader(String shader, VertexFormat format) {
        ShaderReference result =  new ShaderReference(shader, format);
        SHADERS.add(result);
        return result;
    }

    public static void reloadShaders(ResourceProvider resources) {
        SHADERS.forEach(shaderReference -> shaderReference.reload(resources));
    }
}
