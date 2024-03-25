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
                new ShaderInstance(event.getResourceManager(), "crystal", DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> {
                    //ClockworkShaders.crystal = shaderInstance;
                }
        );
        event.registerShader(
                new ShaderInstance(event.getResourceManager(), "heat", DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> {
                    //ClockworkShaders.heat = shaderInstance;
                }
        );
        event.registerShader(
                new ShaderInstance(event.getResourceManager(), "haze", DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> {
                    //ClockworkShaders.haze = shaderInstance;
                }
        );
    }
}