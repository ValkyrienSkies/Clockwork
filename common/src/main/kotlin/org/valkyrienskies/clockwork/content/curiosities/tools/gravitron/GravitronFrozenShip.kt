package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips

/** Saved with the ship. Only Gravitron freezes acquire this visual; other static ships are untouched. */
class GravitronFrozenShip {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var frozenAt = 0L

    private fun broadcast(level: ServerLevel, ship: ServerShip, frozen: Boolean) {
        val packet = GravitronFreezePacket(ship.id, Vec3(x, y, z), frozen,
            (level.gameTime - frozenAt).coerceIn(0L, 40L).toFloat())
        if (frozen) {
            val point = ship.shipToWorld.transformPosition(Vector3d(x, y, z))
            ClockworkPackets.sendToNear(level, BlockPos.containing(point.x, point.y, point.z), 192, packet)
        } else {
            // A client may still know this ship after moving away from its original freeze position.
            level.players().forEach { ClockworkPackets.sendTo(packet, it) }
        }
    }

    companion object {
        fun freeze(level: ServerLevel, ship: LoadedServerShip, anchor: Vector3dc) {
            val field = GravitronFrozenShip().also {
                it.x = anchor.x(); it.y = anchor.y(); it.z = anchor.z(); it.frozenAt = level.gameTime
            }
            ship.setAttachment(field)
            field.broadcast(level, ship, true)
        }

        fun thaw(level: ServerLevel, ship: ServerShip) {
            (ship as? LoadedServerShip)?.removeAttachment(GravitronFrozenShip::class.java)?.broadcast(level, ship, false)
        }

        fun thawConnected(player: ServerPlayer) {
            val stack = player.mainHandItem
            if (stack.item !is CreativeGravitronItem || !player.isAlive || player.isSpectator || player.cooldowns.isOnCooldown(stack.item)) return
            val level = player.serverLevel()
            val eye = player.eyePosition
            val hit = level.clipIncludeShips(ClipContext(eye, eye.add(player.lookAngle.scale(1000.0)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player), true)
            if (hit.type != HitResult.Type.BLOCK) return
            val target = level.getLoadedShipManagingPos(hit.blockPos) ?: return
            val ships = level.shipObjectWorld.loadedShips.filter { it.chunkClaimDimension == level.dimensionId }.associateBy { it.id }
            val edges = HashMap<Long, MutableSet<Long>>()
            for (joint in level.gtpa.getAllJoints().values) {
                val a = joint.shipId0 ?: continue
                val b = joint.shipId1 ?: continue
                if (a == b || a !in ships || b !in ships) continue
                edges.getOrPut(a) { HashSet() }.add(b)
                edges.getOrPut(b) { HashSet() }.add(a)
            }
            val visited = HashSet<Long>()
            val queue = ArrayDeque<Long>()
            queue.add(target.id)
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                for (next in edges[id].orEmpty()) if (next !in visited) queue.add(next)
            }
            val frozen = visited.mapNotNull { ships[it] }.filter {
                it.isStatic && it.getAttachment(GravitronFrozenShip::class.java) != null && !VdexConstruction.locked(it)
            }
            if (frozen.isEmpty()) return
            if (GravitronState.getState(player).shipID in visited) GrabTool.dropShip(player, false)
            for (ship in frozen) {
                ship.isStatic = false
                thaw(level, ship)
            }
            GravitronAnimationPacket.send(player, GravitronAction.UNFREEZE, target.id, hit.blockPos.toJOMLD().add(.5, .5, .5))
            level.playSound(null, player.blockPosition(), ClockworkSounds.GRAVITRON_FREEZE.mainEvent!!,
                player.soundSource, 1f, .75f)
            player.displayClientMessage(Component.translatable("vs_clockwork.gravitron.unfreeze_connected", frozen.size), true)
        }

        fun tick(level: ServerLevel, ship: LoadedServerShip) {
            if (ship.chunkClaimDimension != level.dimensionId) return
            val field = ship.getAttachment(GravitronFrozenShip::class.java) ?: return
            if (!ship.isStatic) thaw(level, ship)
            // Also restores the field after login, changing dimensions, or entering tracking range.
            else if (level.gameTime % 20L == 0L) field.broadcast(level, ship, true)
        }
    }
}
