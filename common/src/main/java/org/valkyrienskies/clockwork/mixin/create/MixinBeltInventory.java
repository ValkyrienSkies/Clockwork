package org.valkyrienskies.clockwork.mixin.create;

import com.simibubi.create.content.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.content.contraptions.relays.belt.transport.BeltInventory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(BeltInventory.class)
public abstract class MixinBeltInventory {
    @Shadow
    @Final
    BeltTileEntity belt;

    @Redirect(method = "eject", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void redirectAdd(ItemEntity instance, Vec3 vec3) {
        instance.setDeltaMovement(outMotion(vec3));
    }

    @Unique
    private Vec3 outMotion(Vec3 motion) {
        Level level = this.belt.getLevel();
        if (level != null) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.belt.getBlockPos());
            if (ship != null) {
                Vector3d tempVec = VectorConversionsMCKt.toJOML(motion);
                ship.getTransform().getShipToWorld().transformDirection(tempVec);
                motion = VectorConversionsMCKt.toMinecraft(tempVec);
            }
        }
        return motion;
    }
}