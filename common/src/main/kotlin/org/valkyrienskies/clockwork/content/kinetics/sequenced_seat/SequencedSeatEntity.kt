package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkEntities
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.*

class SequencedSeatEntity(type: EntityType<*>?, level: Level?) :
    SeatEntity(type, level) {
    private var prevKeys = setOf<InputKey>()
    override fun tick() {
        if (level.isClientSide) {
            if (this.firstPassenger is LocalPlayer) {
                checkKeybinds()
            }
        } else {
            val blockPresent = level.getBlockState(blockPosition())
                .block is SequencedSeatBlock
            if (isVehicle && blockPresent) return
            discard()
            val be = level.getBlockEntity(blockPosition())
            (be as? SequencedSeatBlockEntity)?.updateInput(emptySet())
        }
    }

    private fun checkKeybinds() {
        val input = Objects.requireNonNull(Minecraft.getInstance().player)!!.input
        val keys: MutableSet<InputKey> = HashSet()
        if (input.left) {
            keys.add(InputKey.LEFT)
        }
        if (input.right) {
            keys.add(InputKey.RIGHT)
        }
        if (input.up) {
            keys.add(InputKey.FORWARD)
        }
        if (input.down) {
            keys.add(InputKey.BACKWARD)
        }
        if (input.jumping) {
            keys.add(InputKey.JUMP)
        }
        if (keys != prevKeys) {
            sendUpdate(keys)
        }
        prevKeys = keys
    }

    private fun sendUpdate(keys: Set<InputKey>) {
        ClockworkPackets.sendToServer(SequencedSeatDrivingPacket(id, keys))
    }

    companion object {
        fun create(level: Level, pos: BlockPos): SequencedSeatEntity {
            return ClockworkEntities.SEQUENCED_SEAT.create(level)!!
        }
    }
}