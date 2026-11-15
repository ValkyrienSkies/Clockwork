package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.configureMetalDuctEdge
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction
import org.valkyrienskies.clockwork.util.ConnectedShipMass
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.matchesByAnchors
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.impl.api_impl.config.ConfigPhysicsBackendType
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsPipeline
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

interface BlockAnchorEndpoint : IUniversalJoint {
    val blockEntity: BlockEntity get() = this as BlockEntity
    val linkKind: BlockAnchorLinks.Kind
    var linkId: UUID?
    var connectedPos: BlockPos?
    var main: Boolean
    val stress: Float
    fun setConnection(id: UUID?, target: BlockPos?, primary: Boolean)
    fun updateStress(value: Float)
}

class BlockAnchorLinks : SavedData() {
    enum class Kind { SHAFT, EXTENDON, HOSE }
    data class Link(val id: UUID, var a: WandAnchor, var b: WandAnchor, val kind: Kind = Kind.SHAFT,
                    var length: Double = 0.0, var rotationA: Quaterniond? = null, var rotationB: Quaterniond? = null) {
        var jointId: Int? = null
        var alignmentId: Int? = null
        var edge: DuctEdge? = null
        val legacyIds = mutableSetOf<Int>()
        var legacyA: WandAnchor? = null
        var legacyB: WandAnchor? = null
        var pending = false
        var generation = 0
        var age = 0
        var missing = 0
        var overload = 0
        var previousDistance: Double? = null
        var mass = 100.0
        var limit = Double.NaN
        fun save() = CompoundTag().also {
            it.putUUID("id", id); it.put("a", a.save()); it.put("b", b.save())
            it.putString("kind", kind.name); it.putDouble("length", length)
            fun rotation(name: String, q: Quaterniond?) { if (q != null) it.put(name, CompoundTag().apply {
                putDouble("x", q.x); putDouble("y", q.y); putDouble("z", q.z); putDouble("w", q.w)
            }) }
            rotation("rotationA", rotationA); rotation("rotationB", rotationB)
            if (legacyIds.isNotEmpty()) {
                it.putIntArray("legacyJoints", legacyIds.toIntArray())
                it.put("legacyA", (legacyA ?: a).save()); it.put("legacyB", (legacyB ?: b).save())
            }
        }
    }

    val links = linkedMapOf<UUID, Link>()
    fun at(pos: BlockPos) = links.values.firstOrNull { it.a.pos == pos || it.b.pos == pos }

    fun add(level: ServerLevel, first: BlockAnchorEndpoint, second: BlockAnchorEndpoint): Link? {
        val firstBe = first.blockEntity; val secondBe = second.blockEntity
        if (first === second || first.linkKind != second.linkKind || firstBe.isRemoved || secondBe.isRemoved ||
            firstBe.level !== level || secondBe.level !== level || at(first.pos) != null || at(second.pos) != null || links.size >= 2048) return null
        fun anchor(endpoint: BlockAnchorEndpoint) = endpoint.blockEntity.let { be ->
            WandAnchor.at(level, be.blockPos, be.blockState.getValue(BlockStateProperties.FACING)).copy(offset = Vec3.ZERO) }
        val a = anchor(first); val b = anchor(second)
        val distance = a.world(level)?.distanceTo(b.world(level) ?: return null) ?: return null
        if (distance > minOf(first.maxCreationDistance, second.maxCreationDistance)) return null
        val length = if (first.linkKind == Kind.HOSE) (distance + 1).roundToInt().toDouble().coerceAtLeast(.5) else distance
        return Link(UUID.randomUUID(), a, b, first.linkKind, length).also {
            for (be in listOf(first, second).filterIsInstance<GasLinkBlockEntity>()) it.legacyIds.addAll(be.legacyJointIds)
            it.legacyA = a; it.legacyB = b
            links[it.id] = it; updateBlocks(level, it); ensureEdge(level, it); setDirty()
        }
    }

    fun reconcile(level: ServerLevel, be: BlockAnchorEndpoint) {
        at(be.pos)?.let { updateBlocks(level, it); return }
        if (VdexConstruction.locked(level, be.pos)) return
        if (be.linkId != null) {
            if (!links.containsKey(be.linkId)) be.setConnection(null, null, false)
            return
        }
        val pos = be.connectedPos ?: return
        if (!level.hasChunkAt(pos)) return
        val other = level.getBlockEntity(pos) as? BlockAnchorEndpoint
        if (other == null || other.linkKind != be.linkKind || other.connectedPos != be.pos) { be.setConnection(null, null, false); return }
        if (other.linkId != null || at(pos) != null) { be.setConnection(null, null, false); return }
        val link = if (be.main) add(level, be, other) else add(level, other, be)
        if (link == null) {
            be.setConnection(null, null, false)
            other.setConnection(null, null, false)
        }
    }

    private fun updateBlocks(level: ServerLevel, link: Link) {
        for ((anchor, other) in listOf(link.a to link.b, link.b to link.a)) {
            if (!level.hasChunkAt(anchor.pos)) continue
            val endpoint = level.getBlockEntity(anchor.pos) as? BlockAnchorEndpoint ?: continue
            endpoint.setConnection(link.id, other.pos, anchor == link.a)
            if (endpoint is GasLinkBlockEntity) {
                endpoint.updateLength(link.length)
                if (!link.pending && link.jointId != null && endpoint.distanceJointId != link.jointId) {
                    val primary = level.gtpa.getJointById(link.jointId!!) as? VSDistanceJoint ?: continue
                    endpoint.distanceJoint = primary; endpoint.distanceJointId = link.jointId
                    endpoint.sphericalJoint = link.alignmentId?.let { level.gtpa.getJointById(it) as? VSD6Joint }
                    endpoint.sphericalJointId = link.alignmentId
                }
            }
        }
    }

    private fun reset(level: ServerLevel, link: Link) {
        for (anchor in listOf(link.a, link.b)) if (level.hasChunkAt(anchor.pos)) {
            (level.getBlockEntity(anchor.pos) as? BlockAnchorEndpoint)?.updateStress(0f)
            (level.getBlockEntity(anchor.pos) as? GasLinkBlockEntity)?.clearPhysics()
        }
        link.generation++
        link.jointId?.let { if (level.gtpa.getJointById(it) != null) level.gtpa.removeJoint(it) }
        link.alignmentId?.let { if (level.gtpa.getJointById(it) != null) level.gtpa.removeJoint(it) }
        link.edge?.let { ClockworkMod.getKelvin(level).removeEdge(it.nodeA, it.nodeB) }
        link.edge = null; link.alignmentId = null
        link.jointId = null; link.pending = false; link.age = 0; link.missing = 0; link.overload = 0
        link.previousDistance = null
    }

    fun remove(level: ServerLevel, link: Link, snapped: Boolean = false) {
        if (links[link.id] !== link) return
        links.remove(link.id)
        reset(level, link)
        for (anchor in listOf(link.a, link.b)) if (level.hasChunkAt(anchor.pos)) {
            val be = level.getBlockEntity(anchor.pos) as? BlockAnchorEndpoint
            if (be?.linkId == link.id) be.setConnection(null, null, false)
        }
        if (snapped) link.a.world(level)?.let { (if (link.kind == Kind.SHAFT) ClockworkSounds.JOINT_BREAK else ClockworkSounds.HOSE_RELEASE).play(level, null, it, 1f, 1f) }
        setDirty()
    }

    internal fun remap(level: ServerLevel, destination: (WandAnchor) -> WandLinkTransfer.Destination?) {
        for (link in links.values) {
            val a = destination(link.a)
            val b = destination(link.b)
            if (a == null && b == null) continue
            retireLegacy(level, link)
            reset(level, link)
            if (a != null) { link.a = a.anchor; link.rotationA = link.rotationA?.let { Quaterniond(a.rotation).mul(it).normalize() } }
            if (b != null) { link.b = b.anchor; link.rotationB = link.rotationB?.let { Quaterniond(b.rotation).mul(it).normalize() } }
            setDirty()
        }
    }

    fun restore(level: ServerLevel, link: Link): Boolean {
        if (links.size >= 2048 || links.containsKey(link.id) || link.a.pos == link.b.pos || at(link.a.pos) != null || at(link.b.pos) != null ||
            listOf(link.a, link.b).any { !level.hasChunkAt(it.pos) || (level.getBlockEntity(it.pos) as? BlockAnchorEndpoint)?.linkKind != link.kind }) return false
        links[link.id] = link
        updateBlocks(level, link)
        setDirty()
        return true
    }

    private fun ensureEdge(level: ServerLevel, link: Link) {
        if (link.kind == Kind.SHAFT) return
        val network = ClockworkMod.getKelvin(level)
        val a = link.a.pos.toDuctNodePos(level.dimension().location())
        val b = link.b.pos.toDuctNodePos(level.dimension().location())
        if (network.getNodeAt(a) == null || network.getNodeAt(b) == null) return
        val existing = network.getEdgeBetween(a, b)
        if (existing != null) link.edge = existing else {
            link.edge = configureMetalDuctEdge(PipeDuctEdge(nodeA = a, nodeB = b, type = ConnectionType.PIPE, radius = .3125, length = .375))
            network.addEdge(a, b, link.edge!!)
        }
    }

    private fun retireLegacy(level: ServerLevel, link: Link) {
        val iterator = link.legacyIds.iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()
            val old = level.gtpa.getJointById(id)
            if (old == null) continue
            val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]
            fun matches(anchor: WandAnchor, body: Long?, pose: VSJointPose): Boolean {
                val expected = anchor.shipId.takeIf { it >= 0 }
                if ((body.takeUnless { it == ground || it == -1L }) != expected) return false
                val center = Vector3d(anchor.pos.x + .5, anchor.pos.y + .5, anchor.pos.z + .5)
                return pose.pos.distanceSquared(center) < 1e-6 || (link.kind == Kind.HOSE &&
                    pose.pos.distanceSquared(center.sub(.5, .5, .5)) < 1e-6)
            }
            if (id != link.jointId && id != link.alignmentId && (old is VSDistanceJoint || old is VSD6Joint) &&
                ((matches(link.legacyA ?: link.a, old.shipId0, old.pose0) && matches(link.legacyB ?: link.b, old.shipId1, old.pose1)) ||
                    (matches(link.legacyB ?: link.b, old.shipId0, old.pose0) && matches(link.legacyA ?: link.a, old.shipId1, old.pose1)))) level.gtpa.removeJoint(id)
            iterator.remove()
            setDirty()
        }
    }

    private fun publish(level: ServerLevel, link: Link, joints: List<VSJoint>) {
        for (anchor in listOf(link.a, link.b)) (level.getBlockEntity(anchor.pos) as? GasLinkBlockEntity)?.also {
            it.distanceJoint = joints[0] as VSDistanceJoint; it.distanceJointId = link.jointId
            it.sphericalJoint = joints.getOrNull(1) as? VSD6Joint; it.sphericalJointId = link.alignmentId
        }
    }

    private fun install(level: ServerLevel, link: Link, joints: List<VSJoint>) {
        val generation = link.generation
        link.pending = true
        var remaining = joints.size
        var failed = false
        joints.forEachIndexed { index, joint ->
            joint.shouldBeSerialized = false
            level.gtpa.addJoint(joint) { id -> level.server.execute {
                if (links[link.id] !== link || link.generation != generation) {
                    if (id >= 0) level.gtpa.removeJoint(id)
                } else {
                    if (id < 0) failed = true else if (index == 0) link.jointId = id else link.alignmentId = id
                    if (--remaining == 0) {
                        link.pending = false
                        if (failed) reset(level, link) else publish(level, link, joints)
                    }
                }
            } }
        }
    }

    fun tick(level: ServerLevel) {
        val graph by lazy {
            val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]
            ConnectedShipMass(level.gtpa.getAllJoints().values, groundBody = ground) { id ->
                level.shipObjectWorld.loadedShips.getById(id)?.let { ConnectedShipMass.Load(it.inertiaData.mass, it.isStatic) }
                    ?: level.shipObjectWorld.allBodies.getById(id)?.let { ConnectedShipMass.Load(it.inertiaData.mass, it.isStatic) }
            }
        }
        for (link in links.values.toList()) {
            val ends = listOf(link.a, link.b)
            if (ends.any { (it.shipId >= 0 && level.shipObjectWorld.allShips.getById(it.shipId) == null) ||
                    (level.hasChunkAt(it.pos) && (level.getBlockEntity(it.pos) as? BlockAnchorEndpoint)?.linkKind != link.kind) }) {
                remove(level, link)
                continue
            }
            if (ends.any { !level.hasChunkAt(it.pos) || it.world(level) == null || VdexConstruction.locked(level, it.pos) }) {
                if (link.jointId != null || link.pending || link.edge != null) reset(level, link)
                continue
            }
            updateBlocks(level, link)
            retireLegacy(level, link)
            if (link.a.shipId == link.b.shipId) {
                if (link.jointId != null || link.pending) reset(level, link)
                ensureEdge(level, link)
                continue
            }
            ensureEdge(level, link)
            if (link.pending) continue
            if (link.jointId != null && (level.gtpa.getJointById(link.jointId!!) == null ||
                    (link.kind == Kind.EXTENDON && link.alignmentId?.let { level.gtpa.getJointById(it) } == null))) {
                if (++link.missing > 10) remove(level, link, true)
                continue
            }
            link.missing = 0
            if (link.kind == Kind.EXTENDON) {
                val first = level.getBlockEntity(link.a.pos) as? ExtendonBlockEntity ?: continue
                val second = level.getBlockEntity(link.b.pos) as? ExtendonBlockEntity ?: continue
                if (link.jointId == null) {
                    val (distance, angular) = first.buildJointBlueprints(second) ?: continue
                    if (link.rotationA == null) link.rotationA = Quaterniond(distance.pose0.rot)
                    if (link.rotationB == null) link.rotationB = Quaterniond(distance.pose1.rot)
                    val poseA = distance.pose0.copy(rot = link.rotationA!!)
                    val poseB = distance.pose1.copy(rot = link.rotationB!!)
                    if (link.length <= 0) link.length = distance.minDistance!!.toDouble()
                    install(level, link, listOf(distance.copy(pose0 = poseA, pose1 = poseB,
                        minDistance = link.length.toFloat(), maxDistance = link.length.toFloat()), angular.copy(pose0 = poseA, pose1 = poseB)))
                    setDirty()
                } else {
                    val length = first.distanceJoint?.minDistance?.toDouble()
                    if (length != null && length != link.length) { link.length = length; setDirty() }
                }
                link.age++
                continue
            }
            val a = link.a.world(level) ?: continue
            val b = link.b.world(level) ?: continue
            val distance = a.distanceTo(b)
            val speed = (distance - (link.previousDistance ?: distance)) * 20
            link.previousDistance = distance
            val limit = if (link.kind == Kind.SHAFT) ClockworkConfig.SERVER.maxUniversalJointDistance else link.length
            val stress = stress(distance - limit, speed)
            if (link.age >= RAMP_TICKS) {
                link.overload = if (stress > 1) link.overload + 1 else 0
                if (link.overload >= 3) { remove(level, link, true); continue }
            }
            if (link.age % 5 == 0) for (anchor in ends) (level.getBlockEntity(anchor.pos) as? BlockAnchorEndpoint)
                ?.updateStress(if (link.age >= RAMP_TICKS) stress.toFloat() else 0f)
            var dirty = link.jointId == null || link.age <= RAMP_TICKS || link.limit != limit
            if (link.age % 10 == 0 || link.jointId == null) {
                val (first, second) = graph.endpoints(link.a.shipId.takeIf { it >= 0 }, link.b.shipId.takeIf { it >= 0 })
                val mass = SpringJoints.effectiveMass(first.mass, second.mass, first.anchored, second.anchored)
                if (abs(mass - link.mass) > link.mass * .01) { link.mass = mass; dirty = true }
            }
            if (dirty) {
                link.limit = limit
                val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: continue
                fun pose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond()) }
                val joint = joint(link.a.shipId.takeIf { it >= 0 } ?: ground, link.b.shipId.takeIf { it >= 0 } ?: ground,
                    pose(link.a), pose(link.b), limit, link.mass, link.age, level.server.vsPipeline.getPhysicsBackendType())
                joint.shouldBeSerialized = false
                val id = link.jointId
                if (id != null) {
                    if (level.gtpa.getJointById(id)?.matchesByAnchors(joint) == true) {
                        level.gtpa.updateJoint(id, joint)
                        publish(level, link, listOf(joint))
                    } else { remove(level, link, true); continue }
                } else install(level, link, listOf(joint))
            }
            if (link.jointId != null) link.age++
        }
    }
    override fun save(tag: CompoundTag): CompoundTag = tag.also {
        it.put("links", ListTag().also { list -> links.values.forEach { link -> list.add(link.save()) } })
    }

    companion object {
        private const val RAMP_TICKS = 20
        private const val STIFFNESS = 600.0
        private const val DAMPING = 36.0
        private const val BREAK_ACCELERATION = 900.0

        fun get(level: ServerLevel): BlockAnchorLinks = level.dataStorage.computeIfAbsent(::load, ::BlockAnchorLinks, "clockwork_universal_shafts")
        internal fun load(tag: CompoundTag) = BlockAnchorLinks().also { data ->
            for (entry in tag.getList("links", 10).filterIsInstance<CompoundTag>().take(2048)) {
                if (!entry.hasUUID("id")) continue
                val kind = if (!entry.contains("kind")) Kind.SHAFT else Kind.entries.firstOrNull { it.name == entry.getString("kind") } ?: continue
                val length = entry.getDouble("length")
                if (!length.isFinite() || length < 0) continue
                fun rotation(key: String): Quaterniond? {
                    if (!entry.contains(key)) return null
                    val q = entry.getCompound(key).let { Quaterniond(it.getDouble("x"), it.getDouble("y"), it.getDouble("z"), it.getDouble("w")) }
                    return q.takeIf { it.isFinite && it.lengthSquared() > 1e-12 }?.normalize()
                }
                val link = Link(entry.getUUID("id"), WandAnchor.load(entry.getCompound("a")), WandAnchor.load(entry.getCompound("b")),
                    kind, length, rotation("rotationA"), rotation("rotationB"))
                link.legacyIds.addAll(entry.getIntArray("legacyJoints").take(4))
                if (entry.contains("legacyA")) link.legacyA = WandAnchor.load(entry.getCompound("legacyA"))
                if (entry.contains("legacyB")) link.legacyB = WandAnchor.load(entry.getCompound("legacyB"))
                if (link.a.pos != link.b.pos) data.links[link.id] = link
            }
        }

        internal fun stress(extension: Double, speed: Double) = if (extension <= 0) 0.0 else
            (extension * STIFFNESS + speed.coerceAtLeast(0.0) * DAMPING) / BREAK_ACCELERATION

        internal fun joint(a: Long, b: Long, poseA: VSJointPose, poseB: VSJointPose, length: Double, mass: Double,
                           age: Int, backend: ConfigPhysicsBackendType): VSDistanceJoint {
            val ramp = (.1 + .9 * age.toDouble() / RAMP_TICKS).coerceIn(.1, 1.0)
            val stiffness = mass * STIFFNESS * ramp
            val classic = backend == ConfigPhysicsBackendType.KRUNCH_CLASSIC
            return VSDistanceJoint(a, poseA, b, poseB, VSJointMaxForceTorque(Float.MAX_VALUE, Float.MAX_VALUE),
                compliance = if (classic) 1.0 / stiffness else VSJoint.DEFAULT_COMPLIANCE, maxDistance = length.toFloat(),
                stiffness = if (classic) null else stiffness.toFloat(), damping = if (classic) null else (mass * DAMPING * ramp).toFloat()
            ).also { it.shouldBeSerialized = false }
        }
    }
}
