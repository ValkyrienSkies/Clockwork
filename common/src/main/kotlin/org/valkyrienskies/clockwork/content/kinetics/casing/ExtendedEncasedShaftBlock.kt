package org.valkyrienskies.clockwork.content.kinetics.casing

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.decoration.encasing.CasingBlock
import com.simibubi.create.content.decoration.encasing.EncasedBlock
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.foundation.block.IBE
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkBlocks

class ExtendedEncasedShaftBlock //    public static EncasedShaftBlock brass(Properties properties) {
//        return new EncasedShaftBlock(properties, AllBlocks.BRASS_CASING);
//    }
protected constructor(properties: Properties, private val casing: BlockEntry<CasingBlock>) :
    AbstractEncasedShaftBlock(properties),
    IBE<KineticBlockEntity>, ISpecialBlockItemRequirement,
    EncasedBlock {
    override fun getCasing(): Block {
        return this
    }


    override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        context.level
            .levelEvent(2001, context.clickedPos, getId(state))
        KineticBlockEntity.switchToBlockState(
            context.level, context.clickedPos,
            AllBlocks.SHAFT.defaultState
                .setValue(AXIS, state.getValue(AXIS))
        )
        return InteractionResult.SUCCESS
    }

    override fun getRequiredItems(state: BlockState, te: BlockEntity): ItemRequirement {
        return ItemRequirement.of(AllBlocks.SHAFT.defaultState, te)
    }

    override fun getBlockEntityClass(): Class<KineticBlockEntity> {
        return KineticBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out KineticBlockEntity> {
        return ClockworkBlockEntities.EXTENDED_ENCASED_SHAFT.get()
    }

    companion object {
        fun balloon(properties: Properties): ExtendedEncasedShaftBlock {
            return ExtendedEncasedShaftBlock(properties, ClockworkBlocks.BALLOON_CASING)
        }
    }
}