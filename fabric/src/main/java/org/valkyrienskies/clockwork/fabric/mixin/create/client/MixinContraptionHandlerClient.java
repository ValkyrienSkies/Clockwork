package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandlerClient;
import com.simibubi.create.foundation.utility.Couple;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(ContraptionHandlerClient.class)
public abstract class MixinContraptionHandlerClient {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.MixinContraptionHandlerClient");

    @Unique
    private static Ship getShip(final LocalPlayer player) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(player.level, player.getOnPos());
        if (ship == null && player.level.getBlockState(player.getOnPos()).isAir()) {
            LOGGER.warn("No block beneath player!");
            final BlockPos newPos =
                new BlockPos(player.getOnPos().getX(), player.getOnPos().getY() - 1, player.getOnPos().getZ());
            ship = VSGameUtilsKt.getShipManagingPos(player.level, newPos);
            if (ship == null) {
                LOGGER.warn("No block beneath the block beneath player either!");
            }
        }
        return ship;
    }

    @Inject(
        method = "getRayInputs",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/Couple;create(Ljava/lang/Object;Ljava/lang/Object;)Lcom/simibubi/create/foundation/utility/Couple;"
        ), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true
    )
    private static void redirectedOrigin(
        final LocalPlayer player, final CallbackInfoReturnable<Couple<Vec3>> cir, final Minecraft mc,
        Vec3 origin, final double reach, Vec3 target) {
        boolean isShip = false;
        final LoadedShip loadedShip = VSGameUtilsKt.getShipObjectEntityMountedTo(player.level, player);
        final Ship ship = getShip(player);

        if (loadedShip != null) {
            LOGGER.warn("Player on loadedShip!!!!");
        }
        if (ship != null) {
            isShip = true;
            //LOGGER.warn("Player on ship!!!!");
        }
        /*
        BlockPos shipBlockPos = new BlockPos(0, 0, 0);
        if (mc.hitResult != null && mc.level != null && mc.hitResult.getType() != Type.MISS) {
            if (mc.hitResult.getType() == Type.BLOCK) {
                shipBlockPos = ((BlockHitResult) mc.hitResult).getBlockPos();
                LOGGER.warn("get blockPos " + shipBlockPos);
            }
            LOGGER.warn("get location " + mc.hitResult.getLocation());
            if (mc.hitResult.getType() == Type.ENTITY) {
                LOGGER.warn("get ENTITY location " + ((EntityHitResult) mc.hitResult).getLocation());
            }
            isShip = VSGameUtilsKt.isBlockInShipyard(mc.level, shipBlockPos);
        }*/

        //LOGGER.warn("isShip " + isShip);
        if (isShip) {
            final List<Vector3d>
                originShips =
                VSGameUtilsKt.transformToNearbyShipsAndWorld(player.level, origin.x, origin.y, origin.z, 10);
            if (!originShips.isEmpty()) {
                origin = VectorConversionsMCKt.toMinecraft(originShips.get(0));
            }

            final List<Vector3d>
                targetShips =
                VSGameUtilsKt.transformToNearbyShipsAndWorld(player.level, target.x, target.y, target.z, 10);
            if (!targetShips.isEmpty()) {
                target = VectorConversionsMCKt.toMinecraft(targetShips.get(0));
            }
        }

        cir.setReturnValue(Couple.create(origin, target));
    }
}
