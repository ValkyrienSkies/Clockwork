package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
public abstract class MixinGateIrisGeometry {
    @Unique private static final Pattern clockwork$main = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*(?:void\\s*)?\\)\\s*\\{");

    @ModifyReturnValue(method = {"patchVanilla", "patchSodium"}, at = @At("RETURN"))
    private static Map<PatchShaderType, String> clockwork$clipGateGeometry(Map<PatchShaderType, String> programs, String name) {
        if (!GatemakyrView.isolateShaders || programs == null) return programs;
        String pass = name.toLowerCase(Locale.ROOT);
        if (pass.contains("shadow") || pass.contains("sky")) return programs;
        String source = programs.get(PatchShaderType.FRAGMENT);
        if (source == null) return programs;
        var main = clockwork$main.matcher(source);
        if (!main.find()) return programs;
        var clipped = new EnumMap<>(programs);
        clipped.put(PatchShaderType.FRAGMENT, source.substring(0, main.start())
            + "uniform vec4 clockworkGateExitPlane;\n" + main.group()
            + "\nif (dot(clockworkGateExitPlane, vec4(gl_FragCoord.xyz, 1.0)) < 0.0) discard;\n"
            + source.substring(main.end()));
        return clipped;
    }
}
