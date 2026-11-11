package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.clockwork.ClockworkPackets

object GravitronControlsClient {
    val ROTATE = KeyMapping("key.vs_clockwork.gravitron_rotate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.vs_clockwork.clockwork_keys")
    val CONNECTED_MASS = KeyMapping("key.vs_clockwork.gravitron_connected_mass", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.vs_clockwork.clockwork_keys")
    private var ship = -1L
    private var yaw = 0.0
    private var pitch = 0.0
    private var scroll = 0.0

    fun register() {
        KeyMappingRegistry.register(ROTATE)
        KeyMappingRegistry.register(CONNECTED_MASS)
    }

    private fun held(): Long? {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return null
        if (mc.screen != null || !mc.mouseHandler.isMouseGrabbed || !player.isAlive || player.isSpectator || !GravitronEffects.isGravitron(player.mainHandItem)) return null
        return GravitronEffects.heldShip(player)
    }

    private fun updateShip(): Long? {
        val current = held()
        if ((current ?: -1L) != ship) {
            ship = current ?: -1L
            yaw = 0.0; pitch = 0.0; scroll = 0.0
        }
        return current
    }

    @JvmStatic
    fun rotate(mouseX: Double, mouseY: Double): Boolean {
        if (updateShip() == null || !ROTATE.isDown) return false
        if (mouseX.isFinite() && mouseY.isFinite()) {
            yaw += mouseX * 0.15
            pitch += mouseY * 0.15
        }
        return true
    }

    fun scroll(delta: Double): Boolean {
        if (updateShip() == null) return false
        if (delta.isFinite()) scroll += delta
        return true
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val current = updateShip()
        while (CONNECTED_MASS.consumeClick()) {
            val player = mc.player ?: continue
            if (mc.screen == null && GravitronEffects.isGravitron(player.mainHandItem))
                ClockworkPackets.sendToServer(GravitronInputPacket(GravitronInputPacket.Action.CONNECTED_MASS))
        }
        if (current == null) return
        if (yaw != 0.0 || pitch != 0.0)
            ClockworkPackets.sendToServer(GravitronInputPacket(GravitronInputPacket.Action.ROTATE, current,
                yaw.coerceIn(-45.0, 45.0).toFloat(), pitch.coerceIn(-45.0, 45.0).toFloat()))
        if (scroll != 0.0)
            ClockworkPackets.sendToServer(GravitronInputPacket(GravitronInputPacket.Action.DISTANCE, current, scroll.coerceIn(-8.0, 8.0).toFloat()))
        yaw = 0.0; pitch = 0.0; scroll = 0.0
    }
}
