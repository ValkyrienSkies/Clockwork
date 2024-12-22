package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.entity.BlockEntity

open class UniversalJointItem<T: IUniversalJoint>(properties: Properties) : Item(properties) {
    var firstSelect: T? = null

    override fun useOn(context: UseOnContext): InteractionResult {

        val be = context.level.getBlockEntity(context.clickedPos) ?: return fail()

        if (!isJoint(be)) return fail()
        val tBe = be as T

        if (firstSelect == null) firstSelect = tBe
        else {
            println("l ${context.level.isClientSide}")
            val res = firstSelect!!.tryConnect(context.level, be.blockPos)
            firstSelect = null


            if (res) return InteractionResult.SUCCESS
            else InteractionResult.PASS
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