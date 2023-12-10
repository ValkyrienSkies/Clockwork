package org.valkyrienskies.clockwork.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.glue.SuperGlueItem;
import com.simibubi.create.content.contraptions.glue.SuperGlueRenderer;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionPacket;
import com.simibubi.create.content.contraptions.minecart.CouplingRenderer;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.util.render.BluperClusterRenderer;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClockworkClientEvents {


    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera()
                .getPosition();


        //CreateClient.OUTLINER.renderOutlines(ms, buffer, camera, partialTicks);
        ClockworkMod.INSTANCE.getOUTLINER().renderOutlines(ms, SuperRenderTypeBuffer.getInstance(), camera, partialTicks);
        //BluperClusterRenderer.Companion.getINSTANCE().renderDesignator(Minecraft.getInstance().level, Minecraft.getInstance(), ms);

        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }
}
