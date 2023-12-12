package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.AssembleTool;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GrabTool;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GrabssembleTool;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GravitronToolBase;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;

import static org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GravitronToolBase.getState;

public class GravitronGrabPacket implements C2SCWPacket {
    public BlockPos clickedPos;
    public Vec3 clickLocation;
    public byte mode;

    public GravitronGrabPacket(FriendlyByteBuf buffer) {
        clickedPos = buffer.readBlockPos();
        clickLocation = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        mode = buffer.readByte();
    }

    public GravitronGrabPacket(BlockPos clickedPos, Vec3 clickedLocation, byte mode) {
        this.clickedPos = clickedPos;
        this.clickLocation = clickedLocation;
        this.mode = mode;
    }

    @Override
    public void handle(@NotNull ServerNetworkContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                var s = getState(serverPlayer);
                var stack = serverPlayer.getMainHandItem();
                if (stack.is(ClockworkItems.GRAVITRON.asItem())) {
                    if ((s.getShipID() == null) && !serverPlayer.getCooldowns().isOnCooldown(stack.getItem()) && !s.getGrabbing()) {


                        serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        s.setGrabCD(20);
                        if (mode == GravitronToolBase.GRAB) {
                            s.setGrabbing(true);
                            GrabTool.tryGrabShip(serverLevel, serverPlayer, clickedPos.mutable(), clickLocation);
                        } else if (mode == GravitronToolBase.ASSEMBLE) {
                            GravitronItem.Companion.grabssemble(serverLevel, serverPlayer, clickedPos.mutable(), clickLocation, false);
                        } else if(mode == GravitronToolBase.GRABSSEMBLE){
                            s.setGrabbing(true);
                            GrabssembleTool.tryAssembleAndGrabShip(serverLevel, serverPlayer, clickedPos.mutable(), clickLocation);
                        }
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
        buffer.writeByte(mode);
    }
}
