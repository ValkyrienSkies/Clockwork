package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import org.valkyrienskies.clockwork.util.universal_joint.BlockAnchorLinks

import io.mockk.every
import io.mockk.mockk
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.Bootstrap
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer
import org.valkyrienskies.core.impl.api_impl.config.ConfigPhysicsBackendType
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class BlockAnchorLinksTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test fun telescopeHasOnlyASoftMaximumAndNoRestLength() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        for (backend in listOf(ConfigPhysicsBackendType.KRUNCH_KONSTANT, ConfigPhysicsBackendType.KRUNCH_CLASSIC)) {
            val joint = BlockAnchorLinks.joint(1, 2, pose, pose, 7.0, 24000.0, 20, backend)
            assertNull(joint.minDistance)
            assertEquals(7f, joint.maxDistance)
            assertFalse(joint.shouldBeSerialized)
            if (backend == ConfigPhysicsBackendType.KRUNCH_KONSTANT) {
                assertTrue(joint.stiffness!! > 0)
                assertTrue(joint.damping!! > 0)
                val initial = BlockAnchorLinks.joint(1, 2, pose, pose, 7.0, 24000.0, 0, backend)
                assertTrue(initial.stiffness!! < joint.stiffness!!)
            } else assertTrue(joint.compliance > 0)
        }
        assertEquals(0.0, BlockAnchorLinks.stress(-.1, 50.0))
        assertEquals(0.0, BlockAnchorLinks.stress(0.0, 0.0))
        assertTrue(BlockAnchorLinks.stress(.2, 0.0) in 0.0..1.0)
        assertTrue(BlockAnchorLinks.stress(2.0, 0.0) > 1.0)
        assertTrue(BlockAnchorLinks.stress(.2, 30.0) > 1.0)
    }

    @Test fun shaftPersistenceAndPasteNeverReuseNativeOrConnectionIds() {
        val a = WandAnchor(BlockPos(10000, 82, -20000), Direction.DOWN, 41, offset = Vec3.ZERO)
        val b = WandAnchor(BlockPos(30000, 78, -40000), Direction.UP, 42, offset = Vec3.ZERO)
        val link = BlockAnchorLinks.Link(UUID.randomUUID(), a, b).apply { jointId = 35; pending = true; age = 100 }
        val saved = BlockAnchorLinks().apply { links[link.id] = link }.save(CompoundTag())
        val loaded = BlockAnchorLinks.load(saved).links.getValue(link.id)
        assertEquals(a, loaded.a); assertEquals(b, loaded.b)
        assertNull(loaded.jointId); assertFalse(loaded.pending); assertEquals(0, loaded.age)
        val payload = WandLinkSchematics.encode(emptyList(), mapOf(41L to a.pos.below(2), 42L to b.pos.below(3)))
        payload.put("universalShafts", ListTag().apply { add(link.save()) })
        val targets = mapOf(41L to (71L to BlockPos(90000, 100, 10000)), 42L to (72L to BlockPos(120000, 100, 30000)))
        val pasted = WandLinkSchematics.decodeShafts(payload, targets).single()
        assertNotEquals(link.id, pasted.id)
        assertNotEquals(pasted.id, WandLinkSchematics.decodeShafts(payload, targets).single().id)
        assertEquals(71L, pasted.a.shipId); assertEquals(72L, pasted.b.shipId)
        assertEquals(BlockPos(90000, 102, 10000), pasted.a.pos)
        assertEquals(BlockPos(120000, 103, 30000), pasted.b.pos)
        assertEquals(Vec3.atCenterOf(pasted.a.pos), pasted.a.local())
        assertNull(pasted.jointId)
        assertTrue(WandLinkSchematics.decodeShafts(payload, targets.filterKeys { it == 41L }).isEmpty())
    }

    @Test fun movingEitherEndThroughAssemblyAndWeldKeepsTheBlockCenterAndFace() {
        val a = WandAnchor(BlockPos(10, 80, 10), Direction.EAST, offset = Vec3.ZERO)
        val b = a.copy(pos = a.pos.east(4))
        val turn = Quaterniond().rotationY(Math.PI / 2)
        val level = mockk<ServerLevel>()
        every { level.hasChunkAt(any()) } returns false
        val link = BlockAnchorLinks.Link(UUID.randomUUID(), a, b).apply { pending = true; age = 90 }
        val data = BlockAnchorLinks().apply { links[link.id] = link }
        data.remap(level) { anchor -> if (anchor == a)
            WandLinkTransfer.rotatedDestination(anchor, BlockPos(10000, 80, 10000), 1, turn) else null }
        val first = link.a
        assertEquals(Direction.NORTH, first.face)
        assertEquals(Vec3.atCenterOf(first.pos), first.local())
        assertEquals(b, link.b)
        assertFalse(link.pending); assertEquals(0, link.age); assertEquals(1, link.generation)
        data.remap(level) { anchor -> if (anchor == b)
            WandLinkTransfer.rotatedDestination(anchor, first.pos.north(4), 1, turn) else null }
        val second = link.b
        assertEquals(first.shipId, second.shipId)
        assertEquals(4.0, first.local().distanceTo(second.local()))
        data.remap(level) { anchor -> WandLinkTransfer.rotatedDestination(anchor,
            BlockPos(20, 80, 20).offset(anchor.pos.subtract(first.pos)), -1, Quaterniond()) }
        val world = link.a
        assertEquals(-1L, world.shipId)
        assertEquals(Vec3.atCenterOf(world.pos), world.local())
        assertEquals(-1L, link.b.shipId)
        assertEquals(4.0, world.local().distanceTo(link.b.local()))
        assertSame(link, data.links[link.id])
    }

    @Test fun gasConnectionsKeepTheirKindLengthAndLocalFramesAcrossSavePasteAndWeld() {
        val a = WandAnchor(BlockPos(10, 80, 10), Direction.EAST, 41, offset = Vec3.ZERO)
        val b = a.copy(pos = a.pos.east(6), shipId = 42)
        val rotation = Quaterniond().rotationX(.3)
        for (kind in listOf(BlockAnchorLinks.Kind.EXTENDON, BlockAnchorLinks.Kind.HOSE)) {
            val link = BlockAnchorLinks.Link(UUID.randomUUID(), a, b, kind, 7.0, rotation, Quaterniond()).apply {
                jointId = 6; alignmentId = 7
            }
            val payload = WandLinkSchematics.encode(emptyList(), mapOf(41L to a.pos, 42L to b.pos))
            payload.put("universalShafts", ListTag().apply { add(link.save()) })
            val loaded = WandLinkSchematics.decodeShafts(payload, mapOf(41L to (71L to a.pos), 42L to (72L to b.pos))).single()
            assertEquals(kind, loaded.kind); assertEquals(7.0, loaded.length)
            assertEquals(1.0, kotlin.math.abs(rotation.dot(loaded.rotationA)), 1e-9)
            assertNull(loaded.jointId); assertNull(loaded.alignmentId)
            val level = mockk<ServerLevel>()
            every { level.hasChunkAt(any()) } returns false
            val data = BlockAnchorLinks().apply { links[loaded.id] = loaded }
            val turn = Quaterniond().rotationY(Math.PI / 2)
            data.remap(level) { anchor -> if (anchor == loaded.a)
                WandLinkTransfer.rotatedDestination(anchor, BlockPos(20, 80, 20), -1, turn) else null }
            assertEquals(7.0, loaded.length)
            assertEquals(1.0, kotlin.math.abs(Quaterniond(turn).mul(rotation).dot(loaded.rotationA)), 1e-9)
            assertEquals(1.0, loaded.rotationB!!.w, 1e-9)
        }
    }
}
