package org.valkyrienskies.clockwork.content.curiosities.rope

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkPhysics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class RopeKnotTest {
    @Test fun `physical rope survives extension and velocity which snap a magical bind`() {
        assertTrue(WandLinkPhysics.overloaded(true, 2.0, 4.0, 100))
        assertFalse(RopeKnotPhysics.overloaded(2.0, 4.0))
        assertTrue(RopeKnotPhysics.BREAK_ACCELERATION > WandLinkPhysics.ROPE_BREAK_ACCELERATION * 3)
        assertTrue(RopeKnotPhysics.overloaded(8.1, 0.0))
        assertTrue(RopeKnotPhysics.overloaded(2.0, 80.0))
        assertFalse(RopeKnotPhysics.overloaded(-1.0, 1000.0))
    }

    @Test fun `untying requires three hits while cutting requires one`() {
        assertFalse(RopeKnotPhysics.breaksOnHit(1, false))
        assertFalse(RopeKnotPhysics.breaksOnHit(2, false))
        assertTrue(RopeKnotPhysics.breaksOnHit(3, false))
        assertTrue(RopeKnotPhysics.breaksOnHit(1, true))
    }

    @Test fun `placement leaves a little slack without exceeding supported rope length`() {
        assertEquals(10.25, RopeKnotPhysics.placedLength(10.0))
        assertEquals(0.5, RopeKnotPhysics.placedLength(0.01))
        assertEquals(128.0, RopeKnotPhysics.placedLength(128.0))
    }

    @Test fun `physical and legacy magical ropes persist together without saving transient hitboxes`() {
        val a = WandAnchor(BlockPos(2, 4, 8), Direction.UP)
        val b = WandAnchor(BlockPos(6, 12, 24), Direction.WEST, 42)
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val physical = WandLinks.Link(UUID.randomUUID(), a, b, true, 22.25, pose, pose, true)
        physical.knotA = UUID.randomUUID()
        physical.jointId = 57
        val legacy = physical.copy(id = UUID.randomUUID(), physical = false).save().also { it.remove("physical") }
        val root = CompoundTag().also { it.put("links", ListTag().also { list -> list.add(physical.save()); list.add(legacy) }) }
        val restored = WandLinks.load(root).links
        val rope = restored[physical.id]!!
        assertTrue(rope.physical)
        assertEquals(a, rope.a)
        assertEquals(b, rope.b)
        assertEquals(22.25, rope.length)
        assertNull(rope.knotA)
        assertNull(rope.jointId)
        assertFalse(restored[legacy.getUUID("id")]!!.physical)
    }
}
