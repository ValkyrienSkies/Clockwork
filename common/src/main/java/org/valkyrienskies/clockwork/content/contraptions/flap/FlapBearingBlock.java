package org.valkyrienskies.clockwork.content.contraptions.flap;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class FlapBearingBlock extends BearingBlock implements IBE<FlapBearingBlockEntity> {
    public FlapBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<FlapBearingBlockEntity> getBlockEntityClass() {
        return FlapBearingBlockEntity.class;
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos,
                                 Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn).isEmpty()) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(worldIn, pos, te -> {
                    if (te.running) {
                        te.disassemble();
                        return;
                    }
                    te.assembleNextTick = true;
                });
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntityType<? extends FlapBearingBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.FLAP_BEARING.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult resultType = super.onWrenched(state, context);
        if (!context.getLevel().isClientSide && resultType.consumesAction())
            withBlockEntityDo(context.getLevel(), context.getClickedPos(), FlapBearingBlockEntity::disassemble);
        return resultType;
    }
}
