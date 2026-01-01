package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.mod.common.toWorldCoordinates

open class UniversalJointItem<T: IUniversalJoint>(properties: Properties) : Item(properties) {
    var firstSelect: T? = null

    override fun isFoil(stack: ItemStack?): Boolean {
        return firstSelect != null || super.isFoil(stack)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val be = context.level.getBlockEntity(context.clickedPos) ?: return fail()

        if (!isJoint(be)) return fail()


        val tBe = be as T
        if (firstSelect == null) {
            firstSelect = tBe
            context.player!!.displayClientMessage(Component.translatable("$MOD_ID.universal_shaft.connection_start"), true)
        }
        else {
            val worldDistance = context.level.toWorldCoordinates(firstSelect!!.pos).distanceTo(context.level.toWorldCoordinates(tBe.pos))
            if (worldDistance > tBe.maxCreationDistance) { //todo add joint distance config
                context.player!!.displayClientMessage(Component.translatable("$MOD_ID.universal_shaft.connection_failed.too_far"), true)
                firstSelect = null
                return fail()
            }
            if (firstSelect == tBe || firstSelect!!.pos == tBe.pos) {
                context.player!!.displayClientMessage(Component.translatable("$MOD_ID.universal_shaft.connection_failed.to_itself"), true)
                firstSelect = null
                return fail()
            }
            if (!firstSelect!!.tryConnect(context.level,be.blockPos)) {
                context.player!!.displayClientMessage(Component.translatable("$MOD_ID.universal_shaft.connection_failed"), true)
                firstSelect = null
                return fail()
            }
            context.player!!.displayClientMessage(Component.translatable("$MOD_ID.universal_shaft.connection_end"), true)
            context.level!!.playSound(null, be.blockPos, ClockworkSounds.HOSE_ATTACH.mainEvent, SoundSource.BLOCKS, 1.0f, 1.0f)

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
