package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.mod.common.toWorldCoordinates

open class UniversalJointItem<T: IUniversalJoint>(properties: Properties) : Item(properties) {
    var firstSelect: T? = null

    override fun useOn(context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val be = context.level.getBlockEntity(context.clickedPos) ?: return fail()

        if (!isJoint(be)) return fail()


        val tBe = be as T
        if (firstSelect == null) {
            firstSelect = tBe
            context.player!!.displayClientMessage(Component.literal("Connection Started..."), true)
        }
        else {
            val worldDistance = context.level.toWorldCoordinates(firstSelect!!.pos).distanceTo(context.level.toWorldCoordinates(tBe.pos))
            if (worldDistance > tBe.maxCreationDistance) { //todo add joint distance config
                context.player!!.displayClientMessage(Component.literal("Connection failed: Joints are too far apart!"), true)
                firstSelect = null
                return fail()
            }
            if (firstSelect == tBe) {
                context.player!!.displayClientMessage(Component.literal("Connection failed: Cannot connect a joint to itself!"), true)
                firstSelect = null
                return fail()
            }
            if (!firstSelect!!.tryConnect(context.level,be.blockPos)) {
                context.player!!.displayClientMessage(Component.literal("Connection failed."), true)
                firstSelect = null
                return fail()
            }
            context.player!!.displayClientMessage(Component.literal("Connected!"), true)


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