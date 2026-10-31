package org.valkyrienskies.clockwork.forge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkShaders;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ClockworkMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClockworkShaders {

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "crystal", DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> {
                    //ClockworkShaders.crystal = shaderInstance;
                }
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "heat", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setHeatShader
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "haze", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setHazeShader
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "meteor_trail", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setMeteorTrailShader
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "meteor_glow", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setMeteorGlowShader
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "meteor_shield_depth", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setMeteorShieldDepthShader
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), "meteor_heat_shield", DefaultVertexFormat.NEW_ENTITY),
                ClockworkShaders.INSTANCE::setMeteorHeatShieldShader
        );
    }
}