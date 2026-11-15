package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.curiosities.harpoon.HarpoonArmPoint
import org.valkyrienskies.clockwork.content.curiosities.harpoon.HarpoonBlockEntity
import java.lang.ref.WeakReference
import java.util.WeakHashMap

object HarpoonArmMethodsImpl {
    // All arm points targeting one launcher must share its reservation, including nested simulations.
    // The participant holds its owner weakly so this cache does not retain unloaded block entities.
    private val reservations = WeakHashMap<HarpoonBlockEntity, PendingLoad>()

    @JvmStatic
    fun createPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        Point(type, level, pos, state)

    private class Point(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState) :
        HarpoonArmPoint(type, level, pos, state) {
        override fun insert(stack: ItemStack, transaction: TransactionContext): ItemStack {
            val launcher = launcher(stack) ?: return stack
            val reservation = reservations.getOrPut(launcher) { PendingLoad(launcher) }
            if (!reservation.reserve(stack, transaction)) return stack
            return stack.copyWithCount(stack.count - 1)
        }
    }

    /** Reserve the only slot during simulation, publishing inventory/network changes only on commit. */
    private class PendingLoad(launcher: HarpoonBlockEntity) : SnapshotParticipant<ItemStack>() {
        private val owner = WeakReference(launcher)
        private var item = ItemStack.EMPTY

        fun reserve(stack: ItemStack, transaction: TransactionContext): Boolean {
            if (!item.isEmpty) return false
            updateSnapshots(transaction)
            item = stack.copyWithCount(1)
            return true
        }

        override fun createSnapshot(): ItemStack = item.copy()
        override fun readSnapshot(snapshot: ItemStack) { item = snapshot }
        override fun onFinalCommit() {
            owner.get()?.load(item)
            item = ItemStack.EMPTY
        }
    }
}
