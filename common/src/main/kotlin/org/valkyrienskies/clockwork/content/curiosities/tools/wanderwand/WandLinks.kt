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
import org.valkyrienskies.mod.common.vsPipeline
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.matchesByAnchors
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringSettings
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotPhysics
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotServer
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import java.util.UUID

/** The visual endpoints and joint blueprint are saved together; physics IDs are session-local. */
class WandLinks : SavedData() {
    data class Link(val id: UUID, val a: WandAnchor, val b: WandAnchor, val rope: Boolean,
                    var length: Double, var pose0: VSJointPose, var pose1: VSJointPose, val physical: Boolean = false, val spring: Boolean = false) {
        internal var settings = SpringSettings()
        var appliedLength = length
        var settingsDirty = false
        var releasedMaximum: Double? = null
        val ropeLimit: Double get() = maxOf(length, releasedMaximum ?: length)
        var lastAdjustment = Long.MIN_VALUE
        var jointId: Int? = null
        var alignmentId: Int? = null
        var pending = false
        var needsReframe = false
        var stress = 0f
        var sentStress = 0f
        var age = 0
        var previousDistance: Double? = null
        var missingTicks = 0
        var suspended = false
        var mass = 100.0
        var knotA: UUID? = null
        var knotB: UUID? = null
        var hits = 0
        var lastHit = Long.MIN_VALUE
        fun save() = CompoundTag().also {
            it.putUUID("id", id); it.put("a", a.save()); it.put("b", b.save())
            it.putBoolean("rope", rope); it.putDouble("length", length)
            if (spring) {
                it.put("settings", settings.save())
                it.putDouble("appliedLength", appliedLength)
            }
            it.putBoolean("physical", physical); it.putBoolean("spring", spring)
            it.put("pose0", savePose(pose0)); it.put("pose1", savePose(pose1))
            it.putBoolean("faceAligned", !needsReframe)
        }
    }

    val links = linkedMapOf<UUID, Link>()
    private var ropeLengthChanged = false
    val sessions = mutableMapOf<UUID, WanderwandServer.Session>()
    // Riding is transient; only the rope itself is saved.
    val riders = mutableMapOf<UUID, WandRopeRiding.Ride>()

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("links", ListTag().also { list -> links.values.forEach { list.add(it.save()) } })
        return tag
    }

    fun add(level: ServerLevel, a: WandAnchor, b: WandAnchor, rope: Boolean, length: Double, physical: Boolean = false, spring: Boolean = false): Link? {
        if (links.size >= 2048 || a == b) return null
        for (anchor in listOf(a, b).filter { it.winch }) {
            if (!physical || !rope || spring ||
                level.getBlockEntity(anchor.pos) !is org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity ||
                links.values.any { (it.a.winch && it.a.pos == anchor.pos) || (it.b.winch && it.b.pos == anchor.pos) }) return null
        }
        if ((!rope || spring) && !WandLinkPhysics.canAttach(a, b)) return null
        if (links.values.any { it.rope == rope && it.spring == spring && ((it.a == a && it.b == b) || (it.a == b && it.b == a)) }) return null
        a.world(level) ?: return null
        b.world(level) ?: return null
        fun ropePose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond()) }
        val poses = if (rope && !spring) ropePose(a) to ropePose(b) else facePoses(level, a, b)
        val link = Link(UUID.randomUUID(), a, b, rope, length.coerceIn(0.5, 128.0), poses.first, poses.second, physical && rope, spring)
        links[link.id] = link
        setDirty()
        ensureJoint(level, link)
        if (link.physical) RopeKnotServer.ensureKnots(level, link)
        return link
    }

    fun resizeRope(level: ServerLevel, link: Link, length: Double) {
        if (!length.isFinite() || !link.rope || link.spring || links[link.id] !== link) return
        link.length = length.coerceIn(0.5, 128.0)
        ropeLengthChanged = true
        link.settingsDirty = true
        setDirty()
        ensureJoint(level, link)
    }

    fun updateWinchRelease(level: ServerLevel, link: Link) {
        if (!link.physical || !link.rope || link.spring || (!link.a.winch && !link.b.winch)) return
        val anchors = listOf(link.a, link.b).filter { it.winch }
        if (anchors.isEmpty() || anchors.any { !level.hasChunkAt(it.pos) }) return
        val previousLimit = link.ropeLimit
        val maximum = anchors.mapNotNull { anchor ->
            val winch = level.getBlockEntity(anchor.pos) as? RopeWinchBlockEntity ?: return@mapNotNull null
            if (level.hasNeighborSignal(anchor.pos)) winch.maximum.value.toDouble() else null
        }.maxOrNull()
        val payoutMaximum = link.releasedMaximum ?: maximum
        if (payoutMaximum != null) {
            val a = link.a.world(level)
            val b = link.b.world(level)
            if (a != null && b != null) {
                val next = RopeWinchBlockEntity.releasedLength(link.length, a.distanceTo(b), payoutMaximum)
                if (next != link.length) {
                    link.length = next
                    ropeLengthChanged = true
                    setDirty()
                }
            }
        }
        link.releasedMaximum = maximum
        if (link.ropeLimit != previousLimit) link.settingsDirty = true
    }

    fun remove(level: ServerLevel, link: Link) {
        links.remove(link.id)
        link.knotA?.let { level.getEntity(it)?.discard() }
        link.knotB?.let { level.getEntity(it)?.discard() }
        link.jointId?.let { level.gtpa.removeJoint(it) }
        link.alignmentId?.let { level.gtpa.removeJoint(it) }
        setDirty()
    }

    fun tick(level: ServerLevel): Boolean {
        var changed = ropeLengthChanged && level.gameTime % 2L == 0L
        if (changed) ropeLengthChanged = false
        for (link in links.values.toList()) {
            val ends = listOf(link.a, link.b)
            val a = link.a.world(level)
            val b = link.b.world(level)
            val invalidWinch = ends.any { it.winch && level.hasChunkAt(it.pos) &&
                level.getBlockEntity(it.pos) !is org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity }
            val invalid = invalidWinch || ends.any { it.shipId >= 0 && level.shipObjectWorld.allShips.getById(it.shipId) == null } ||
                ends.any { it.world(level) != null && level.hasChunkAt(it.pos) && level.getBlockState(it.pos).isAir }
            if (!invalid && (a == null || b == null || ends.any { !level.hasChunkAt(it.pos) })) {
                // Unloaded bodies are not broken ropes. Resume with a fresh ramp on return.
                link.stress = 0f
                link.previousDistance = null; link.age = 0; link.missingTicks = 0; link.suspended = true
                continue
            }
            if (link.suspended) {
                if (link.jointId?.let { level.gtpa.getJointById(it) } == null ||
                    (link.spring && link.alignmentId?.let { level.gtpa.getJointById(it) } == null)) {
                    link.jointId?.let { level.gtpa.removeJoint(it) }
                    link.alignmentId?.let { level.gtpa.removeJoint(it) }
                    link.jointId = null
                    link.alignmentId = null
                }
                link.suspended = false
            }
            if (link.spring || (link.rope && link.physical)) refreshPhysicalMass(level, link)
            if (link.spring) {
                val next = SpringSettings.easeLength(link.appliedLength, link.length)
                if (next != link.appliedLength) {
                    link.appliedLength = next
                    link.settingsDirty = true
                    setDirty()
                }
            }
            val distance = if (a != null && b != null) a.distanceTo(b) else 0.0
            updateWinchRelease(level, link)
            val speed = (distance - (link.previousDistance ?: distance)) * 20
            link.previousDistance = distance
            val hasPhysics = link.a.shipId != link.b.shipId
            link.stress = if (!hasPhysics || !link.physical) 0f else {
                val ratio = if (link.spring) SpringJoints.stressRatio(distance - link.appliedLength, speed, link.settings)
                    else RopeKnotPhysics.stressRatio(distance - link.ropeLimit, speed)
                ratio.coerceIn(0.0, 1.0).toFloat()
            }
            val missing = link.jointId != null && !link.pending && (level.gtpa.getJointById(link.jointId!!) == null ||
                (link.spring && link.alignmentId?.let { level.gtpa.getJointById(it) } == null))
            link.missingTicks = if (missing) link.missingTicks + 1 else 0
            val overloaded = hasPhysics && if (link.spring) SpringJoints.overloaded(distance - link.appliedLength, speed, link.settings)
                else if (link.physical) RopeKnotPhysics.overloaded(distance - link.ropeLimit, speed)
                else WandLinkPhysics.overloaded(link.rope, distance - if (link.rope) link.length else 0.01, speed, link.age)
            if (invalid || overloaded || link.missingTicks > 10) {
                remove(level, link)
                WanderwandServer.breakEffect(level, link)
                changed = true
            } else {
                ensureJoint(level, link)
                if (link.physical) RopeKnotServer.ensureKnots(level, link)
                if (link.jointId != null) link.age++
            }
        }
        org.valkyrienskies.clockwork.content.curiosities.spring.SpringBendingController.publish(level.dimensionId, links.values)
        return changed
    }

    private fun facePoses(level: ServerLevel, a: WandAnchor, b: WandAnchor) = WandLinkPhysics.gluePoses(a, b,
        level.shipObjectWorld.loadedShips.getById(a.shipId)?.transform?.shipToWorldRotation ?: Quaterniond(),
        level.shipObjectWorld.loadedShips.getById(b.shipId)?.transform?.shipToWorldRotation ?: Quaterniond())

    private fun refreshPhysicalMass(level: ServerLevel, link: Link) {
        val shipA = level.shipObjectWorld.loadedShips.getById(link.a.shipId)
        val shipB = level.shipObjectWorld.loadedShips.getById(link.b.shipId)
        val mass = SpringJoints.effectiveMass(shipA?.inertiaData?.mass, shipB?.inertiaData?.mass,
            fixedA = shipA?.isStatic == true, fixedB = shipB?.isStatic == true)
        if (kotlin.math.abs(mass - link.mass) > link.mass * 0.01) {
            link.mass = mass
            link.settingsDirty = true
        }
    }

    private fun ensureJoint(level: ServerLevel, link: Link) {
        updateWinchRelease(level, link)
        if (link.a.shipId == link.b.shipId || link.pending) return
        if (link.spring && link.jointId != null && !link.settingsDirty) return
        if (link.a.world(level) == null || link.b.world(level) == null) return
        if (!link.spring && link.jointId != null && !link.settingsDirty && (link.rope || link.age > WandLinkPhysics.GLUE_RAMP_TICKS || link.age % 2 != 0)) return
        val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: return
        val a = link.a.shipId.takeIf { it >= 0 } ?: ground
        val b = link.b.shipId.takeIf { it >= 0 } ?: ground
        if (link.needsReframe && !link.rope) {
            val poses = facePoses(level, link.a, link.b)
            link.pose0 = poses.first; link.pose1 = poses.second; link.needsReframe = false
            setDirty()
        }
        if ((link.spring || (link.rope && link.physical)) && link.jointId == null) refreshPhysicalMass(level, link)
        if (!link.spring && !link.physical && link.jointId == null) {
            val massA = level.shipObjectWorld.loadedShips.getById(link.a.shipId)?.inertiaData?.mass
            val massB = level.shipObjectWorld.loadedShips.getById(link.b.shipId)?.inertiaData?.mass
            link.mass = WandLinkPhysics.effectiveMass(massA, massB)
        }
        if (link.spring) {
            val pair = SpringJoints.create(a, b, link.pose0, link.pose1, link.appliedLength, link.mass, link.settings)
            if (link.jointId != null) {
                val springId = link.jointId!!
                val alignmentId = link.alignmentId ?: return
                if (link.settingsDirty && level.gtpa.getJointById(springId)?.matchesByAnchors(pair.first) == true &&
                    level.gtpa.getJointById(alignmentId)?.matchesByAnchors(pair.second) == true) {
                    level.gtpa.updateJoint(springId, pair.first)
                    level.gtpa.updateJoint(alignmentId, pair.second)
                    link.settingsDirty = false
                }
                return
            }
            link.settingsDirty = false
            link.pending = true
            level.gtpa.addJoint(pair.first) { springId ->
                level.server.execute {
                    level.gtpa.addJoint(pair.second) { alignmentId ->
                        level.server.execute {
                            link.pending = false
                            if (links[link.id] !== link || springId < 0 || alignmentId < 0) {
                                if (springId >= 0) level.gtpa.removeJoint(springId)
                                if (alignmentId >= 0) level.gtpa.removeJoint(alignmentId)
                            } else {
                                link.jointId = springId
                                link.alignmentId = alignmentId
                            }
                        }
                    }
                }
            }
            return
        }
        val joint: VSJoint = if (link.rope) WandLinkPhysics.ropeJoint(a, b, link.pose0, link.pose1, link.mass,
            link.ropeLimit, link.physical, level.server.vsPipeline.getPhysicsBackendType())
        else WandLinkPhysics.glueJoint(a, b, link.pose0, link.pose1, link.mass, link.age)
        link.jointId?.let { id ->
            val current = level.gtpa.getJointById(id)
            // Never resurrect a native joint which snapped, or modify an ID now owned by another tool.
            if (current?.matchesByAnchors(joint) == true && ((link.rope && link.settingsDirty) || (!link.rope && link.age <= WandLinkPhysics.GLUE_RAMP_TICKS && link.age % 2 == 0)))
                level.gtpa.updateJoint(id, joint)
            link.settingsDirty = false
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
                    t.getBoolean("rope"), length.coerceIn(0.5, 128.0), loadPose(t.getCompound("pose0")), loadPose(t.getCompound("pose1")),
                    t.getBoolean("physical") && t.getBoolean("rope"), t.getBoolean("spring"))
                link.settings = SpringSettings.load(t.getCompound("settings"))
                if (link.spring) link.appliedLength = readAppliedLength(t, link.length)
                link.needsReframe = !t.getBoolean("faceAligned") && !link.rope
                data.links[link.id] = link
            }
        }
        private fun savePose(pose: VSJointPose) = CompoundTag().also {
            it.putDouble("x", pose.pos.x()); it.putDouble("y", pose.pos.y()); it.putDouble("z", pose.pos.z())
            it.putDouble("qx", pose.rot.x()); it.putDouble("qy", pose.rot.y()); it.putDouble("qz", pose.rot.z()); it.putDouble("qw", pose.rot.w())
        }
        internal fun readAppliedLength(tag: CompoundTag, target: Double): Double =
            tag.getDouble("appliedLength").takeIf { tag.contains("appliedLength", Tag.TAG_ANY_NUMERIC.toInt()) && it.isFinite() }
                ?.coerceIn(0.5, 128.0) ?: target

        internal fun loadPoseRotation(tag: CompoundTag) =
            Quaterniond(tag.getDouble("qx"), tag.getDouble("qy"), tag.getDouble("qz"), tag.getDouble("qw"))

        private fun loadPose(t: CompoundTag) = VSJointPose(Vector3d(t.getDouble("x"), t.getDouble("y"), t.getDouble("z")),
            loadPoseRotation(t))
    }
}
