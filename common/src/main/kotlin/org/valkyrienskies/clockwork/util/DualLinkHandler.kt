package org.valkyrienskies.clockwork.util

import com.simibubi.create.AllItems
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.RaycastHelper
import dev.architectury.event.EventResult
import io.github.fabricators_of_create.porting_lib.fake_players.FakePlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingLinkBehavior
import org.valkyrienskies.clockwork.platform.PlatformUtils

object DualLinkHandler {
    @JvmStatic
    fun handler(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {
        val world = player.level

        if (player.isShiftKeyDown || player.isSpectator) return EventResult.pass()

        val behaviour = BlockEntityBehaviour.get(world, pos, FlapBearingLinkBehavior.TYPE)
            ?: return EventResult.pass()

        val heldItem = player.getItemInHand(hand)
        val ray = RaycastHelper.rayTraceRange(world, player, 10.0) ?: return EventResult.pass()
        if (AllItems.LINKED_CONTROLLER.isIn(heldItem)) return EventResult.pass()
        if (AllItems.WRENCH.isIn(heldItem)) return EventResult.pass()

        val fakePlayer = player is ServerPlayer && player.javaClass != ServerPlayer::class.java
        var fakePlayerChoice = false

        if (fakePlayer) {
            val blockState = world.getBlockState(pos)
            val localHit = ray.location
                .subtract(Vec3.atLowerCornerOf(pos))
                .add(Vec3.atLowerCornerOf(ray.direction.normal).scale(.25))
            fakePlayerChoice = localHit.distanceToSqr(behaviour.slotOne.getLocalOffset(blockState)) > localHit
                .distanceToSqr(behaviour.slotTwo.getLocalOffset(blockState))
        }

        for (first in mutableListOf(false, true)) {
            if (behaviour.testHit(first, ray.location) || fakePlayer && fakePlayerChoice == first) {
                if (!world.isClientSide()) behaviour.setFrequency(first, heldItem)
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f)
                return EventResult.interruptTrue()
            }
        }

        return EventResult.pass()
    }
}