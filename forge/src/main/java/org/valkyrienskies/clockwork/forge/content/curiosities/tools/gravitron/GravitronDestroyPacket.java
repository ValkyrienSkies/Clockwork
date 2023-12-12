package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;
import org.valkyrienskies.clockwork.util.ShipDestroyer;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import static java.lang.Math.PI;
import static java.lang.Math.floor;

public class GravitronDestroyPacket implements C2SCWPacket {
    public BlockPos clickedPos;

    public GravitronDestroyPacket(FriendlyByteBuf buffer) {
        clickedPos = buffer.readBlockPos();
    }

    public GravitronDestroyPacket(BlockPos clickedPos) {
        this.clickedPos = clickedPos;
    }

    @Override
    public void handle(@NotNull ServerNetworkContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            ServerLevel serverLevel = serverPlayer.serverLevel();
            var chunkX = clickedPos.getX() >> 4;
            var chunkZ = clickedPos.getZ() >> 4;
            LoadedServerShip ship = VSGameUtilsKt.getShipObjectWorld(serverLevel).getLoadedShips().getByChunkPos(chunkX, chunkZ, VSGameUtilsKt.getDimensionId(serverLevel));

            if (ship != null) {
                var invRotation = ship.getTransform().getShipToWorldRotation().invert(new Quaterniond());
                var invRotationAxisAngle = new AxisAngle4d(invRotation);
                var alignTarget = Direction.from2DDataValue((int) floor((invRotationAxisAngle.angle / (PI * 0.5)) + 4.5) % 4);

                ShipDestroyer.INSTANCE.unfillShip(serverLevel, ship, alignTarget);
            }

        });
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeBlockPos(clickedPos);
    }
}
