package org.valkyrienskies.clockwork.mixin.content.universal_joint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity;

@Mixin(StructureTemplate.class)
public class MixinStructureTemplate {
    @WrapOperation(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    public void cw$wrapBlockEntityLoad(
            BlockEntity instance,
            CompoundTag tag,
            Operation<Void> original,
            ServerLevelAccessor levelAccessor,
            BlockPos pos0,
            BlockPos pos1,
            StructurePlaceSettings settings,
            RandomSource randomSource,
            int flags
    ) {
        CompoundTag resultTag = tag;

        if (instance instanceof UniversalShaftBlockEntity) {
            if (tag.contains("otherPosXr")) {
                BlockPos offset = new BlockPos(tag.getInt("otherPosXr"),tag.getInt("otherPosYr"),tag.getInt("otherPosZr"));
                //BlockPos connectedPos = worldPosition.offset(offset)
                offset = StructureTemplate.transform(offset, settings.getMirror(), settings.getRotation(), BlockPos.ZERO);
                resultTag.putInt("otherPosXr", offset.getX());
                resultTag.putInt("otherPosYr", offset.getY());
                resultTag.putInt("otherPosZr", offset.getZ());
            }

        }

        original.call(instance, resultTag);
    }
}
