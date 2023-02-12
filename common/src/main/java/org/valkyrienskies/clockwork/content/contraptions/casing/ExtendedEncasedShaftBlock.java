package org.valkyrienskies.clockwork.content.contraptions.casing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.AbstractEncasedShaftBlock;
import com.simibubi.create.content.schematics.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.ItemRequirement;
import com.simibubi.create.foundation.block.ITE;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkBlocks;

import javax.annotation.Nullable;

public class ExtendedEncasedShaftBlock extends AbstractEncasedShaftBlock
        implements ITE<KineticTileEntity>, ISpecialBlockItemRequirement {

    private BlockEntry<CasingBlock> casing;

    public static ExtendedEncasedShaftBlock balloon(Properties properties) {
        return new ExtendedEncasedShaftBlock(properties, ClockWorkBlocks.BALLOON_CASING);
    }

//    public static EncasedShaftBlock brass(Properties properties) {
//        return new EncasedShaftBlock(properties, AllBlocks.BRASS_CASING);
//    }

    protected ExtendedEncasedShaftBlock(Properties properties, BlockEntry<CasingBlock> casing) {
        super(properties);
        this.casing = casing;
    }

    public BlockEntry<CasingBlock> getCasing() {
        return casing;
    }

    @Override
    public void fillItemCategory(CreativeModeTab pTab, NonNullList<ItemStack> pItems) {}

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().isClientSide)
            return InteractionResult.SUCCESS;
        context.getLevel()
                .levelEvent(2001, context.getClickedPos(), Block.getId(state));
        KineticTileEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                AllBlocks.SHAFT.getDefaultState()
                        .setValue(AXIS, state.getValue(AXIS)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity te) {
        return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), te);
    }

    @Override
    public Class<KineticTileEntity> getTileEntityClass() {
        return KineticTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticTileEntity> getTileEntityType() {
        return ClockWorkBlockEntities.EXTENDED_ENCASED_SHAFT.get();
    }

}
