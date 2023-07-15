package org.valkyrienskies.clockwork.content.kinetics.casing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkBlocks;

public class ExtendedEncasedShaftBlock extends AbstractEncasedShaftBlock
        implements IBE<KineticBlockEntity>, ISpecialBlockItemRequirement, EncasedBlock {

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

    public Block getCasing() {
        return this;
    }

    @Override
    public void fillItemCategory(CreativeModeTab pTab, NonNullList<ItemStack> pItems) {}

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().isClientSide)
            return InteractionResult.SUCCESS;
        context.getLevel()
                .levelEvent(2001, context.getClickedPos(), Block.getId(state));
        KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                AllBlocks.SHAFT.getDefaultState()
                        .setValue(AXIS, state.getValue(AXIS)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity te) {
        return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), te);
    }

    @Override
    public Class<KineticBlockEntity> getBlockEntityClass() {
        return KineticBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.EXTENDED_ENCASED_SHAFT.get();
    }

}
