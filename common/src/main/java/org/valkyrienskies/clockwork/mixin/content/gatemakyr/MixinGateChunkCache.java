package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrChunkCache;
import net.minecraft.world.level.chunk.EmptyLevelChunk;

@Mixin(ClientChunkCache.class)
public abstract class MixinGateChunkCache implements GatemakyrChunkCache {
    @Shadow @Final private ClientLevel level;
    @Shadow public abstract LevelChunk getChunk(int x, int z, ChunkStatus status, boolean load);
    @Unique private final Map<Long, LevelChunk> clockwork$remote = new ConcurrentHashMap<>();
    @Unique private Set<Long> clockwork$watched = Set.of();

    @Override public boolean keepsGateChunk(int x, int z) {
        return clockwork$watched.contains(ChunkPos.asLong(x, z));
    }

    @Override public void setGateChunks(Set<Long> chunks) {
        clockwork$watched = chunks;
        for (long key : clockwork$remote.keySet()) {
            if (chunks.contains(key)) continue;
            LevelChunk removed = clockwork$remote.remove(key);
            ChunkPos pos = new ChunkPos(key);
            if (getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                level.unload(removed);
                level.getLightEngine().setLightEnabled(pos, false);
            }
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false), cancellable = true)
    private void clockwork$readRemote(int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> entities, CallbackInfoReturnable<LevelChunk> cir) {
        if (!keepsGateChunk(x, z)) return;
        ChunkPos pos = new ChunkPos(x, z);
        LevelChunk chunk = clockwork$remote.computeIfAbsent(pos.toLong(), key -> new LevelChunk(level, pos));
        chunk.replaceWithPacketData(buf, tag, entities);
        level.onChunkLoaded(pos);
        cir.setReturnValue(chunk);
    }

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void clockwork$retainLocal(int x, int z, FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> entities, CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null && keepsGateChunk(x, z)) clockwork$remote.put(ChunkPos.asLong(x, z), cir.getReturnValue());
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("RETURN"), cancellable = true)
    private void clockwork$getRemote(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
        if (clockwork$remote.isEmpty()) return;
        LevelChunk remote = clockwork$remote.get(ChunkPos.asLong(x, z));
        if (remote != null && (cir.getReturnValue() == null || cir.getReturnValue() instanceof EmptyLevelChunk))
            cir.setReturnValue(remote);
    }

    @Inject(method = "replaceBiomes", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false), cancellable = true)
    private void clockwork$readBiomes(int x, int z, FriendlyByteBuf data, CallbackInfo ci) {
        LevelChunk chunk = clockwork$remote.get(ChunkPos.asLong(x, z));
        if (chunk != null) {
            chunk.replaceBiomes(data);
            ci.cancel();
        }
    }
}
