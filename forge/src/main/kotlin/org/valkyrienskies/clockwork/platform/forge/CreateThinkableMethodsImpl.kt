package org.valkyrienskies.clockwork.platform.forge

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EnchantmentTableBlock

object CreateThinkableMethodsImpl {
    @JvmStatic
    fun observedItems(observer: SmartObserverBlockEntity): Double? {
        val inventory = observer.getBehaviour(InvManipulationBehaviour.TYPE)?.inventory ?: return null
        val filter = observer.getBehaviour(FilteringBehaviour.TYPE) ?: return null
        return (0 until inventory.slots).sumOf { slot ->
            val stack = inventory.extractItem(slot, inventory.getSlotLimit(slot), true)
            if (filter.test(stack)) stack.count.toDouble() else 0.0
        }
    }

    @JvmStatic
    fun observedFluid(observer: SmartObserverBlockEntity): Double? {
        val inventory = observer.getBehaviour(TankManipulationBehaviour.OBSERVE)?.inventory ?: return null
        val filter = observer.getBehaviour(FilteringBehaviour.TYPE) ?: return null
        return (0 until inventory.tanks).sumOf { tank ->
            val stack = inventory.getFluidInTank(tank)
            if (filter.test(stack)) stack.amount.toDouble() else 0.0
        }
    }

    @JvmStatic
    fun tankFluid(tank: FluidTankBlockEntity, capacity: Boolean): Double =
        (if (capacity) tank.tankInventory.capacity else tank.tankInventory.fluidAmount).toDouble()

    @JvmStatic
    fun enchantPower(level: Level, pos: BlockPos): Double? {
        if (EnchantmentTableBlock.BOOKSHELF_OFFSETS.any { !level.hasChunkAt(pos.offset(it)) }) return null
        return EnchantmentTableBlock.BOOKSHELF_OFFSETS.filter { EnchantmentTableBlock.isValidBookShelf(level, pos, it) }.sumOf {
            val shelf = pos.offset(it)
            level.getBlockState(shelf).getEnchantPowerBonus(level, shelf).toDouble()
        }
    }
}
