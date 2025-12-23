package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.foundation.item.ItemHelper
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil
import io.github.fabricators_of_create.porting_lib.util.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity

object SolidDeliveryMethodsImpl {

    @JvmStatic
    fun extractFrom(level: Level?, be: DeliveryCannonBlockEntity?): ItemStack {
        if (level == null) return ItemStack.EMPTY

        val capability = grabCapability(level, be)
        if (capability == null) return ItemStack.EMPTY
        val inv = capability[Direction.UP]


        return ItemHelper.extract(inv, {true}, ItemHelper.ExtractionCountMode.UPTO, 64, false)
    }

    @JvmStatic
    fun pushTo(level: Level?, be: DeliveryChuteBlockEntity?): Boolean {

        level ?: return false
		val inv  = grabCapability(level, be)

        if (!be!!.itemStack.isEmpty && inv != null) {
            if (level.isClientSide && !be.isVirtual) return false

            TransferUtil.getTransaction().use { t ->
                inv
                val inserted = inv[Direction.UP]?.insert(
                    ItemVariant.of(be.itemStack),
                    be.itemStack.getCount().toLong(),
                    t
                ) ?: 0L
                if (inserted != 0L) t.commit()
                val held = be.itemStack

                val newStack = held.copy()
                newStack.shrink(ItemHelper.truncateLong(inserted))
                be.itemStack = newStack

                if (inserted != 0L) return true
            }

        }
        return false
    }

    private fun grabCapability(level: Level?, be: BlockEntity?): StorageProvider<ItemVariant>? {

        level ?: return null
        be ?: return null

        val pos: BlockPos = be.blockPos.relative(Direction.DOWN)
        val be: BlockEntity? = level.getBlockEntity(pos)

        return StorageProvider.createForItems(level, be!!.blockPos.below())
    }
}