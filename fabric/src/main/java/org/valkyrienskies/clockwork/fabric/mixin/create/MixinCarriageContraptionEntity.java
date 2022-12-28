package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.logistics.trains.entity.Carriage;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.utility.VecHelper;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(CarriageContraptionEntity.class)
public abstract class MixinCarriageContraptionEntity {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.MixinCarriageContraptionEntity");

    @Unique
    private Level world;

    @Shadow
    private Carriage carriage;

    @Shadow
    public boolean validForRender;

    @Inject(
        method = "control",
        at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectCaptureLevel(
        final BlockPos controlsLocalPos, final Collection<Integer> heldControls, final Player player,
        final CallbackInfoReturnable<Boolean> cir) {
        this.world = player.level;
        final Vec3 controlsLPos =
            ((CarriageContraptionEntity) (Object) this).toGlobalVector(VecHelper.getCenterOf(controlsLocalPos), 1);
        LOGGER.warn("closerThan " +

            this.redirectCloserThan(controlsLPos, player.position(), 8) + "controlsLocalPos " + controlsLPos +
            " player.position " + player.position());
        if (carriage == null) {
            LOGGER.warn("Carriage is null!");
        }
        LOGGER.warn("Return Value " + cir.getReturnValue());

    }

    @Redirect(
        method = "control",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"
        )
    )
    private boolean redirectCloserThan(final Vec3 instance, final Position arg, final double d) {
        Vec3 newVec3 = instance;
        LOGGER.warn("handle closerThan instance " + instance + " arg " + arg + " d " + d);
        if (VSGameUtilsKt.isBlockInShipyard(this.world, new BlockPos(instance.x, instance.y, instance.z))) {
            LOGGER.warn("Block in shipyard");
            final Ship ship = VSGameUtilsKt.getShipManagingPos(this.world, instance);
            newVec3 = VSGameUtilsKt.toWorldCoordinates(ship, instance);
            LOGGER.warn("handle closerThan newVec3 " + newVec3 + " arg " + arg + " d " + d);
        }

        return newVec3.closerThan(arg, d);
    }
}
