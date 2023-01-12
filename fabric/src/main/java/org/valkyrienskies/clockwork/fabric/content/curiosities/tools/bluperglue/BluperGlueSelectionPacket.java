package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
public class BluperGlueSelectionPacket extends SimplePacketBase {

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
    public void handle(Supplier<Context> context) {
        Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            double range = ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5 : 4.5) + 2;
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
            BluperGlueEntity entity = new BluperGlueEntity(player.level, bb);
            player.level.addFreshEntity(entity);
            entity.spawnParticles();
        });
        ctx.setPacketHandled(true);
    }

}