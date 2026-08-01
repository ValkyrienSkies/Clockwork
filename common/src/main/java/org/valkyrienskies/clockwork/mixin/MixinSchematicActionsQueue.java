package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.spaceeye.vmod.utils.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.mod.common.assembly.ICopyableBlock;

import java.util.List;
import java.util.Map;

// REMOVE ME WHEN 2.5 VMOD IS OUT!!!
@Mixin(targets = "net.spaceeye.vmod.schematic.SchematicActionsQueue$SchemPlacementItem")
public class MixinSchematicActionsQueue {
    @Inject(
            method = "placeChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;", ordinal = 0)
    )
    private void injectBlacklistCheck(
            ServerLevel level,
            Map<Long, Long> oldToNewId,
            @Coerce Object currentChunkData,
            @Coerce Object blockPalette,
            List<? extends CompoundTag> flatTagData,
            Vector3d shipCenter,
            CallbackInfo ci,
            @Local(name = "blacklisted") LocalBooleanRef blacklisted,
            @Local(name = "block") Block block
    ) {
        if (block instanceof ICopyableBlock) {
            blacklisted.set(false);
        }
    }
}
