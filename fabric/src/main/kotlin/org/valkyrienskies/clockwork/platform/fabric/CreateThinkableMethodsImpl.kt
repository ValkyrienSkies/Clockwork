package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour
import io.github.fabricators_of_create.porting_lib.enchant.EnchantmentBonusBlock
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EnchantmentTableBlock

object CreateThinkableMethodsImpl {
    @JvmStatic
    fun observedItems(observer: SmartObserverBlockEntity): Double? {
        val inventory = observer.getBehaviour(InvManipulationBehaviour.TYPE)?.inventory ?: return null
        val filter = observer.getBehaviour(FilteringBehaviour.TYPE) ?: return null
        return TransferUtil.getTransaction().use { transaction ->
            inventory.nonEmptyViews().sumOf { view ->
                if (filter.test(view.resource.toStack())) view.extract(view.resource, view.amount, transaction).toDouble() else 0.0
            }
        }
    }

    @JvmStatic
    fun observedFluid(observer: SmartObserverBlockEntity): Double? {
        val inventory = observer.getBehaviour(TankManipulationBehaviour.OBSERVE)?.inventory ?: return null
        val filter = observer.getBehaviour(FilteringBehaviour.TYPE) ?: return null
        return inventory.nonEmptyViews().sumOf { view ->
            if (filter.test(FluidStack(view))) view.amount.toDouble() * 1000.0 / FluidConstants.BUCKET else 0.0
        }
    }

    @JvmStatic
    fun tankFluid(tank: FluidTankBlockEntity, capacity: Boolean): Double =
        (if (capacity) tank.tankInventory.capacity else tank.tankInventory.fluidAmount).toDouble() * 1000.0 / FluidConstants.BUCKET

    @JvmStatic
    fun enchantPower(level: Level, pos: BlockPos): Double? {
        if (EnchantmentTableBlock.BOOKSHELF_OFFSETS.any { !level.hasChunkAt(pos.offset(it)) }) return null
        return EnchantmentTableBlock.BOOKSHELF_OFFSETS.filter { EnchantmentTableBlock.isValidBookShelf(level, pos, it) }.sumOf {
            val shelf = pos.offset(it)
            val state = level.getBlockState(shelf)
            val block = state.block
            if (block is EnchantmentBonusBlock) block.getEnchantPowerBonus(state, level, shelf).toDouble()
            else if (state.`is`(Blocks.BOOKSHELF)) 1.0 else 0.0
        }
    }
}
