package org.valkyrienskies.clockwork.mixin.content.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintItem;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonAccess;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonBuild;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonInventory;

@Mixin(value = SchematicannonBlockEntity.class, remap = false)
public abstract class MixinVdexCannon implements VdexCannonAccess {
    @Unique private VdexCannonBuild clockwork$build;
    @Shadow private boolean blockSkipped;

    @Override public VdexCannonBuild getVdexBuild() { return clockwork$build; }
    @Override public void setVdexBuild(VdexCannonBuild build) { clockwork$build = build; }

    @Inject(method = "initializePrinter", at = @At("HEAD"), cancellable = true)
    private void clockwork$initialize(ItemStack stack, CallbackInfo ci) {
        if (!(stack.getItem() instanceof VdexBlueprintItem)) return;
        ci.cancel();
        SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
        try {
            if (clockwork$build == null) {
                if (stack.getOrCreateTag().getBoolean("VdexStopped") && cannon.state == SchematicannonBlockEntity.State.STOPPED) return;
                stack.removeTagKey("VdexStopped");
                clockwork$build = VdexCannonBuild.create(cannon, stack);
            }
            cannon.printer = clockwork$build.printer(cannon);
            cannon.updateChecklist();
            cannon.sendUpdate = true;
            cannon.setChanged();
        } catch (Exception e) {
            cannon.state = SchematicannonBlockEntity.State.PAUSED;
            cannon.statusMsg = "vdexError";
            cannon.sendUpdate = true;
        }
    }

    @Inject(method = "tickPrinter", at = @At("HEAD"), cancellable = true)
    private void clockwork$tick(CallbackInfo ci) {
        SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
        ItemStack stack = ((VdexCannonInventory) this).blueprint();
        if (clockwork$build == null && !(stack.getItem() instanceof VdexBlueprintItem)) return;
        blockSkipped = false;
        ci.cancel();
        if (clockwork$build != null) {
            clockwork$build.protectShips((ServerLevel) cannon.getLevel(), cannon.getBlockPos());
            clockwork$build.tick(cannon);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void clockwork$read(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        clockwork$build = tag.contains("ClockworkVdex") ? new VdexCannonBuild(tag.getCompound("ClockworkVdex").copy()) : null;
        if (clockwork$build != null) {
            SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
            cannon.printer = clockwork$build.printer(cannon);
        }
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void clockwork$write(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (clockwork$build != null) tag.put("ClockworkVdex", clockwork$build.getSaved().copy());
    }
}
