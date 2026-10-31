package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.quad_link

import com.simibubi.create.AllItems
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.RaycastHelper
import dev.architectury.event.EventResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingFrequencySlot

object QuadLinkHandler {

    @JvmStatic
    fun getBehaviourType(state: BlockState, face: Direction): BehaviourType<QuadLinkBehaviour> {
        return when (face) {
            GimbalBearingFrequencySlot.getSlotDirection(state, 1) -> QuadLinkBehaviour.TYPE1
            GimbalBearingFrequencySlot.getSlotDirection(state, 2) -> QuadLinkBehaviour.TYPE2
            GimbalBearingFrequencySlot.getSlotDirection(state, 3) -> QuadLinkBehaviour.TYPE3
            else -> QuadLinkBehaviour.TYPE4
        }
    }

    @JvmStatic
    fun handler(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {

        val world = player.level()

        if (player.isShiftKeyDown || player.isSpectator) return EventResult.pass()

        val state = world.getBlockState(pos)

        if (!state.hasProperty(BlockStateProperties.FACING)) return EventResult.pass()

        val type = getBehaviourType(state, face)

        val behaviour = BlockEntityBehaviour.get(world, pos, type)
            ?: return EventResult.pass()

        val heldItem = player.getItemInHand(hand)
        val ray = RaycastHelper.rayTraceRange(world, player, 10.0) ?: return EventResult.pass()

        // Let the controller's own bind-mode flow handle this, which shows the red outline and waits
        // for a button press instead of copying frequencies immediately.
        if (AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
            return EventResult.pass()
        }

        if (AllItems.WRENCH.isIn(heldItem)) return EventResult.pass()

        val fakePlayer = player is ServerPlayer && player.javaClass != ServerPlayer::class.java
        var fakePlayerChoice = false

        if (fakePlayer) {
            val blockState = world.getBlockState(pos)
            val localHit = ray.location
                .subtract(Vec3.atLowerCornerOf(pos))
                .add(Vec3.atLowerCornerOf(ray.direction.normal).scale(.25))
            fakePlayerChoice = localHit.distanceToSqr(behaviour.firstSlot.getLocalOffset(world, pos, blockState)) > localHit
                .distanceToSqr(behaviour.secondSlot.getLocalOffset(world, pos, blockState))
        }

        for (first in mutableListOf(false, true)) {
            if (behaviour.testHit(first, ray.location) || fakePlayer && fakePlayerChoice == first) {
                if (!world.isClientSide()) {
                    behaviour.setFrequency(first, heldItem)
                }
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f)
                return EventResult.interruptTrue()
            }
        }

        return EventResult.pass()
    }
}
