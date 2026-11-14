package org.valkyrienskies.clockwork.mixin.content.schematics;

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
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableSchematicBehaviour;

@Mixin(StructureTemplate.class)
public class MixinThinkableTemplate {
    @WrapOperation(method = "placeInWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void clockwork$transformConnections(BlockEntity be, CompoundTag tag, Operation<Void> original,
            ServerLevelAccessor level, BlockPos origin, BlockPos pivot, StructurePlaceSettings settings,
            RandomSource random, int flags) {
        if (be instanceof IThinkable) {
            tag = ThinkableSchematicBehaviour.transform(tag, settings.getMirror(), settings.getRotation(),
                    be instanceof CombobulatorBlockEntity);
        }
        original.call(be, tag);
    }
}
