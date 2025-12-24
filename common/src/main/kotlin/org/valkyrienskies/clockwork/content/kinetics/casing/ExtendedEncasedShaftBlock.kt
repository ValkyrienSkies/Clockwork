package org.valkyrienskies.clockwork.content.kinetics.casing


import com.simibubi.create.AllBlockEntityTypes
import com.simibubi.create.AllBlocks
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement
import com.simibubi.create.content.decoration.encasing.CasingBlock
import com.simibubi.create.content.decoration.encasing.EncasedBlock
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.foundation.block.IBE
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.core.NonNullList
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkBlocks
import java.util.function.Supplier

class ExtendedEncasedShaftBlock
//    public static EncasedShaftBlock brass(Properties properties) {
//        return new EncasedShaftBlock(properties, AllBlocks.BRASS_CASING);
//    }
protected constructor(properties: Properties, casing: Supplier<Block>) :
    EncasedShaftBlock(properties, casing),
    IBE<KineticBlockEntity>, SpecialBlockItemRequirement,
    EncasedBlock {
//    override fun getCasing(): Block {
//        return casing.get()
//    }

    override fun getBlockEntityType(): BlockEntityType<out KineticBlockEntity> {
        return ClockworkBlockEntities.ENCASED_SHAFT.get()
    }

    companion object {
        fun balloon(properties: Properties): ExtendedEncasedShaftBlock {
            return ExtendedEncasedShaftBlock(properties) {ClockworkBlocks.BALLOON_CASING.get()}
        }
    }
}
