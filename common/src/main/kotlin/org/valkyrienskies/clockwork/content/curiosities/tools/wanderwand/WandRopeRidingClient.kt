package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.simibubi.create.AllTags.AllItemTags
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.UUID

object WandRopeRidingClient {
    private data class Rope(val id: UUID, val a: WandAnchor, val b: WandAnchor, val length: Double) {
        fun curve(level: ClientLevel): WandRopeCurve? {
            val start = a.world(level) ?: return null
            val end = b.world(level) ?: return null
            return WandRopeCurve(start, end, length)
        }
    }
    private data class Rider(val rope: Rope, val ride: WandRopeRiding.Ride, var synced: Long)
    private val ropes = mutableMapOf<UUID, Rope>()
    private val riders = mutableMapOf<UUID, Rider>()
    private var world: ClientLevel? = null
    private var pendingUntil = 0L
    var target: Vec3? = null
        private set

    private fun level(): ClientLevel? {
        val level = Minecraft.getInstance().level
        if (world !== level) {
            ropes.clear(); riders.clear(); target = null; pendingUntil = 0; world = level
        }
        return level
    }

    private fun readRope(tag: CompoundTag) = Rope(tag.getUUID("id"), WandAnchor.load(tag.getCompound("a")),
        WandAnchor.load(tag.getCompound("b")), tag.getDouble("length"))

    fun accept(data: CompoundTag) {
        val level = level() ?: return
        if (data.getString("kind") == "links") {
            ropes.clear()
            for (entry in data.getList("links", Tag.TAG_COMPOUND.toInt()).take(256)) {
                val tag = entry as CompoundTag
                if (tag.getBoolean("rope") && tag.hasUUID("id")) readRope(tag).let { ropes[it.id] = it }
            }
            return
        }
        val owner = data.getUUID("owner")
        if (!data.contains("link")) { riders.remove(owner); return }
        val rope = readRope(data.getCompound("link"))
        val previous = riders[owner]
        // Keep local prediction continuous between server heartbeats. The server bounds distance to its path.
        if (previous?.rope == rope) { previous.synced = level.gameTime; return }
        val parameter = data.getDouble("parameter").coerceIn(0.0, 1.0)
        val point = rope.curve(level)?.point(parameter) ?: return
        val hand = InteractionHand.values()[data.getInt("hand").coerceIn(0, 1)]
        riders[owner] = Rider(rope, WandRopeRiding.Ride(rope.id, hand, parameter, data.getDouble("speed"), point), level.gameTime)
        if (owner == Minecraft.getInstance().player?.uuid) {
            val mc = Minecraft.getInstance()
            mc.gui.setOverlayMessage(Component.translatable("mount.onboard", mc.options.keyShift.translatedKeyMessage), false)
        }
    }

    @JvmStatic fun ridingHand(player: Player): InteractionHand? {
        val level = level() ?: return null
        val rider = riders[player.uuid] ?: return null
        return rider.ride.hand.takeIf { level.gameTime - rider.synced <= 30 && WandRopeRiding.eligible(player) &&
            AllItemTags.CHAIN_RIDEABLE.matches(player.getItemInHand(it)) }
    }

    private fun pick(player: Player, level: ClientLevel): Pair<Rope, WandRopeCurve.Hit>? {
        if (!WandRopeRiding.eligible(player) || WandRopeRiding.hand(player) == null ||
            ChainConveyorRidingHandler.ridingChainConveyor != null) return null
        val range = WandRopeRiding.visibleRange(player)
        return ropes.values.mapNotNull { rope -> rope.curve(level)?.pick(player.eyePosition, player.lookAngle, range)?.let { rope to it } }
            .minByOrNull { it.second.distance }
    }

    /** Called from Minecraft's use action, so remapped keys work on both loaders. */
    @JvmStatic fun onUse(): Boolean {
        val level = level() ?: return false
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        if (mc.screen != null) return false
        if (ridingHand(player) != null) return true
        val (rope, _) = pick(player, level) ?: return false
        if (level.gameTime >= pendingUntil) {
            pendingUntil = level.gameTime + 10
            ClockworkPackets.sendToServer(WandRopeRidePacket(rope.id))
        }
        return true
    }

    fun tick() {
        val level = level() ?: return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.isPaused) return
        riders.entries.removeIf { it.key != player.uuid && (level.gameTime - it.value.synced > 30 || level.getPlayerByUUID(it.key) == null) }
        val rider = riders[player.uuid]
        target = if (rider == null && mc.screen == null) pick(player, level)?.let { it.first.curve(level)?.point(it.second.parameter) } else null
        if (rider == null) return
        if (ridingHand(player) == null || !ropes.containsKey(rider.rope.id) ||
            ChainConveyorRidingHandler.ridingChainConveyor != null) { stop(player); return }
        val ride = rider.ride
        val curve = rider.rope.curve(level) ?: run { stop(player); return }
        val step = WandRopeSlidePhysics.step(curve, ride.parameter, ride.speed, ServerSpeedProvider.get().toDouble().coerceIn(0.05, 2.0))
        val point = curve.point(step.parameter)
        val feet = point.add(0.0, -WandRopeRiding.gripHeight(player), 0.0)
        val oldFeet = ride.previousPoint.add(0.0, -WandRopeRiding.gripHeight(player), 0.0)
        val movement = WandRopeSlidePhysics.follow(player.position(), player.deltaMovement, feet, point.subtract(ride.previousPoint))
        val pathBox = player.boundingBox.move(oldFeet.subtract(player.position()))
        if (step.finished || feet.distanceToSqr(player.position()) > 36 ||
            !WandRopeRiding.clearPath(player, pathBox, feet.subtract(oldFeet)) ||
            !WandRopeRiding.clearPath(player, player.boundingBox, movement) ||
            !WandRopeRiding.clearPath(player, player.boundingBox, feet.subtract(player.position()))) {
            // Drop at the last safe position; vanilla collision handles the remaining momentum.
            stop(player); return
        }
        ride.parameter = step.parameter; ride.speed = step.speed; ride.previousPoint = point; ride.age++
        player.deltaMovement = movement
        player.fallDistance = 0f
    }

    private fun stop(player: Player) {
        if (riders.remove(player.uuid) == null) return
        player.fallDistance = 0f
        pendingUntil = (world?.gameTime ?: 0) + 10
        ClockworkPackets.sendToServer(WandRopeRidePacket(null))
    }
}
