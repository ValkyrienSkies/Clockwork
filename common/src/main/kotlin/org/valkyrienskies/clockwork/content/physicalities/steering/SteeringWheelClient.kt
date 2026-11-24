package org.valkyrienskies.clockwork.content.physicalities.steering

import net.createmod.catnip.outliner.Outliner
import net.minecraft.world.phys.AABB
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.Input
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPackets

object SteeringWheelClient {
    private var world: ClientLevel? = null
    private var position: BlockPos? = null
    private var pendingUntil = 0L
    private var lastSent = 0L
    private var lastInput = 0
    private var lastBrake = false
    private var lastThrottle = 0
    private var lastThrottleAdjustment = 0

    fun use(pos: BlockPos) {
        val level = Minecraft.getInstance().level ?: return
        if (position == pos && world === level) { stop(false); return }
        stop()
        world = level
        position = pos.immutable()
        pendingUntil = level.gameTime + 20
        lastSent = 0
        lastInput = 0
        lastBrake = false
        lastThrottle = 0
        lastThrottleAdjustment = 0
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val level = mc.level
        if (level != null) mc.player?.handSlots?.forEach { stack ->
            if (stack.item !is DrivingControlItem) return@forEach
            for (hub in DrivingControlItem.selected(stack, level)) {
                Outliner.getInstance().showAABB(DrivingControlItem::class to hub, AABB(hub))
                    .colored(0x70D5A3).lineWidth(1f / 32f).disableLineNormals()
            }
        }
        if (position == null) return
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
        val throttle = (if (input.up) 1 else 0) - (if (input.down) 1 else 0)
        val braking = ClockworkModClient.HANDBRAKE_KEYBIND.isDown
        val throttleAdjustment = (if (ClockworkModClient.THROTTLE_UP_KEYBIND.isDown) 1 else 0) -
            (if (ClockworkModClient.THROTTLE_DOWN_KEYBIND.isDown) 1 else 0)
        if (direction != lastInput || throttle != lastThrottle || throttleAdjustment != lastThrottleAdjustment ||
            braking != lastBrake || world!!.gameTime - lastSent >= 5
        ) {
            ClockworkPackets.sendToServer(SteeringWheelInputPacket(pos, direction, throttle, braking, throttleAdjustment))
            lastBrake = braking
            lastInput = direction
            lastThrottle = throttle
            lastThrottleAdjustment = throttleAdjustment
            lastSent = world!!.gameTime
        }
        input.left = false
        input.right = false
        input.up = false
        input.down = false
        input.leftImpulse = 0f
        input.forwardImpulse = 0f
    }

    private fun stop(send: Boolean = true) {
        val mc = Minecraft.getInstance()
        if (send && position != null && world === mc.level && mc.player != null)
            ClockworkPackets.sendToServer(SteeringWheelInputPacket(position!!, 2))
        position = null
        world = null
    }
}
