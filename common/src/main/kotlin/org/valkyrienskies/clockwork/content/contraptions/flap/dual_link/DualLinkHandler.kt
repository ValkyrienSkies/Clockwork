package org.valkyrienskies.clockwork.content.contraptions.flap.dual_link

import com.simibubi.create.AllItems
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
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

object DualLinkHandler {

    @JvmStatic
    fun getFrontFacing(state: BlockState): Direction {


        val direction = state.getValue(BlockStateProperties.FACING)
        val axis_along = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)


        var original_direction = when(direction) {
            Direction.EAST -> Direction.NORTH
            Direction.WEST -> Direction.SOUTH
            Direction.NORTH -> Direction.WEST
            else -> Direction.EAST
        }

        if (direction.axis.isVertical && axis_along) original_direction = original_direction.clockWise

        return original_direction
    }

    @JvmStatic
    fun handler(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {

        val world = player.level()


        if (player.isShiftKeyDown || player.isSpectator) return EventResult.pass()

        val state = world.getBlockState(pos)

        if (!state.hasProperty(BlockStateProperties.FACING) || !state.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) return EventResult.pass()

        val type: BehaviourType<DualLinkBehaviour>
        if (face == getFrontFacing(state)) type =  DualLinkBehaviour.FRONT_TYPE
        else type = DualLinkBehaviour.BACK_TYPE

        val behaviour = BlockEntityBehaviour.get(world, pos, type)
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
