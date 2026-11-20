package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import net.minecraft.server.level.ServerPlayer

internal class GatemakyrTraversal {
    private var previous = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()
    private var centers = emptyMap<Entity, Vec3>()

    fun tick(level: ServerLevel, gates: Map<Pair<UUID, Int>, GatemakyrPlayerGate>) {
        if (gates.isEmpty()) {
            previous = emptyMap()
            centers = emptyMap()
            return
        }
        val nearby = mutableSetOf<Entity>()
        fun bounds(gate: GatemakyrPlayerGate): AABB {
            val frame = gate.passage.entrance
            val u = frame.u.scale(gate.source.width * 0.5)
            val v = frame.v.scale(gate.source.height * 0.5)
            return AABB(frame.position.add(u).add(v), frame.position.subtract(u).subtract(v))
                .minmax(AABB(frame.position.add(u).subtract(v), frame.position.subtract(u).add(v))).inflate(4.0)
        }
        for ((key, gate) in gates) {
            val current = bounds(gate)
            val bounds = previous[key]?.takeIf(gate::matches)?.let { current.minmax(bounds(it)) } ?: current
            nearby.addAll(level.getEntities(null as Entity?, bounds) { it !is Player && GatemakyrSurfaceCollision.eligible(it) })
        }

        val next = mutableMapOf<Entity, Vec3>()
        for (entity in nearby + centers.keys) {
            if (entity.level() !== level || !GatemakyrSurfaceCollision.eligible(entity)) continue
            if (entity.tickCount > 0) {
                val from = centers[entity] ?: entity.boundingBox.center.add(entity.xo - entity.x, entity.yo - entity.y, entity.zo - entity.z)
                advance(entity, from, gates)
            }
            if (entity in nearby) next[entity] = entity.boundingBox.center
        }
        centers = next
        previous = gates
    }

    private fun advance(entity: Entity, from: Vec3, gates: Map<Pair<UUID, Int>, GatemakyrPlayerGate>) {
        var start = from
        var frames = previous
        var departed: Pair<UUID, Int>? = null
        repeat(8) {
            val end = entity.boundingBox.center
            var nearest = Double.POSITIVE_INFINITY
            var crossed: GatemakyrPlayerGate? = null
            for ((key, gate) in gates) {
                if (key == departed) continue
                val old = frames[key]?.takeIf(gate::matches) ?: continue
                val fraction = gate.crossing(old, start, end) ?: continue
                if (fraction < nearest && GatemakyrSurfaceCollision.ridingBounds(entity).let { group ->
                        gate.admits(group, gate.crossingPoint(old, start, end, fraction).add(group.center.subtract(end)))
                    }) {
                    nearest = fraction
                    crossed = gate
                }
            }
            val gate = crossed ?: return
            val passage = gate.passage
            val old = frames.getValue(gate.packet.owner to gate.packet.side)
            val entranceContact = gate.crossingPoint(old, start, end, nearest)
            val contact = passage.point(entranceContact)
            val offset = entity.position().subtract(end)
            val arrival = GatemakyrSurfaceCollision.exitAnchor(entity, contact, passage.point(end), offset, gate) ?: return
            val feet = arrival.end.add(offset)
            val rotation = passage.angles(entity.xRot, entity.yRot)
            val velocity = passage.velocity(entity.deltaMovement, entranceContact, old.passage)
            entity.moveTo(feet.x, feet.y, feet.z, rotation.y, rotation.x)
            if (entity is LivingEntity) {
                entity.yHeadRot = rotation.y
                entity.yBodyRot = rotation.y
            }
            entity.deltaMovement = velocity
            entity.setOnGround(false)
            entity.hasImpulse = true
            entity.hurtMarked = true
            entity.fallDistance = 0f
            GatemakyrEntityTransitPacket.send(entity, arrival.contact.add(offset))
            fun carry(mount: Entity) {
                for (rider in mount.passengers) {
                    mount.positionRider(rider)
                    val angles = passage.angles(rider.xRot, rider.yRot)
                    rider.moveTo(rider.x, rider.y, rider.z, angles.y, angles.x)
                    rider.deltaMovement = velocity
                    rider.fallDistance = 0f
                    if (rider is ServerPlayer) {
                        if (entity.controllingPassenger === rider) rider.connection.send(ClientboundMoveVehiclePacket(entity))
                        rider.connection.teleport(rider.x, rider.y, rider.z, angles.y, angles.x)
                        rider.serverLevel().chunkSource.move(rider)
                    }
                    GatemakyrEntityTransitPacket.send(rider, rider.position())
                    carry(rider)
                }
            }
            carry(entity)
            start = arrival.contact
            departed = gate.partner.owner to gate.partner.side
            frames = gates
        }
    }
}
