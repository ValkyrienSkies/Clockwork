package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.worldWrappers.RayTraceWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkPackets;

import java.util.HashSet;
import java.util.Set;

public class BluperGlueHandler {

    public static InteractionResult glueListensForBlockPlacement(BlockPlaceContext context) {
        LevelAccessor world = context.getLevel();
        Entity entity = context.getPlayer();
        BlockPos pos = context.getClickedPos();

        if (entity == null || world == null || pos == null)
            return InteractionResult.PASS;
        if (world.isClientSide())
            return InteractionResult.PASS;

        Set<BluperGlueEntity> cached = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            BlockPos relative = pos.relative(direction);
            if (BluperGlueEntity.isBluGlued(world, pos, direction, cached))
                ClockWorkPackets.sendToClientsTrackingAndSelf(new BluperGlueEffectPacket(pos, direction, true), (ServerPlayer) entity);
        }

        if (entity instanceof ServerPlayer)
            return bluperglueInOffHandAppliesOnBlockPlace(
                    context.getLevel().getBlockState(
                            context.getClickedPos().relative(context.getClickedFace().getOpposite())
                    ),
                    pos,
                    (ServerPlayer) entity
            );
        return InteractionResult.PASS;
    }

    public static InteractionResult bluperglueInOffHandAppliesOnBlockPlace(BlockState placedAgainst, BlockPos pos, Player placer) {
        ItemStack itemstack = placer.getOffhandItem();
        if (!ClockWorkItems.BLUPERGLUE.isIn(itemstack))
            return InteractionResult.PASS;
        if (AllItems.WRENCH.isIn(placer.getMainHandItem()))
            return InteractionResult.PASS;
        if (placedAgainst == IPlacementHelper.ID)
            return InteractionResult.PASS;

        double distance = ReachEntityAttributes.getReachDistance(placer, placer.isCreative() ? 5 : 4.5);
        Vec3 start = placer.getEyePosition(1);
        Vec3 look = placer.getViewVector(1);
        Vec3 end = start.add(look.x * distance, look.y * distance, look.z * distance);
        Level world = placer.level;

        RayTraceWorld rayTraceWorld =
                new RayTraceWorld(world, (p, state) -> p.equals(pos) ? Blocks.AIR.defaultBlockState() : state);
        BlockHitResult ray =
                rayTraceWorld.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, placer));

        Direction face = ray.getDirection();
        if (face == null || ray.getType() == Type.MISS)
            return InteractionResult.PASS;

        BlockPos gluePos = ray.getBlockPos();
        if (!gluePos.relative(face)
                .equals(pos)) {
            return InteractionResult.SUCCESS;
        }

        if (BluperGlueEntity.isBluGlued(world, gluePos, face, null))
            return InteractionResult.PASS;

        BluperGlueEntity entity = new BluperGlueEntity(world, BluperGlueEntity.span(gluePos, gluePos.relative(face)));
        CompoundTag compoundnbt = itemstack.getTag();
        if (compoundnbt != null)
            EntityType.updateCustomEntityTag(world, placer, entity, compoundnbt);

        if (BluperGlueEntity.isValidFace(world, gluePos, face)) {
            if (!world.isClientSide) {
                world.addFreshEntity(entity);
                ClockWorkPackets.sendToClientsTracking(new BluperGlueEffectPacket(gluePos, face, true), entity);
            }
            itemstack.hurtAndBreak(1, placer, BluperGlueItem::onBroken);
        }
        return InteractionResult.PASS;
    }

}
