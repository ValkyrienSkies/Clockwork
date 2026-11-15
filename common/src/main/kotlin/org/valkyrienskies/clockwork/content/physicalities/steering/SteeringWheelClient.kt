package org.valkyrienskies.clockwork.content.physicalities.steering

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.Input
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkPackets

object SteeringWheelClient {
    private var world: ClientLevel? = null
    private var position: BlockPos? = null
    private var pendingUntil = 0L
    private var lastSent = 0L
    private var lastInput = 0

    fun use(pos: BlockPos) {
        val level = Minecraft.getInstance().level ?: return
        if (position == pos && world === level) { stop(false); return }
        stop()
        world = level
        position = pos.immutable()
        pendingUntil = level.gameTime + 20
        lastSent = 0
    }

    fun tick() {
        if (position == null) return
        val mc = Minecraft.getInstance()
        val player = mc.player
        val wheel = world?.getBlockEntity(position!!) as? SteeringWheelBlockEntity
        if (world !== mc.level || player == null || !mc.isWindowActive || mc.screen != null || wheel == null ||
            !wheel.canReach(player) || (wheel.driver != player.uuid && world!!.gameTime > pendingUntil)) {
            stop()
            return
        }
        if (wheel.driver == player.uuid) {
            pendingUntil = 0
            mc.gui.setOverlayMessage(Component.translatable("mount.onboard",
                mc.options.keyShift.translatedKeyMessage), false)
        }
    }

    @JvmStatic fun capture(input: Input) {
        val pos = position ?: return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val wheel = world?.getBlockEntity(pos) as? SteeringWheelBlockEntity ?: return
        if (world !== mc.level || wheel.driver != player.uuid || mc.screen != null || mc.isPaused) return
        if (input.shiftKeyDown) { stop(); return }
        val direction = (if (input.right) 1 else 0) - (if (input.left) 1 else 0)
        if (direction != lastInput || world!!.gameTime - lastSent >= 5) {
            ClockworkPackets.sendToServer(SteeringWheelInputPacket(pos, direction))
            lastInput = direction
            lastSent = world!!.gameTime
        }
        input.left = false
        input.right = false
        input.leftImpulse = 0f
    }

    private fun stop(send: Boolean = true) {
        val mc = Minecraft.getInstance()
        if (send && position != null && world === mc.level && mc.player != null)
            ClockworkPackets.sendToServer(SteeringWheelInputPacket(position!!, 2))
        position = null
        world = null
    }
}
