package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics;
import org.valkyrienskies.clockwork.util.ClockworkSchematicConnections;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.schematic.*;

@Mixin(value = VdexWrapper.class, remap = false)
public class MixinVdexLinks {
    @WrapOperation(method = "saveShip", at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/mod/common/util/GameToPhysicsAdapter;getAllConnectedShips(J)Ljava/util/List;"))
    private List<Long> clockwork$connectedShips(GameToPhysicsAdapter adapter, long shipId, Operation<List<Long>> original,
            @Local(argsOnly = true) ServerLevel level) {
        return ClockworkSchematicConnections.expand(level, original.call(adapter, shipId));
    }

    @WrapOperation(method = "saveShip", at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/mod/common/schematic/VdexIO;write(Ljava/nio/file/Path;Lorg/valkyrienskies/mod/common/schematic/VdexMetadata;Ljava/util/List;Ljava/util/Map;)V"))
    private void clockwork$saveLinks(VdexIO io, Path path, VdexMetadata metadata, List<VdexModEntry> mods,
            Map<String, CompoundTag> blocks, Operation<Void> original, @Local(argsOnly = true) ServerLevel level) {
        VdexData captured = WandLinkSchematics.capture(level, metadata, mods, blocks);
        original.call(io, path, captured.getMetadata(), captured.getModList(), captured.getNbtData());
    }

    @Inject(method = "loadShip", at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/mod/common/schematic/VdexWrapper$DelayedICopyableProcessor;<init>(Ljava/util/List;)V", shift = At.Shift.AFTER))
    private void clockwork$restoreLinks(ServerLevel level, Vector3d position, String filename, Quaterniondc rotation,
            boolean entities, List<StructureProcessor> processors, CallbackInfoReturnable<Component> cir,
            @Local(name = "schem") VdexData schematic, @Local(name = "oldIdToNewShip") Map<Long, ServerShip> ships,
            @Share("clockwork$linksQueued") LocalBooleanRef queued) {
        if (queued.get()) return;
        queued.set(true);
        WandLinkSchematics.queuePaste(level, schematic, ships);
    }
}
