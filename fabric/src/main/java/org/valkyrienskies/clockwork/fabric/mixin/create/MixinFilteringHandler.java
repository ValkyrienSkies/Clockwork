package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(FilteringHandler.class)
public abstract class MixinFilteringHandler {

    @ModifyArg(
        method = "*",
        at = @At(
                value = "INVOKE",
                target = "Lcom/simibubi/create/foundation/tileEntity/behaviour/filtering/FilteringBehaviour;testHit(Lnet/minecraft/world/phys/Vec3;)Z"
        ), index = 0
    )
    private static Vec3 modTestHit(Vec3 hit) {
        Vec3 result = hit;
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        if (mc.level != null && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
            result = VSGameUtilsKt.toShipRenderCoordinates(Minecraft.getInstance().level, new Vec3(pos.getX(), pos.getY(), pos.getZ()), hit);
        }
        return result;
    }
}