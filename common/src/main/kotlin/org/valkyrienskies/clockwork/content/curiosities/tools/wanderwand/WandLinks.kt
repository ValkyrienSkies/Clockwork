package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.matchesByAnchors
import org.valkyrienskies.clockwork.util.updateJoint
import java.util.UUID

/** The visual endpoints and joint blueprint are saved together; physics IDs are session-local. */
class WandLinks : SavedData() {
    data class Link(val id: UUID, val a: WandAnchor, val b: WandAnchor, val rope: Boolean,
                    val length: Double, var pose0: VSJointPose, var pose1: VSJointPose) {
        var jointId: Int? = null
        var pending = false
        var needsReframe = false
        var age = 0
        var previousDistance: Double? = null
        var missingTicks = 0
        var suspended = false
        var mass = 100.0
        fun save() = CompoundTag().also {
            it.putUUID("id", id); it.put("a", a.save()); it.put("b", b.save())
            it.putBoolean("rope", rope); it.putDouble("length", length)
            it.put("pose0", savePose(pose0)); it.put("pose1", savePose(pose1))
            it.putBoolean("faceAligned", !needsReframe)
        }
    }

    val links = linkedMapOf<UUID, Link>()
    val sessions = mutableMapOf<UUID, WanderwandServer.Session>()
    // Riding is transient; only the rope itself is saved.
    val riders = mutableMapOf<UUID, WandRopeRiding.Ride>()

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("links", ListTag().also { list -> links.values.forEach { list.add(it.save()) } })
        return tag
    }

    fun add(level: ServerLevel, a: WandAnchor, b: WandAnchor, rope: Boolean, length: Double): Link? {
        if (links.size >= 2048 || a == b) return null
        if (!rope && !WandLinkPhysics.canAttach(a, b)) return null
        if (links.values.any { it.rope == rope && ((it.a == a && it.b == b) || (it.a == b && it.b == a)) }) return null
        a.world(level) ?: return null
        b.world(level) ?: return null
        fun ropePose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond()) }
        val poses = if (rope) ropePose(a) to ropePose(b) else facePoses(level, a, b)
        val link = Link(UUID.randomUUID(), a, b, rope, length.coerceIn(0.5, 128.0), poses.first, poses.second)
        links[link.id] = link
        setDirty()
        ensureJoint(level, link)
        return link
    }

    fun remove(level: ServerLevel, link: Link) {
        links.remove(link.id)
        link.jointId?.let { level.gtpa.removeJoint(it) }
        setDirty()
    }

    fun tick(level: ServerLevel): Boolean {
        var changed = false
        for (link in links.values.toList()) {
            val ends = listOf(link.a, link.b)
            val a = link.a.world(level)
            val b = link.b.world(level)
            val invalid = ends.any { it.shipId >= 0 && level.shipObjectWorld.allShips.getById(it.shipId) == null } ||
                ends.any { it.world(level) != null && level.hasChunkAt(it.pos) && level.getBlockState(it.pos).isAir }
            if (!invalid && (a == null || b == null || ends.any { !level.hasChunkAt(it.pos) })) {
                // Unloaded bodies are not broken ropes. Resume with a fresh ramp on return.
                link.previousDistance = null; link.age = 0; link.missingTicks = 0; link.suspended = true
                continue
            }
            if (link.suspended) {
                if (link.jointId?.let { level.gtpa.getJointById(it) } == null) link.jointId = null
                link.suspended = false
            }
            val distance = if (a != null && b != null) a.distanceTo(b) else 0.0
            val speed = (distance - (link.previousDistance ?: distance)) * 20
            link.previousDistance = distance
            val hasPhysics = link.a.shipId != link.b.shipId
            val missing = link.jointId != null && !link.pending && level.gtpa.getJointById(link.jointId!!) == null
            link.missingTicks = if (missing) link.missingTicks + 1 else 0
            val overloaded = hasPhysics && WandLinkPhysics.overloaded(link.rope,
                distance - if (link.rope) link.length else 0.01, speed, link.age)
            if (invalid || overloaded || link.missingTicks > 10) {
                remove(level, link)
                WanderwandServer.breakEffect(level, link)
                changed = true
            } else {
                ensureJoint(level, link)
                if (link.jointId != null) link.age++
            }
        }
        return changed
    }

    private fun facePoses(level: ServerLevel, a: WandAnchor, b: WandAnchor) = WandLinkPhysics.gluePoses(a, b,
        level.shipObjectWorld.loadedShips.getById(a.shipId)?.transform?.shipToWorldRotation ?: Quaterniond(),
        level.shipObjectWorld.loadedShips.getById(b.shipId)?.transform?.shipToWorldRotation ?: Quaterniond())

    private fun ensureJoint(level: ServerLevel, link: Link) {
        if (link.a.shipId == link.b.shipId || link.pending) return
        if (link.a.world(level) == null || link.b.world(level) == null) return
        if (link.jointId != null && (link.rope || link.age > WandLinkPhysics.GLUE_RAMP_TICKS || link.age % 2 != 0)) return
        val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: return
        val a = link.a.shipId.takeIf { it >= 0 } ?: ground
        val b = link.b.shipId.takeIf { it >= 0 } ?: ground
        if (link.needsReframe && !link.rope) {
            val poses = facePoses(level, link.a, link.b)
            link.pose0 = poses.first; link.pose1 = poses.second; link.needsReframe = false
            setDirty()
        }
        if (link.jointId == null) link.mass = WandLinkPhysics.effectiveMass(
            level.shipObjectWorld.loadedShips.getById(link.a.shipId)?.inertiaData?.mass,
            level.shipObjectWorld.loadedShips.getById(link.b.shipId)?.inertiaData?.mass)
        val joint: VSJoint = if (link.rope) VSDistanceJoint(a, link.pose0, b, link.pose1,
            VSJointMaxForceTorque((link.mass * WandLinkPhysics.ROPE_BREAK_ACCELERATION).toFloat(), Float.MAX_VALUE),
            minDistance = 0f, maxDistance = link.length.toFloat(), stiffness = (link.mass * WandLinkPhysics.ROPE_STIFFNESS).toFloat(),
            damping = (link.mass * WandLinkPhysics.ROPE_DAMPING).toFloat())
        else WandLinkPhysics.glueJoint(a, b, link.pose0, link.pose1, link.mass, link.age)
        link.jointId?.let { id ->
            val current = level.gtpa.getJointById(id)
            // Never resurrect a native joint which snapped, or modify an ID now owned by another tool.
            if (current?.matchesByAnchors(joint) == true && !link.rope && link.age <= WandLinkPhysics.GLUE_RAMP_TICKS && link.age % 2 == 0)
                level.gtpa.updateJoint(id, joint)
            return
        }
        // SavedData owns reconstruction. Do not also serialize a second copy in VS's joint store.
        joint.shouldBeSerialized = false
        link.pending = true
        level.gtpa.addJoint(joint) { id ->
            // GTPA invokes this on the physics thread. All record ownership stays on the server thread.
            level.server.execute {
                link.pending = false
                if (id < 0) { link.age = 0; return@execute }
                if (links[link.id] !== link) level.gtpa.removeJoint(id) else link.jointId = id
            }
        }
    }

    companion object {
        fun get(level: ServerLevel): WandLinks = level.dataStorage.computeIfAbsent(::load, ::WandLinks, "clockwork_wand_links")
        internal fun load(tag: CompoundTag) = WandLinks().also { data ->
            for (entry in tag.getList("links", Tag.TAG_COMPOUND.toInt()).take(2048)) {
                val t = entry as CompoundTag
                if (!t.hasUUID("id")) continue
                val length = t.getDouble("length")
                if (!length.isFinite()) continue
                val link = Link(t.getUUID("id"), WandAnchor.load(t.getCompound("a")), WandAnchor.load(t.getCompound("b")),
                    t.getBoolean("rope"), length.coerceIn(0.5, 128.0), loadPose(t.getCompound("pose0")), loadPose(t.getCompound("pose1")))
                link.needsReframe = !t.getBoolean("faceAligned") && !link.rope
                data.links[link.id] = link
            }
        }
        private fun savePose(pose: VSJointPose) = CompoundTag().also {
            it.putDouble("x", pose.pos.x()); it.putDouble("y", pose.pos.y()); it.putDouble("z", pose.pos.z())
            it.putDouble("qx", pose.rot.x()); it.putDouble("qy", pose.rot.y()); it.putDouble("qz", pose.rot.z()); it.putDouble("qw", pose.rot.w())
        }
        private fun loadPose(t: CompoundTag) = VSJointPose(Vector3d(t.getDouble("x"), t.getDouble("y"), t.getDouble("z")),
            Quaterniond(t.getDouble("qx"), t.getDouble("qy"), t.getDouble("qz"), t.getDouble("qw")))
    }
}
