package org.valkyrienskies.clockwork.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Inject(method = "readAdditional", at = @At("RETURN"), remap = false)
    private void clockwork$restoreSchematicAnchor(CompoundTag tag, boolean spawnPacket, CallbackInfo ci) {
        if (!spawnPacket && tag.contains("ClockworkAnchorOffset")) {
            AbstractContraptionEntity entity = (AbstractContraptionEntity) (Object) this;
            entity.getContraption().anchor = blockPosition().offset(NbtUtils.readBlockPos(tag.getCompound("ClockworkAnchorOffset")));
        }
    }
}
