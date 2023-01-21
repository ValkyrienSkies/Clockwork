package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import com.simibubi.create.content.contraptions.components.structureMovement.chassis.AbstractChassisBlock;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.platform.CWItem;


public class BluperGlueItem extends CWItem {

    public BluperGlueItem(Properties properties) {
        super(properties.durability(99));
    }

    public static InteractionResult glueItemAlwaysPlacesWhenUsed(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hitResult != null) {
            BlockState blockState = world
                    .getBlockState(hitResult
                            .getBlockPos());
            if (blockState.getBlock() instanceof AbstractChassisBlock cb)
                if (cb.getGlueableSide(blockState, hitResult.getDirection()) != null)
                    return InteractionResult.PASS;
        }

        if (player.getItemInHand(hand).getItem() instanceof BluperGlueItem)
            return InteractionResult.FAIL;
        return InteractionResult.PASS;
    }

    public static void onBroken(Player player) {
    }

    @Environment(EnvType.CLIENT)
    public static void spawnParticles(Level world, BlockPos pos, Direction direction, boolean fullBlock) {
        Vec3 vec = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 plane = VecHelper.axisAlingedPlaneOf(vec);
        Vec3 facePos = VecHelper.getCenterOf(pos)
                .add(vec.scale(.5f));

        float distance = fullBlock ? 1f : .25f + .25f * (world.random.nextFloat() - .5f);
        plane = plane.scale(distance);
        ItemStack stack = new ItemStack(ClockWorkItems.BLUUGUU.get());

        for (int i = fullBlock ? 40 : 15; i > 0; i--) {
            Vec3 offset = VecHelper.rotate(plane, 360 * world.random.nextFloat(), direction.getAxis());
            Vec3 motion = offset.normalize()
                    .scale(1 / 16f);
            if (fullBlock)
                offset =
                        new Vec3(Mth.clamp(offset.x, -.5, .5), Mth.clamp(offset.y, -.5, .5), Mth.clamp(offset.z, -.5, .5));
            Vec3 particlePos = facePos.add(offset);
            world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), particlePos.x, particlePos.y,
                    particlePos.z, motion.x, motion.y, motion.z);
        }

    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    public boolean canBeDepleted() {
        return true;
    }

}