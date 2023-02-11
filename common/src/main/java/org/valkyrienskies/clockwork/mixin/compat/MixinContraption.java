package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.StabilizedContraption;
import com.simibubi.create.foundation.utility.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(Contraption.class)
public class MixinContraption {
    @Shadow
    public AbstractContraptionEntity entity;

    @Unique
    private BlockFace subContraptionBlockFace;

    @Inject(method = "onEntityCreated", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/bearing/StabilizedContraption;removeBlocksFromWorld(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectOnEntityCreated(AbstractContraptionEntity entity, CallbackInfo ci, Iterator var2, BlockFace blockFace) {
        this.subContraptionBlockFace = blockFace;
    }

    private boolean addToWorld(Level world, Entity entity) {
        boolean added = world.addFreshEntity(entity);
        BlockPos anchor = subContraptionBlockFace.getConnectedPos();
        if (added) {
            entity.moveTo(anchor.getX() + .5, anchor.getY(), anchor.getZ() + .5);
        }
        return added;
    }

    @Redirect(method = "onEntityCreated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean redirectAddFreshEntity(Level instance, Entity entity) {
        return addToWorld(instance, entity);
    }
}
