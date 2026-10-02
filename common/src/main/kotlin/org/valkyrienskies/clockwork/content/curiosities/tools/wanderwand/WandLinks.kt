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
import java.util.UUID

/** The visual endpoints and joint blueprint are saved together; physics IDs are session-local. */
class WandLinks : SavedData() {
    data class Link(val id: UUID, val a: WandAnchor, val b: WandAnchor, val rope: Boolean,
                    val length: Double, val pose0: VSJointPose, val pose1: VSJointPose) {
        var jointId: Int? = null
        var pending = false
        fun save() = CompoundTag().also {
            it.putUUID("id", id); it.put("a", a.save()); it.put("b", b.save())
            it.putBoolean("rope", rope); it.putDouble("length", length)
            it.put("pose0", savePose(pose0)); it.put("pose1", savePose(pose1))
        }
    }

    val links = linkedMapOf<UUID, Link>()
    val sessions = mutableMapOf<UUID, WanderwandServer.Session>()

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("links", ListTag().also { list -> links.values.forEach { list.add(it.save()) } })
        return tag
    }

    fun add(level: ServerLevel, a: WandAnchor, b: WandAnchor, rope: Boolean, length: Double): Link? {
        if (links.size >= 2048 || a == b) return null
        if (links.values.any { it.rope == rope && ((it.a == a && it.b == b) || (it.a == b && it.b == a)) }) return null
        val wa = a.world(level) ?: return null
        val wb = b.world(level) ?: return null
        // A fixed joint holds the existing relative pose, rather than snapping distant click points together.
        val common = wa.add(wb).scale(0.5)
        fun pose(anchor: WandAnchor): VSJointPose {
            val ship = level.shipObjectWorld.loadedShips.getById(anchor.shipId)
            val p = if (rope) anchor.local() else common
            val v = Vector3d(p.x, p.y, p.z)
            if (!rope) ship?.worldToShip?.transformPosition(v)
            val q = if (rope || ship == null) Quaterniond() else Quaterniond(ship.transform.shipToWorldRotation).invert()
            return VSJointPose(v, q)
        }
        val link = Link(UUID.randomUUID(), a, b, rope, length.coerceIn(0.5, 128.0), pose(a), pose(b))
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

    fun tick(level: ServerLevel) {
        for (link in links.values.toList()) {
            val ends = listOf(link.a, link.b)
            if (ends.any { it.shipId >= 0 && level.shipObjectWorld.allShips.getById(it.shipId) == null } ||
                ends.any { it.world(level) != null && level.hasChunkAt(it.pos) && level.getBlockState(it.pos).isAir }) {
                remove(level, link)
                WanderwandServer.breakEffect(level, link)
            } else ensureJoint(level, link)
        }
    }

    private fun ensureJoint(level: ServerLevel, link: Link) {
        if (link.a.shipId == link.b.shipId || link.pending) return
        if (link.a.world(level) == null || link.b.world(level) == null) return
        val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: return
        val a = link.a.shipId.takeIf { it >= 0 } ?: ground
        val b = link.b.shipId.takeIf { it >= 0 } ?: ground
        val joint: VSJoint = if (link.rope) VSDistanceJoint(a, link.pose0, b, link.pose1,
            minDistance = 0f, maxDistance = link.length.toFloat())
        else VSFixedJoint(a, link.pose0, b, link.pose1, VSJointMaxForceTorque(1e10f, 1e10f))
        if (link.jointId?.let { level.gtpa.getJointById(it)?.matchesByAnchors(joint) } == true) return
        // SavedData owns reconstruction. Do not also serialize a second copy in VS's joint store.
        joint.shouldBeSerialized = false
        link.pending = true
        level.gtpa.addJoint(joint) { id ->
            // GTPA invokes this on the physics thread. All record ownership stays on the server thread.
            level.server.execute {
                link.pending = false
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
