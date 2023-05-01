package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class PhysBearingBlock extends BearingBlock implements ITE<PhysBearingBlockEntity> {

    public PhysBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide)
                return InteractionResult.SUCCESS;
            withTileEntityDo(worldIn, pos, te -> {
                if (te.running) {
//                    te.disassemble();
                    return;
                }
                te.assembleNextTick = true;
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Class<PhysBearingBlockEntity> getTileEntityClass() {
        return PhysBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhysBearingBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.PHYS_BEARING.get();
    }

    @Override
    public net.minecraft.core.Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, net.minecraft.core.Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    public static int getLight(BlockState state) {
        return 8;
    }

}