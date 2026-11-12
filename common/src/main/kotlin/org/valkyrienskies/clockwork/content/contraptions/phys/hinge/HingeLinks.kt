package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.acos

class HingeLinks : SavedData() {
    data class Link(val id: UUID, var a: WandAnchor, var b: WandAnchor, var lower: Int = -90, var upper: Int = 90,
                    var connected: Boolean = true) {
        var jointId: Int? = null
        var pending = false
        var generation = 0
        var dirty = true
        var missing = 0
        var age = 0
        var overloadedTicks = 0
        var appliedLower = lower
        var appliedUpper = upper
        fun save() = CompoundTag().also {
            it.putUUID("id", id); it.put("a", a.save()); it.put("b", b.save())
            it.putInt("lower", lower); it.putInt("upper", upper); it.putBoolean("connected", connected)
        }
    }

    val links = LinkedHashMap<UUID, Link>()
    fun at(pos: BlockPos) = links.values.firstOrNull { it.a.pos == pos || it.b.pos == pos }

    fun add(level: ServerLevel, a: WandAnchor, b: WandAnchor, lower: Int, upper: Int): Link? {
        if (links.size >= 2048 || a.pos == b.pos || at(a.pos)?.connected == true || at(b.pos)?.connected == true) return null
        listOfNotNull(at(a.pos), at(b.pos)).distinct().forEach { remove(level, it) }
        val link = Link(UUID.randomUUID(), a, b, lower.coerceIn(-180, 0), upper.coerceIn(0, 180))
        links[link.id] = link
        updateBlocks(level, link)
        setDirty()
        return link
    }

    fun reset(level: ServerLevel, link: Link) {
        if (level.hasChunkAt(link.a.pos)) (level.getBlockEntity(link.a.pos) as? HingeBlockEntity)?.updateStrain(false)
        link.generation++
        link.jointId?.let { if (level.gtpa.getJointById(it) != null) level.gtpa.removeJoint(it) }
        link.jointId = null; link.pending = false; link.dirty = true; link.missing = 0; link.age = 0; link.overloadedTicks = 0
    }

    fun remove(level: ServerLevel, link: Link) {
        reset(level, link)
        link.connected = false
        updateBlocks(level, link)
        links.remove(link.id)
        setDirty()
    }

    fun breakLink(level: ServerLevel, link: Link) {
        reset(level, link)
        link.connected = false
        updateBlocks(level, link)
        setDirty()
        worldPoint(level, link.a)?.let {
            ClockworkSounds.HINGE_DISASSEMBLE.play(level, null, it, 0.8f, 0.8f)
            level.sendParticles(ParticleTypes.CRIT, it.x, it.y, it.z, 14, 0.18, 0.18, 0.18, 0.12)
        }
    }

    private fun updateBlocks(level: ServerLevel, link: Link) {
        for (anchor in listOf(link.a, link.b)) {
            if (!level.hasChunkAt(anchor.pos)) continue
            val state = level.getBlockState(anchor.pos)
            val block = state.block as? HingeBlock ?: continue
            val assembled = if (block.attachment) link.connected else true
            if (state.getValue(HingeBlock.ASSEMBLED) != assembled)
                level.setBlock(anchor.pos, state.setValue(HingeBlock.ASSEMBLED, assembled), 3)
        }
    }

    fun tick(level: ServerLevel) {
        for (link in links.values.toList()) {
            val ends = listOf(link.a, link.b)
            if (ends.any { (it.shipId >= 0 && level.shipObjectWorld.allShips.getById(it.shipId) == null) ||
                    (level.hasChunkAt(it.pos) && level.getBlockState(it.pos).block !is HingeBlock) }) {
                remove(level, link)
                continue
            }
            if (ends.any { !level.hasChunkAt(it.pos) || (it.shipId >= 0 && level.shipObjectWorld.loadedShips.getById(it.shipId) == null) }) {
                if (link.jointId != null || link.pending) reset(level, link)
                continue
            }
            updateBlocks(level, link)
            if (!link.connected || link.pending) continue
            if (link.a.shipId == link.b.shipId) {
                if (link.jointId != null) reset(level, link)
                continue
            }
            if (link.jointId != null && level.gtpa.getJointById(link.jointId!!) == null) {
                if (++link.missing > 5) breakLink(level, link)
                continue
            }
            link.missing = 0
            val nextLower = link.appliedLower + (link.lower - link.appliedLower).coerceIn(-2, 2)
            val nextUpper = link.appliedUpper + (link.upper - link.appliedUpper).coerceIn(-2, 2)
            if (nextLower != link.appliedLower || nextUpper != link.appliedUpper) {
                link.appliedLower = nextLower; link.appliedUpper = nextUpper
                link.dirty = true
            }
            val stateA = level.getBlockState(link.a.pos)
            val stateB = level.getBlockState(link.b.pos)
            val poseA = VSJointPose(center(link.a.pos), HingeGeometry.orientation(stateA))
            val poseB = VSJointPose(center(link.b.pos), HingeGeometry.orientation(stateB))
            if (link.jointId == null || link.dirty || link.age <= HingeGeometry.SETTLE_TICKS) {
                val joint = HingeGeometry.joint(link.a.shipId.takeIf { it >= 0 }, link.b.shipId.takeIf { it >= 0 },
                    poseA, poseB, link.appliedLower, link.appliedUpper, link.age)
                link.dirty = false
                if (link.jointId != null) level.gtpa.updateJoint(link.jointId!!, joint) else {
                    val generation = link.generation
                    link.pending = true
                    level.gtpa.addJoint(joint) { id -> level.server.execute {
                        if (links[link.id] !== link || generation != link.generation || !link.connected) {
                            if (id >= 0) level.gtpa.removeJoint(id)
                        } else {
                            link.pending = false
                            if (id >= 0) link.jointId = id else link.dirty = true
                        }
                    } }
                }
            }
            link.age++
            if (link.age > HingeGeometry.BREAK_GRACE_TICKS) {
                val a = worldPoint(level, link.a) ?: continue
                val b = worldPoint(level, link.b) ?: continue
                val qa = worldRotation(level, link.a)
                val qb = worldRotation(level, link.b)
                val angle = HingeGeometry.angle(qa, qb)
                val axisAlignment = qa.transform(Vector3d(1.0, 0.0, 0.0)).dot(qb.transform(Vector3d(1.0, 0.0, 0.0)))
                val angularSlack = Math.toDegrees(HingeGeometry.MAX_TORQUE * HingeGeometry.COMPLIANCE)
                if (a.distanceTo(b) > HingeGeometry.MAX_FORCE * HingeGeometry.COMPLIANCE ||
                    Math.toDegrees(acos(axisAlignment.coerceIn(-1.0, 1.0))) > angularSlack ||
                    angle < link.appliedLower - angularSlack || angle > link.appliedUpper + angularSlack) {
                    link.overloadedTicks++
                } else link.overloadedTicks = 0
                if (link.overloadedTicks >= 3) {
                    breakLink(level, link)
                    continue
                }
            }
            if (link.age % 5 == 0) {
                val angle = HingeGeometry.angle(worldRotation(level, link.a), worldRotation(level, link.b))
                val a = worldPoint(level, link.a) ?: continue
                val b = worldPoint(level, link.b) ?: continue
                val strain = a.distanceTo(b) > 0.03 || angle < link.appliedLower - 1 || angle > link.appliedUpper + 1
                (level.getBlockEntity(link.a.pos) as? HingeBlockEntity)?.updateStrain(strain)
            }
        }
    }

    internal fun remap(level: ServerLevel, destination: (WandAnchor) -> WandLinkTransfer.Destination?) {
        for (link in links.values) {
            val a = destination(link.a)?.anchor
            val b = destination(link.b)?.anchor
            if (a == null && b == null) continue
            reset(level, link)
            if (a != null) link.a = a
            if (b != null) link.b = b
            setDirty()
        }
    }

    fun restore(level: ServerLevel, link: Link): Boolean {
        if (links.size >= 2048 || at(link.a.pos) != null || at(link.b.pos) != null ||
            listOf(link.a, link.b).any { level.getBlockState(it.pos).block !is HingeBlock }) return false
        links[link.id] = link
        updateBlocks(level, link)
        setDirty()
        return true
    }

    override fun save(tag: CompoundTag): CompoundTag = tag.also {
        it.put("links", ListTag().also { list -> links.values.forEach { link -> list.add(link.save()) } })
    }

    companion object {
        fun get(level: ServerLevel): HingeLinks = level.dataStorage.computeIfAbsent(::load, ::HingeLinks, "clockwork_hinges")
        fun load(tag: CompoundTag) = HingeLinks().also { data ->
            for (entry in tag.getList("links", Tag.TAG_COMPOUND.toInt()).filterIsInstance<CompoundTag>().take(2048)) {
                if (!entry.hasUUID("id")) continue
                val link = Link(entry.getUUID("id"), WandAnchor.load(entry.getCompound("a")), WandAnchor.load(entry.getCompound("b")),
                    entry.getInt("lower").coerceIn(-180, 0), entry.getInt("upper").coerceIn(0, 180), entry.getBoolean("connected"))
                data.links[link.id] = link
            }
        }
        fun center(pos: BlockPos) = Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        fun worldPoint(level: ServerLevel, anchor: WandAnchor): Vec3? {
            val point = center(anchor.pos)
            if (anchor.shipId >= 0) {
                val ship = level.shipObjectWorld.loadedShips.getById(anchor.shipId) ?: return null
                ship.transform.shipToWorld.transformPosition(point)
            }
            return Vec3(point.x, point.y, point.z)
        }
        fun worldRotation(level: ServerLevel, anchor: WandAnchor): Quaterniond {
            val rotation = HingeGeometry.orientation(level.getBlockState(anchor.pos))
            val ship = level.shipObjectWorld.loadedShips.getById(anchor.shipId)
            return if (ship == null) rotation else Quaterniond(ship.transform.shipToWorldRotation).mul(rotation)
        }
    }
}
