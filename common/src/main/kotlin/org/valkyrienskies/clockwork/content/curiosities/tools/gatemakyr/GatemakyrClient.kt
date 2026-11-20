package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.platform.InputConstants
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import java.util.UUID

object GatemakyrClient {
    val MODE = KeyMapping("key.vs_clockwork.gatemakyr_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.vs_clockwork.clockwork_keys")
    val SMALLER = KeyMapping("key.vs_clockwork.gatemakyr_smaller", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "category.vs_clockwork.clockwork_keys")
    val LARGER = KeyMapping("key.vs_clockwork.gatemakyr_larger", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "category.vs_clockwork.clockwork_keys")
    val CLEAR = KeyMapping("key.vs_clockwork.gatemakyr_clear", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.vs_clockwork.clockwork_keys")
    class State {
        val animation = GatemakyrAnimation()
        var lastSync = 0L
        var muzzle: Vec3? = null
        var muzzleFirstPerson = false
        var muzzleTime = -1L
    }
    class Flight(val packet: GatemakyrEffectPacket, var origin: Vec3? = null)
    class Rift(val packet: GatemakyrEffectPacket, var lastSync: Long)
    data class Closing(val target: GatemakyrTarget, val side: Int, val startedAt: Long)
    private data class Owner(val player: Player?)
    private val owners = ArrayDeque<Owner>()
    private val states = mutableMapOf<UUID, State>()
    val flights = mutableListOf<Flight>()
    val rifts = linkedMapOf<Pair<UUID, Int>, Rift>()
    val closing = mutableListOf<Closing>()
    private var world: ClientLevel? = null
    private var collisionTick = Long.MIN_VALUE
    private var collisionSnapshot = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()

    internal fun rayGates(level: Level): Map<Pair<UUID, Int>, GatemakyrPlayerGate> {
        val mc = Minecraft.getInstance()
        return if (mc.isSameThread && level === mc.level) collisionGates() else emptyMap()
    }

    internal fun collisionGates(): Map<Pair<UUID, Int>, GatemakyrPlayerGate> {
        val level = Minecraft.getInstance().level ?: return emptyMap()
        if (rifts.size < 2) return emptyMap()
        if (collisionTick != level.gameTime) {
            collisionSnapshot = GatemakyrPlayerGate.snapshot(level, rifts, false) { it.packet }
            collisionTick = level.gameTime
        }
        return collisionSnapshot
    }
    private var activeSide = -1
    private var slot = -1
    private var stack: ItemStack? = null
    var width = 3
        private set
    var height = 3
        private set
    var free = false
        private set
    var distance = 8.0
        private set
    var preview: GatemakyrTarget? = null
        private set

    fun register() { listOf(MODE, SMALLER, LARGER, CLEAR).forEach(KeyMappingRegistry::register) }

    @JvmStatic
    fun holding(): Boolean = Minecraft.getInstance().player?.mainHandItem?.item is GatemakyrItem

    private fun ready(): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        return holding() && mc.screen == null && mc.mouseHandler.isMouseGrabbed && mc.isWindowActive && player.isAlive && !player.isSpectator
    }

    fun state(player: Player?): State? = player?.let { states[it.uuid] }
    fun renderedPlayer(): Player? = owners.lastOrNull()?.player

    private fun checkWorld() {
        val current = Minecraft.getInstance().level
        if (world === current) return
        world = current
        collisionTick = Long.MIN_VALUE
        collisionSnapshot = emptyMap()
        GatemakyrPlayerCrossing.clear()
        GatemakyrView.clear()
        GatemakyrSounds.clear()
        states.clear(); flights.clear(); rifts.clear(); owners.clear()
        closing.clear()
        activeSide = -1; slot = -1; stack = null; preview = null
    }

    @JvmStatic
    fun onMouseInput(button: Int, pressed: Boolean): Boolean {
        val options = Minecraft.getInstance().options
        val side = when {
            options.keyAttack.matchesMouse(button) -> 0
            options.keyUse.matchesMouse(button) -> 1
            else -> return false
        }
        return input(side, pressed)
    }

    @JvmStatic
    fun onKeyInput(key: Int, scanCode: Int, pressed: Boolean) {
        val options = Minecraft.getInstance().options
        when {
            options.keyAttack.matches(key, scanCode) -> input(0, pressed)
            options.keyUse.matches(key, scanCode) -> input(1, pressed)
        }
    }

    private fun input(side: Int, pressed: Boolean): Boolean {
        checkWorld()
        if (!ready()) return false
        val player = Minecraft.getInstance().player ?: return false
        val now = player.level().gameTime
        if (pressed && activeSide == -1) {
            equip(player)
            activeSide = side
            val state = states.getOrPut(player.uuid) { State() }
            state.animation.start(side, now)
            state.lastSync = now
            GatemakyrSounds.charge(player)
            send(GatemakyrInputPacket.Action.CHARGE)
        } else if (!pressed && activeSide == side) {
            send(GatemakyrInputPacket.Action.RELEASE)
            states[player.uuid]?.animation?.release(now, false)
            GatemakyrSounds.stopCharge(player.uuid)
            activeSide = -1
            preview = null
        }
        return true
    }

    private fun equip(player: Player) {
        if (stack?.item === player.mainHandItem.item && slot == player.inventory.selected) {
            stack = player.mainHandItem
            return
        }
        stack = player.mainHandItem
        slot = player.inventory.selected
        free = stack?.tag?.getBoolean(GatemakyrItem.FREE) ?: false
        distance = GatemakyrItem.distance(player.mainHandItem)
        width = GatemakyrItem.size(player.mainHandItem, GatemakyrItem.WIDTH)
        height = GatemakyrItem.size(player.mainHandItem, GatemakyrItem.HEIGHT)
    }

    @JvmStatic
    fun scroll(delta: Double): Boolean {
        if (!ready() || activeSide < 0 || !free) return false
        if (delta.isFinite() && delta != 0.0) {
            distance = (distance + delta * 0.5).coerceIn(2.0, 48.0)
            send(GatemakyrInputPacket.Action.SETTINGS)
        }
        return true
    }

    private fun send(action: GatemakyrInputPacket.Action) =
        ClockworkPackets.sendToServer(GatemakyrInputPacket(action, activeSide.coerceAtLeast(0), free, distance, width, height))

    fun tick() {
        checkWorld()
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return
        val now = level.gameTime
        if (activeSide >= 0 && (!ready() || stack?.item !== player.mainHandItem.item || slot != player.inventory.selected)) {
            send(GatemakyrInputPacket.Action.CANCEL)
            states[player.uuid]?.animation?.release(now, false)
            activeSide = -1
            preview = null
        }
        if (ready()) equip(player)
        while (MODE.consumeClick()) if (ready()) {
            free = !free
            send(GatemakyrInputPacket.Action.SETTINGS)
            player.displayClientMessage(Component.translatable("vs_clockwork.gatemakyr.mode." + if (free) "free" else "surface"), true)
        }
        for ((key, delta) in listOf(SMALLER to -1, LARGER to 1)) while (key.consumeClick()) if (ready()) {
            val window = mc.window.window
            val shift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
            val ctrl = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
            if (!shift || ctrl) width = (width + delta).coerceIn(1, 32)
            if (!ctrl || shift) height = (height + delta).coerceIn(1, 32)
            send(GatemakyrInputPacket.Action.SETTINGS)
            player.displayClientMessage(Component.translatable("vs_clockwork.gatemakyr.size", width, height), true)
        }
        while (CLEAR.consumeClick()) if (ready()) {
            send(GatemakyrInputPacket.Action.CLEAR)
            states[player.uuid]?.animation?.release(now, false)
            GatemakyrSounds.stopCharge(player.uuid)
            activeSide = -1
            preview = null
        }
        if (!mc.isPaused && activeSide >= 0) {
            preview = GatemakyrTarget.acquire(player, free, distance, width, height, rifts[player.uuid to (1 - activeSide)]?.packet?.target)
            if (now % 10L == 0L) send(GatemakyrInputPacket.Action.CHARGE)

        }
        states.entries.removeIf { (id, state) ->
            if (state.animation.charging && id != player.uuid && now - state.lastSync > 30) state.animation.release(now, false)
            now - state.lastSync > 200 && id != player.uuid
        }
        flights.removeAll { now - it.packet.startedAt > it.packet.duration + 20 }
        rifts.entries.removeIf { now - it.value.lastSync > 60 }
        closing.removeAll { now - it.startedAt > 16 }
        GatemakyrSounds.tick()
    }

    fun accept(packet: GatemakyrEffectPacket) {
        checkWorld()
        val level = world ?: return
        if (level.dimension().location() != packet.dimension) return
        val now = level.gameTime
        collisionTick = Long.MIN_VALUE
        val state = states.getOrPut(packet.owner) { State() }
        state.lastSync = now
        when (packet.kind) {
            GatemakyrEffectPacket.Kind.CLEAR -> {
                flights.removeAll { it.packet.owner == packet.owner }
                rifts.entries.removeIf { it.key.first == packet.owner }
                state.animation.release(now, false)
                GatemakyrSounds.stopCharge(packet.owner)
                if (packet.owner == Minecraft.getInstance().player?.uuid) { activeSide = -1; preview = null }
            }
            GatemakyrEffectPacket.Kind.CHARGE -> {
                if (packet.owner != Minecraft.getInstance().player?.uuid || activeSide == packet.side) {
                    state.animation.start(packet.side, packet.startedAt)
                    level.getPlayerByUUID(packet.owner)?.let { GatemakyrSounds.charge(it) }
                }
            }
            GatemakyrEffectPacket.Kind.CANCEL -> {
                state.animation.release(now, false)
                GatemakyrSounds.stopCharge(packet.owner)
                level.getPlayerByUUID(packet.owner)?.let { GatemakyrSounds.oneShot(ClockworkSounds.GATEMAKYR_CANCEL, it.eyePosition, 0.5f) }
            }
            GatemakyrEffectPacket.Kind.IMPACT -> {
                val existing = flights.firstOrNull { it.packet.owner == packet.owner && it.packet.side == packet.side &&
                    it.packet.startedAt == packet.startedAt }
                if (existing != null) {
                    flights.remove(existing)
                    if (packet.target != null) flights.add(Flight(packet, existing.origin))
                }
            }
            GatemakyrEffectPacket.Kind.SHOT -> {
                state.animation.release(now, true)
                GatemakyrSounds.stopCharge(packet.owner)
                GatemakyrSounds.oneShot(ClockworkSounds.GATEMAKYR_FIRE, packet.origin)
                if (flights.size >= 128) flights.removeAt(0)
                flights.add(Flight(packet))
            }
            GatemakyrEffectPacket.Kind.RIFT -> {
                if (rifts[packet.owner to packet.side]?.packet?.startedAt != packet.startedAt && now - packet.startedAt < 10)
                    packet.target?.frame(level)?.impact?.let { GatemakyrSounds.oneShot(ClockworkSounds.PORTAL_OPEN, it) }
                rifts[packet.owner to packet.side] = Rift(packet, now)
                val other = packet.owner to (1 - packet.side)
                if (packet.partner != null) {
                    val snapshot = GatemakyrEffectPacket(packet.dimension, packet.owner, packet.kind, other.second,
                        packet.partnerStartedAt, target = packet.partner)
                    rifts[other] = Rift(snapshot, now)
                } else rifts.remove(other)
                while (rifts.size > 128) rifts.remove(rifts.keys.first())
            }
            GatemakyrEffectPacket.Kind.CLOSE -> {
                rifts.remove(packet.owner to packet.side)
                packet.target?.let { target ->
                    if (closing.size >= 32) closing.removeAt(0)
                    closing.add(Closing(target, packet.side, now))
                    target.frame(level)?.position?.let { GatemakyrSounds.oneShot(ClockworkSounds.PORTAL_CLOSE, it, 0.55f) }
                }
            }
        }
    }

    @JvmStatic
    fun beginItem(entity: LivingEntity, stack: ItemStack, context: ItemDisplayContext) {
        val player = entity as? Player
        val held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        val right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        owners.addLast(Owner(player?.takeIf { held && stack.item is GatemakyrItem && it.mainHandItem === stack &&
            (it.mainArm == HumanoidArm.RIGHT) == right }))
    }

    @JvmStatic
    fun endItem() { if (owners.isNotEmpty()) owners.removeLast() }
}
