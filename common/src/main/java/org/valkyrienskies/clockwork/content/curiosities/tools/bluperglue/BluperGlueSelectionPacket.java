package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;

import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;

public class BluperGlueSelectionPacket implements C2SCWPacket {

    private BlockPos from;
    private BlockPos to;

    public BluperGlueSelectionPacket(BlockPos from, BlockPos to) {
        this.from = from;
        this.to = to;
    }

    public BluperGlueSelectionPacket(FriendlyByteBuf buffer) {
        from = buffer.readBlockPos();
        to = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(from);
        buffer.writeBlockPos(to);
    }

    @Override
    public void handle(ServerNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            double range = PlatformUtils.getReachDistance(player) + 2;
            if (player.distanceToSqr(Vec3.atCenterOf(to)) > range * range)
                return;
            if (!to.closerThan(from, 25))
                return;

            Set<BlockPos> group = BluperGlueSelectionHelper.searchGlueGroup(player.level, from, to, false);
            if (group == null)
                return;
            if (!group.contains(to))
                return;
            if (!BluperGlueSelectionHelper.collectGlueFromInventory(player, 1, true))
                return;

            AABB bb = BluperGlueEntity.span(from, to);
            BluperGlueSelectionHelper.collectGlueFromInventory(player, 1, false);
            BluperGlueEntity entity = BluperGlueEntity.create(player.level, bb);
            player.level.addFreshEntity(entity);
            entity.spawnParticles();
        });
        ctx.setPacketHandled(true);
    }

}