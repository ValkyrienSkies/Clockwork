package org.valkyrienskies.clockwork.mixin.content.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.SchematicExport;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableSchematicCapture;

@Mixin(value = SchematicExport.class, remap = false)
public class MixinThinkableSchematicExport {
    @WrapOperation(method = "saveSchematic", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag clockwork$captureThinkables(StructureTemplate structure, CompoundTag tag,
            Operation<CompoundTag> original, Path dir, String fileName, boolean overwrite,
            Level level, BlockPos first, BlockPos second) {
        return ThinkableSchematicCapture.capture(level, first, second, original.call(structure, tag));
    }
}
