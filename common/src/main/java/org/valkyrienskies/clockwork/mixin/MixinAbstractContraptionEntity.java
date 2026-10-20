package org.valkyrienskies.clockwork.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipReadout;
import org.valkyrienskies.clockwork.mixinduck.MixinAbstractContraptionEntityDuck;

@Mixin(AbstractContraptionEntity.class)
public abstract class MixinAbstractContraptionEntity extends Entity implements MixinAbstractContraptionEntityDuck {
    @Shadow protected abstract StructureTransform makeStructureTransform();

    public MixinAbstractContraptionEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public StructureTransform getStructureTransform() {
        return makeStructureTransform();
    }

    @Inject(method = "setBlock", at = @At("TAIL"), remap = false)
    private void clockwork$invalidateWingReadout(BlockPos pos, StructureTemplate.StructureBlockInfo info, CallbackInfo ci) {
        if (!level().isClientSide) {
            AeronautShipReadout.invalidateContraption(((AbstractContraptionEntity) (Object) this).getContraption());
        }
    }
}
