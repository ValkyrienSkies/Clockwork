package org.valkyrienskies.clockwork

import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.core.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.DispenserBlock

object ClockworkFluids {
    fun registerTarBucket(bucket: BucketItem) {
        FuelRegistry.register(20000, bucket)
        DispenserBlock.registerBehavior(bucket, object : DefaultDispenseItemBehavior() {
            private val fallback = DefaultDispenseItemBehavior()

            override fun execute(source: BlockSource, stack: ItemStack): ItemStack {
                val pos = source.pos.relative(source.blockState.getValue(DispenserBlock.FACING))
                if (bucket.emptyContents(null, source.level, pos, null)) {
                    bucket.checkExtraContent(null, source.level, stack, pos)
                    return ItemStack(Items.BUCKET)
                }
                return fallback.dispense(source, stack)
            }
        })
    }
}
