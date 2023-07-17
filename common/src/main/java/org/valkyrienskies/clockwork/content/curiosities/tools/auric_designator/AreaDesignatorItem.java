package org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import kotlin.jvm.internal.markers.KMutableSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.core.impl.util.AABBdUtilKt;
import org.valkyrienskies.core.impl.util.VSCoreUtilKt;
import org.valkyrienskies.core.impl.util.VectorConversionsKt;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class AreaDesignatorItem extends CWItem {

    public Set<AABBic> selectedAreas = new HashSet<>();
    public Set<Set<AABBic>> selectionClusters = new HashSet<>();

    private boolean wasSelected = false;

    public Vector3ic firstPos = null;
    public Vector3ic secondPos = null;

    public boolean shouldRenderOutlines = false;

    private Random soundRandom = new Random();

    private float soundTickCounter = 0;

    //ANIMATION
    public Animation animationType = Animation.IDLE;

    public float drawProgress = 0;
    public float successProgress = 0;
    public float dumpProgress = 0;
    public float idleProgress = 0;

    public AreaDesignatorItem(Properties properties) {
        super(properties);
    }

    private void clusterNewArea(AABBic initial) {
        Set<AABBic> newCluster = new HashSet<>();
        boolean makeNewCluster = true;
        for (AABBic area : selectedAreas) {
            if (initial.containsAABB(area)) {
                boolean existingCluster = false;
                Set<AABBic> foundCluster = new HashSet<>();
                for (Set<AABBic> cluster : selectionClusters) {
                    if (cluster.contains(area) && !existingCluster) {
                        cluster.add(initial);
                        foundCluster = new HashSet<>(cluster);
                        existingCluster = true;
                        makeNewCluster = false;
                    } else if (cluster.contains(area)) {
                        foundCluster.addAll(cluster);
                        selectionClusters.remove(cluster);
                        makeNewCluster = true;
                        newCluster.addAll(foundCluster);
                    }
                }
            }
        }
        if (makeNewCluster) {
            selectionClusters.add(newCluster);
            mergeClusters();
        }
    }

    private void mergeClusters() {
        for (Set<AABBic> cluster : selectionClusters) {
            for (AABBic area : cluster) {
                for (Set<AABBic> cluster2 : selectionClusters) {
                    if (cluster2.contains(area) && cluster != cluster2) {
                        cluster.addAll(cluster2);
                        selectionClusters.remove(cluster2);
                    }
                }
            }
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (isSelected && !wasSelected) {
            shouldRenderOutlines = true;
            animationType = Animation.DRAW;
            float pitch = Mth.randomBetween(soundRandom, 0.8f, 1.3f);
            level.playSound(null, entity, ClockWorkSounds.DESIGNATOR_ACTIVATE.getMainEvent(), entity.getSoundSource(), 1.0f, pitch);
        } else if (!isSelected && wasSelected) {
            shouldRenderOutlines = false;
        }
        wasSelected = isSelected;
        idleProgress += 0.01f;
        if (idleProgress > 1) {
            idleProgress = 0;
        }
        if (isSelected) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (player.swinging && !player.isUsingItem()) {
                    BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
                    Vector3ic pos = VectorConversionsMCKt.toJOML(hitResult.getBlockPos());
                    Set<AABBic> hitCluster = getClusterContaining(pos);
                    if (hitCluster != null) {
                        float pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f);
                        dumpCluster(hitCluster);
                        level.playSound(null, player, ClockWorkSounds.DESIGNATOR_DUMP_CLUSTER.getMainEvent(), player.getSoundSource(), 1.0f, pitch);
                        animationType = Animation.DUMP;
                    }
                }
            }


            if (animationType.equals(Animation.IDLE)) {
                soundTickCounter += Mth.randomBetween(soundRandom, 0.1f, 0.3f);
                if (soundTickCounter >= 10) {
                    soundTickCounter = 0;
                    float pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f);
                    level.playSound(null, entity, ClockWorkSounds.DESIGNATOR_IDLE.getMainEvent(), entity.getSoundSource(), 1.0f, pitch);
                }
            } else if (animationType.equals(Animation.DRAW)) {
                drawProgress++;
                if (drawProgress >= 60) {
                    drawProgress = 0;
                    animationType = Animation.IDLE;
                }
            } else if (animationType.equals(Animation.SUCCESS)) {
                successProgress++;
                if (successProgress >= 40) {
                    successProgress = 0;
                    animationType = Animation.IDLE;
                }
            } else if (animationType.equals(Animation.DUMP)) {
                dumpProgress++;
                if (dumpProgress >= 40) {
                    dumpProgress = 0;
                    animationType = Animation.IDLE;
                }
            }
        } else {
            firstPos = null;
            secondPos = null;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        InteractionHand hand = context.getHand();
        ItemStack stack = player.getItemInHand(hand);
        Vector3ic pos = VectorConversionsMCKt.toJOML(context.getClickedPos());

        if (!stack.is(this)) {
            return super.useOn(context);
        }
        float pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f);

        if (firstPos == null) {
            firstPos = pos;
            player.displayClientMessage(new TextComponent("First Position Selected!").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)), true);
            world.playSound(null, player, ClockWorkSounds.DESIGNATOR_SELECT_START.getMainEvent(), player.getSoundSource(), 1.0f, pitch);
            return InteractionResult.SUCCESS;
        } else if (secondPos == null && firstPos != null) {
            secondPos = pos;
            AABBic area = new AABBi(firstPos, secondPos);
            firstPos = null;
            secondPos = null;
            if (selectedAreas.contains(area)) {
                player.displayClientMessage(new TextComponent("Area Already Exists.").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)), true);
                world.playSound(null, player, ClockWorkSounds.PHYSICS_INFUSER_LIGHTNING.getMainEvent(), player.getSoundSource(), 1.0f, pitch);
                animationType = Animation.DUMP;
                return InteractionResult.SUCCESS;
            }
            selectedAreas.add(area);
            clusterNewArea(area);
            player.displayClientMessage(new TextComponent("Area Designated!").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)), true);
            world.playSound(null, player, ClockWorkSounds.DESIGNATOR_SELECT_END.getMainEvent(), player.getSoundSource(), 1.0f, pitch);
            stack.setDamageValue(stack.getDamageValue() - 1);
            animationType = Animation.SUCCESS;
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    public Set<AABBic> getClosestCluster(Vector3ic pos) {
        Set<AABBic> returnCluster = new HashSet<>();
        double closestDistance = Double.MAX_VALUE;
        for (Set<AABBic> cluster : selectionClusters) {
            for (AABBic area : cluster) {
                if (area.containsPoint(pos)) {
                    return cluster;
                } else {
                    Vector3dc center = area.center(new Vector3d());
                    double distance = center.distance(pos.x(), pos.y(), pos.z());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        returnCluster = cluster;
                    }
                }
            }
        }
        return returnCluster;
    }

    public Set<AABBic> getClusterContaining(Vector3ic pos) {
        for (Set<AABBic> cluster : selectionClusters) {
            for (AABBic area : cluster) {
                if (area.containsPoint(pos)) {
                    return cluster;
                }
            }
        }
        return null;
    }

    public DenseBlockPosSet denseBlocksFromCluster(Set<AABBic> cluster) {
        DenseBlockPosSet set = new DenseBlockPosSet();
        for (AABBic area : cluster) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    for (int z = area.minZ(); z <= area.maxZ(); z++) {
                        set.add(x, y, z);
                    }
                }
            }
        }
        return set;
    }

    public Set<BlockPos> blocksFromCluster(Set<AABBic> cluster) {
        Set<BlockPos> set = new HashSet<>();
        for (AABBic area : cluster) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    for (int z = area.minZ(); z <= area.maxZ(); z++) {
                        set.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return set;
    }

    public AABBic getAABBFromPos(Vector3ic pos) {
        for (AABBic area : selectedAreas) {
            if (area.containsPoint(pos)) {
                return area;
            }
        }
        return null;
    }

    public void dumpCluster(Set<AABBic> cluster) {
        selectionClusters.remove(cluster);
        selectedAreas.removeAll(cluster);
    }

    enum Animation {
        DRAW, IDLE, SUCCESS, DUMP
    }
}
