package org.valkyrienskies.clockwork.forge.mixin.compat;

import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(FilteringHandler.class)
public abstract class MixinFilteringHandler {
    @Unique
    private static BlockHitResult injectedHitResult;
    @Unique
    private static Level injectedLevel;

    @Inject(
            method = "onBlockActivated",
            at = @At("HEAD"), remap = false
    )
    private static void injectHead(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        injectedLevel = event.getWorld();
        injectedHitResult = event.getHitVec();
    }

    @ModifyArg(
            method = "onBlockActivated",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/tileEntity/behaviour/filtering/FilteringBehaviour;testHit(Lnet/minecraft/world/phys/Vec3;)Z"
            ), index = 0, remap = false
    )
    private static Vec3 modTestHit1(Vec3 hit) {
        return modTestHit(hit, injectedLevel, injectedHitResult);
    }

    @OnlyIn(Dist.CLIENT)
    @ModifyArg(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/tileEntity/behaviour/filtering/FilteringBehaviour;testHit(Lnet/minecraft/world/phys/Vec3;)Z"
            ), index = 0, remap = false
    )
    private static Vec3 modTestHit2(Vec3 hit) {
        Minecraft mc = Minecraft.getInstance();
        return modTestHit(hit, mc.level, mc.hitResult);
    }

    @Unique
    private static Vec3 modTestHit(Vec3 hit, Level level, HitResult hitResult) {
        Vec3 result = hit;
        if (level != null && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
            if (ship != null) {
                Vector3d newPos = new Vector3d();
                ship.getTransform().getWorldToShip().transformPosition(hit.x, hit.y, hit.z, newPos);
                result = VectorConversionsMCKt.toMinecraft(newPos);
            }
        }
        return result;
    }
}