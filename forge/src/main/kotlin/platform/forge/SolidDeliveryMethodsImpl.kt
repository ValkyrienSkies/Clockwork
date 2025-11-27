package org.valkyrienskies.clockwork.platform.forge

import com.simibubi.create.foundation.item.ItemHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.IItemHandler
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity

object SolidDeliveryMethodsImpl {

    @JvmStatic
    fun extractFrom(level: Level?, be: DeliveryCannonBlockEntity?): ItemStack {
        if (level == null) return ItemStack.EMPTY

        val capability = grabCapability(level, be)
        if (!capability.isPresent) return ItemStack.EMPTY
        val inv = capability.orElseThrow { AssertionError() }


        return ItemHelper.extract(inv, {true}, ItemHelper.ExtractionCountMode.UPTO, 64, false)
    }

    private fun grabCapability(level: Level?, be: DeliveryCannonBlockEntity?): LazyOptional<IItemHandler> {

        if (level == null) return LazyOptional.empty()
        if (be == null) return LazyOptional.empty()

        val pos: BlockPos = be.blockPos.relative(Direction.DOWN)
        val be: BlockEntity? = level.getBlockEntity(pos)

        return be!!.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
    }
}