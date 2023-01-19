package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.content.contraptions.base.KineticBlock;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.gui.ScreenOpener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class SequencedSeatBlock extends HorizontalKineticBlock implements ITE<SequencedSeatBlockEntity> {

    public SequencedSeatBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferredFacing= getPreferredHorizontalFacing(context);
        if (preferredFacing != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()))
            return withDirection(preferredFacing);
        return withDirection(context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        ItemStack held = player.getMainHandItem();
        if (AllItems.WRENCH.isIn(held))
            return InteractionResult.PASS;
        if (held.getItem() instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) held.getItem();
            if (blockItem.getBlock() instanceof KineticBlock && hasShaftTowards(level, pos, state, hit.getDirection()))
                return InteractionResult.PASS;
        }

        if (level.isClientSide)
            withTileEntityDo(level, pos, te -> this.displayScreen(te, player));

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face != Direction.UP;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    private BlockState withDirection(Direction direction) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, direction);
    }

    @Environment(value = EnvType.CLIENT)
    protected void displayScreen(SequencedSeatBlockEntity te, Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new SequencedSeatScreen(te));
    }

    @Override
    public Class<SequencedSeatBlockEntity> getTileEntityClass() {
        return SequencedSeatBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SequencedSeatBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.SEQUENCED_SEAT.get();
    }
}
