package org.valkyrienskies.clockwork.platform.forge

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.curiosities.harpoon.HarpoonArmPoint

object HarpoonArmMethodsImpl {
    @JvmStatic
    fun createPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        Point(type, level, pos, state)

    private class Point(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState) :
        HarpoonArmPoint(type, level, pos, state) {
        override fun insert(stack: ItemStack, simulate: Boolean): ItemStack {
            val launcher = launcher(stack) ?: return stack
            if (!simulate && !launcher.load(stack)) return stack
            return stack.copyWithCount(stack.count - 1)
        }
    }
}
