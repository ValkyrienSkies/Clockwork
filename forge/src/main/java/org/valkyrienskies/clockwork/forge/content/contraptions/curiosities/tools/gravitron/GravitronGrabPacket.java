package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GrabTool;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GrabssembleTool;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;

import static org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GravitronToolBase.getState;

public class GravitronGrabPacket implements C2SCWPacket {
    public BlockPos clickedPos;
    public Vec3 clickLocation;

    public GravitronGrabPacket(FriendlyByteBuf buffer) {
        clickedPos = buffer.readBlockPos();
        clickLocation = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public GravitronGrabPacket(BlockPos clickedPos, Vec3 clickedLocation) {
        this.clickedPos = clickedPos;
        this.clickLocation = clickedLocation;
    }

    @Override
    public void handle(@NotNull ServerNetworkContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                var s = getState(serverPlayer);
                var stack = serverPlayer.getMainHandItem();
                if (stack.is(ClockworkItems.GRAVITRON.asItem())) {
                    System.out.println("ID: " + s.getShipID() + " : " + s.getGrabbing());
                    if ((s.getShipID() == null) && !serverPlayer.getCooldowns().isOnCooldown(stack.getItem()) && !s.getGrabbing()) {

                        s.setGrabbing(true);
                        serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        s.setGrabCD(20);

                        //GrabTool.tryGrabShip(serverLevel, serverPlayer, clickedPos, clickLocation);
                        //GrabssembleTool.tryAssembleAndGrabShip(serverLevel, serverPlayer, clickedPos, clickLocation);
                    }
                }
            }
        });
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeBlockPos(clickedPos);
        buffer.writeDouble(clickLocation.x);
        buffer.writeDouble(clickLocation.y);
        buffer.writeDouble(clickLocation.z);
    }
}
