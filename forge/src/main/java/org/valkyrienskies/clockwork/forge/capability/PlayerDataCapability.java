package org.valkyrienskies.clockwork.forge.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.util.ClockworkUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PlayerDataCapability implements INBTSerializable<CompoundTag>, AreaData {

    public static Capability<PlayerDataCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    public boolean shouldReset = false;
    public int resetTimer = 20;
    public SelectedAreaToolkit toolkit = new SelectedAreaToolkit();
    public Optional<Vector3ic> firstPos = Optional.empty();
    public Optional<Vector3ic> secondPos = Optional.empty();

    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerDataCapability cap = PlayerDataCapability.getCapability(player);
            if (cap.shouldReset) {
                cap.resetTimer--;
                if (cap.resetTimer <= 0) {
                    cap.resetTimer = 20;
                    cap.shouldReset(false);
                    HashSet<Set<AABBic>> clone = new HashSet<>(cap.getArea().getSelectionClusters());

                    for (Set<AABBic> aabBic : clone) {
                        cap.getArea().dumpCluster(aabBic);
                    }
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDataCapability.syncSelf(serverPlayer);
                }
            }
        }
    }


    @Override
    public void setArea(SelectedAreaToolkit load) {
        this.toolkit = load;
    }

    @Override
    public SelectedAreaToolkit getArea() {
        return toolkit;
    }

    @Override
    public Optional<Vector3ic> getFirstPos() {
        return firstPos;
    }

    @Override
    public void setFirstPos(Optional<Vector3ic> pos) {
        this.firstPos = pos;
    }

    @Override
    public Optional<Vector3ic> getSecondPos() {
        return secondPos;
    }

    @Override
    public void setSecondPos(Optional<Vector3ic> pos) {
        this.secondPos = pos;
    }

    @Override
    public void shouldReset(boolean reset) {
        this.shouldReset = reset;
    }



    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();

        CompoundTag tag = new CompoundTag();
        ClockworkUtils.INSTANCE.saveArea(tag, getArea());

        if (getFirstPos().isPresent()) {
            tag.putInt("XF", getFirstPos().get().x());
            tag.putInt("YF", getFirstPos().get().y());
            tag.putInt("ZF", getFirstPos().get().z());
        }

        if (getSecondPos().isPresent()) {
            tag.putInt("XS", getSecondPos().get().x());
            tag.putInt("YS", getSecondPos().get().y());
            tag.putInt("ZS", getSecondPos().get().z());
        }

        compoundTag.put("AreaData", tag);
        compoundTag.putBoolean("Reset", shouldReset);


        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        CompoundTag tag = (CompoundTag) compoundTag.get("AreaData");
        if (tag != null) {
            setArea(ClockworkUtils.INSTANCE.loadArea(tag));

            setFirstPos(Optional.of(new Vector3i(tag.getInt("XF"), tag.getInt("YF"), tag.getInt("ZF"))));
            setSecondPos(Optional.of(new Vector3i(tag.getInt("XS"), tag.getInt("YS"), tag.getInt("ZS"))));
        }

        if (compoundTag.contains("Reset")) {
            shouldReset(compoundTag.getBoolean("Reset"));
        }
    }

    public static void syncSelf(ServerPlayer player) {
        sync(player, PacketDistributor.PLAYER.with(() -> player));
    }

    public static void syncTrackingAndSelf(Player player) {
        sync(player, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player));
    }

    public static void syncTracking(Player player) {
        sync(player, PacketDistributor.TRACKING_ENTITY.with(() -> player));
    }

    public static void sync(Player player, PacketDistributor.PacketTarget target) {

        getCapabilityOptional(player).ifPresent(c -> {
            if (player instanceof ServerPlayer serverPlayer) {
                SharedValues.getPacketChannel().sendToClientsTrackingAndSelf(new SyncPlayerCapabilityDataPacket(player.getUUID(), c.serializeNBT()), serverPlayer);
            }
        });
    }

    public static LazyOptional<PlayerDataCapability> getCapabilityOptional(Player player) {
        return player.getCapability(CAPABILITY);
    }

    public static PlayerDataCapability getCapability(Player player) {
        return player.getCapability(CAPABILITY).orElse(new PlayerDataCapability());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerDataCapability.class);
    }

    public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            final PlayerDataCapability capability = new PlayerDataCapability();
            event.addCapability(ClockworkMod.INSTANCE.asResource("player_data"), new ClockworkCapabilityProvider<>(PlayerDataCapability.CAPABILITY, () -> capability));
        }
    }

    public static void syncPlayerCapability(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player player) {
            if (player.level() instanceof ServerLevel) {
                syncTracking(player);
            }
        }
    }
}