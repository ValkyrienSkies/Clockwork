package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.simibubi.create.AllTags.AllItemTags
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.primitives.AABBd
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.UUID

object WandRopeRiding {
    class Ride(val link: UUID, val hand: InteractionHand, var parameter: Double, var speed: Double,
               var previousPoint: Vec3, var age: Int = 0)

    // Match Create's chain-riding tools, including a wrench in the offhand.
    fun hand(player: Player): InteractionHand? = InteractionHand.values().firstOrNull {
        AllItemTags.CHAIN_RIDEABLE.matches(player.getItemInHand(it))
    }
    fun eligible(player: Player) = player.isAlive && !player.isSpectator && !player.isPassenger &&
        !player.isShiftKeyDown && !player.abilities.flying && !player.isFallFlying && !player.isSleeping

    fun gripHeight(player: Player) = player.bbHeight.toDouble() + 0.5
    fun visibleRange(player: Player): Double {
        val range = if (player.isCreative) 5.0 else 4.5
        val eye = player.eyePosition
        return player.level().clipIncludeShips(ClipContext(eye, eye.add(player.lookAngle.scale(range)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).location.distanceTo(eye).coerceAtMost(range)
    }

    /** Vanilla collision queries alone do not include blocks in moving shipyards. */
    fun clearPath(player: Player, box: AABB, movement: Vec3): Boolean =
        WandRopeSlidePhysics.clearSweep(box, movement) { bounds ->
            val level = player.level()
            level.noCollision(player, bounds) && level.getShipsIntersecting(bounds).all { ship ->
                val local = AABBd(bounds.toJOML()).transform(ship.worldToShip).toMinecraft()
                level.noCollision(player, local)
            }
        }

    fun use(player: ServerPlayer, linkId: UUID?) {
        val data = WandLinks.get(player.serverLevel())
        if (linkId == null) { stop(player, data); return }
        if (!eligible(player) || data.riders.containsKey(player.uuid)) return
        val hand = hand(player) ?: return
        val link = data.links[linkId]?.takeIf { it.rope && !it.suspended } ?: return
        if (!validEnds(player.serverLevel(), link)) return
        val a = link.a.world(player.level()) ?: return
        val b = link.b.world(player.level()) ?: return
        val curve = WandRopeCurve(a, b, link.length)
        // Only the ID comes from the client. Reach, line of sight and the actual latch point are server checked.
        val hit = curve.pick(player.eyePosition, player.lookAngle, visibleRange(player)) ?: return
        val point = curve.point(hit.parameter)
        val feet = point.add(0.0, -gripHeight(player), 0.0)
        if (!clearPath(player, player.boundingBox, feet.subtract(player.position()))) return
        val speed = player.deltaMovement.dot(curve.derivative(hit.parameter).normalize()).coerceIn(-0.4, 0.4)
        val ride = Ride(linkId, hand, hit.parameter, speed, point)
        data.riders[player.uuid] = ride
        player.fallDistance = 0f
        sync(player, ride, link)
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.5f, 1.2f)
    }

    @JvmStatic fun isRiding(player: ServerPlayer) = WandLinks.get(player.serverLevel()).riders.containsKey(player.uuid)

    private fun validEnds(level: ServerLevel, link: WandLinks.Link) = listOf(link.a, link.b).all {
        level.hasChunkAt(it.pos) && !level.getBlockState(it.pos).isAir && it.world(level) != null
    }

    fun tick(level: ServerLevel, data: WandLinks) {
        for ((id, ride) in data.riders.toMap()) {
            val player = level.getPlayerByUUID(id) as? ServerPlayer
            if (player == null) { data.riders.remove(id); continue }
            val link = data.links[ride.link]
            if (!eligible(player) || !AllItemTags.CHAIN_RIDEABLE.matches(player.getItemInHand(ride.hand)) ||
                link == null || link.suspended || !validEnds(level, link)) { stop(player, data); continue }
            val curve = WandRopeCurve(link.a.world(level)!!, link.b.world(level)!!, link.length)
            val step = WandRopeSlidePhysics.step(curve, ride.parameter, ride.speed)
            val point = curve.point(step.parameter)
            val feet = point.add(0.0, -gripHeight(player), 0.0)
            val oldFeet = ride.previousPoint.add(0.0, -gripHeight(player), 0.0)
            val pathBox = player.boundingBox.move(oldFeet.subtract(player.position()))
            // Check both the rope's next stretch and the rider's approach. Never teleport into geometry.
            if (step.finished || feet.distanceToSqr(player.position()) > 36 ||
                !clearPath(player, pathBox, feet.subtract(oldFeet)) ||
                !clearPath(player, player.boundingBox, feet.subtract(player.position()))) {
                stop(player, data); continue
            }
            ride.parameter = step.parameter; ride.speed = step.speed; ride.previousPoint = point; ride.age++
            player.fallDistance = 0f
            if (ride.age % 10 == 0) sync(player, ride, link)
        }
    }

    private fun stop(player: ServerPlayer, data: WandLinks) {
        if (data.riders.remove(player.uuid) == null) return
        player.fallDistance = 0f
        sync(player, null, null)
    }

    private fun sync(player: ServerPlayer, ride: Ride?, link: WandLinks.Link?) {
        val tag = CompoundTag()
        tag.putString("kind", "ride"); tag.putUUID("owner", player.uuid)
        if (ride != null && link != null) {
            tag.put("link", link.save()); tag.putInt("hand", ride.hand.ordinal)
            tag.putDouble("parameter", ride.parameter); tag.putDouble("speed", ride.speed)
        }
        ClockworkPackets.sendToClientsTrackingAndSelf(WanderwandStatePacket(tag), player)
    }
}
