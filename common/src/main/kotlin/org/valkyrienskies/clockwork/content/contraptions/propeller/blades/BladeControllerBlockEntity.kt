package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class BladeControllerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {
    var blades = mutableListOf<ItemStack>()
    var clientBladeAngle = LerpedFloat.linear()
        .chase(0.0, 0.5, LerpedFloat.Chaser.EXP)

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        behaviours.add(BladeControlBehaviour(this))
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) clientBladeAngle.tickChaser()

        if (level?.isClientSide() == false) {
            val sLevel = level as ServerLevel
        }
    }

    fun insertBlade(blade: ItemStack): Boolean {
        if (blades.size < 8) {
            blades.add(blade)
            return true
        } else return false
    }

    fun removeBlade(index: Int): ItemStack {
        return blades.removeAt(index)
    }

    fun removeBlade(): ItemStack {
        if (blades.isEmpty()) return ItemStack.EMPTY
        return blades.removeAt(blades.size - 1)
    }
}