package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.entity.BlockEntity

open class UniversalJointItem<T: IUniversalJoint>(properties: Properties) : Item(properties) {
    var firstSelect: T? = null

    override fun useOn(context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val be = context.level.getBlockEntity(context.clickedPos) ?: return fail()

        if (!isJoint(be)) return fail()


        val tBe = be as T
        if (firstSelect == null) {
            firstSelect = tBe
            context.player!!.displayClientMessage(Component.literal("First selected"), true)
        }
        else {
            context.player!!.displayClientMessage(Component.literal("second selected"), true)
            if (firstSelect == tBe) return fail()
            if (!firstSelect!!.tryConnect(context.level,be.blockPos)) return fail()
            context.itemInHand.count -= 1
            firstSelect = null
        }
        return InteractionResult.SUCCESS
    }

    fun fail(): InteractionResult {
        firstSelect = null
        return InteractionResult.PASS
    }

    open fun isJoint(be: BlockEntity): Boolean {
        return be is IUniversalJoint
    }
}