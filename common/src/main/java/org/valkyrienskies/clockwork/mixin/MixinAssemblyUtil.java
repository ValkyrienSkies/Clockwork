package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.util.compat.CopyableBlockEntity;
import org.valkyrienskies.mod.common.assembly.AssemblyUtil;

//TODO this is dumb
@Mixin(AssemblyUtil.class)
public abstract class MixinAssemblyUtil {
    @WrapOperation(method = "copyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithId()Lnet/minecraft/nbt/CompoundTag;"))
    public CompoundTag vs_clockwork$copy(BlockEntity instance, Operation<CompoundTag> original) {
        if (instance instanceof CopyableBlockEntity) {
            return ((CopyableBlockEntity) instance).copyWrite();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "copyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"))
    public void vs_clockwork$write(BlockEntity instance, CompoundTag tag, Operation<Void> original) {
        if (instance instanceof CopyableBlockEntity) {
            ((CopyableBlockEntity) instance).copyRead(tag);
        } else {
            original.call(instance, tag);
        }
    }
}
