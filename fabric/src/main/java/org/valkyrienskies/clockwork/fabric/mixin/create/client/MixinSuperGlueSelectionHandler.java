package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueSelectionHandler;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(SuperGlueSelectionHandler.class)
public abstract class MixinSuperGlueSelectionHandler {
    @Unique
    private Vec3 oldOrigin;

    @ModifyVariable(
        method = "tick",
        at = @At(
            value = "STORE"
        ), ordinal = 0, remap = false
    )
    private Vec3 getTraceOrigin(final Vec3 value) {
        oldOrigin = value;
        return modVec3ToShip(value);
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 redirectGetTraceTarget(final Player playerIn, final double range, final Vec3 origin) {
        final Vec3 value = RaycastHelper.getTraceTarget(playerIn, range, oldOrigin);
        return modVec3ToShip(value);
    }

    @Unique
    private Vec3 modVec3ToShip(final Vec3 value) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK && mc.level != null) {
            final BlockPos blockPos = ((BlockHitResult) mc.hitResult).getBlockPos();

            final ClientShip ship = (ClientShip) VSGameUtilsKt.getShipManagingPos(mc.level, blockPos);
            if (ship != null) {
                final ShipTransform transform = ship.getRenderTransform();
                final Vector3d transformed =
                    transform.getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(value));
                return VectorConversionsMCKt.toMinecraft(transformed);
            }
        }
        return value;
    }
}