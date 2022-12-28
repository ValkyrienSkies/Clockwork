package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandlerClient;
import com.simibubi.create.foundation.utility.Couple;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(ContraptionHandlerClient.class)
public abstract class MixinContraptionHandlerClient {

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
        final Ship ship = VSGameUtilsKt.getShipManagingPos(player.level, player.getOnPos());

        if (ship != null) {
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