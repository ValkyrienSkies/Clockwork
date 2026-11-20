package org.valkyrienskies.clockwork.compat.gatemakyr;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Function;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;
import kotlin.Pair;

public final class GateShaderPass {
    private static final LinkedHashMap<Pair<UUID, Integer>, GateShaderPass> views = new LinkedHashMap<>(4, 0.75f, true);
    private static NamespacedId dimension;

    private final WorldRenderingPipeline pipeline;
    private IrisChunkProgramOverrides chunks;

    private GateShaderPass(WorldRenderingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static WorldRenderingPipeline prepare(NamespacedId id, Function<NamespacedId, WorldRenderingPipeline> factory) {
        if (!id.equals(dimension)) {
            clear();
            dimension = id;
        }
        var key = GatemakyrView.captureKey;
        GateShaderPass active = views.get(key);
        if (active == null) {
            if (views.size() < 2) {
                active = new GateShaderPass(factory.apply(id));
            } else {
                var oldest = views.values().iterator();
                active = oldest.next();
                oldest.remove();
                if (active.pipeline instanceof GateShaderHistory history) history.clockwork$resetGateHistory();
            }
            views.put(key, active);
            GatemakyrView.releaseShaderPass = GateShaderPass::clear;
        }
        return active.pipeline;
    }

    public static WorldRenderingPipeline current() {
        GateShaderPass pass = views.get(GatemakyrView.captureKey);
        return pass == null ? null : pass.pipeline;
    }

    public static IrisChunkProgramOverrides chunks() {
        GateShaderPass pass = views.get(GatemakyrView.captureKey);
        if (pass.chunks == null) pass.chunks = new IrisChunkProgramOverrides();
        return pass.chunks;
    }

    public static void clear() {
        for (GateShaderPass pass : views.values()) {
            if (pass.chunks != null) pass.chunks.deleteShaders();
            pass.pipeline.destroy();
        }
        views.clear();
        dimension = null;
        GatemakyrView.releaseShaderPass = null;
    }
}
