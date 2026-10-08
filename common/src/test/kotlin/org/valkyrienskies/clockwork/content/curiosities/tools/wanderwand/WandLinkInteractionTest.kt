package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class WandLinkInteractionTest {
    private val world = WandAnchor(BlockPos(4, 6, 3), Direction.UP)
    private val ship = WandAnchor(BlockPos(-20000000, 7, 20000000), Direction.DOWN, 12)

    private fun rope(a: WandAnchor, b: WandAnchor): WandLinks.Link {
        fun pose(anchor: WandAnchor) = anchor.local().let { VSJointPose(Vector3d(it.x, it.y, it.z), Quaterniond()) }
        return WandLinks.Link(UUID.randomUUID(), a, b, true, 12.0, pose(a), pose(b))
    }

    @Test fun `attachments accept ship world in either order but reject a single body`() {
        assertTrue(WandLinkPhysics.canAttach(ship, world))
        assertTrue(WandLinkPhysics.canAttach(world, ship))
        assertTrue(WandLinkPhysics.canAttach(ship, ship.copy(shipId = 13)))
        assertFalse(WandLinkPhysics.canAttach(world, world.copy(pos = BlockPos(8, 6, 3))))
        assertFalse(WandLinkPhysics.canAttach(ship, ship.copy(pos = ship.pos.above())))
    }

    @Test fun `ground attachment frames align the ship face with the world face in either order`() {
        val shipRotation = Quaterniond().rotateXYZ(0.6, -0.3, 1.2)
        for (worldFirst in listOf(false, true)) for (worldFace in Direction.values()) for (shipFace in Direction.values()) {
            val groundAnchor = world.copy(face = worldFace)
            val shipAnchor = ship.copy(face = shipFace)
            val (a, b) = if (worldFirst) groundAnchor to shipAnchor else shipAnchor to groundAnchor
            val (ra, rb) = if (worldFirst) Quaterniond() to shipRotation else shipRotation to Quaterniond()
            val (pa, pb) = WandLinkPhysics.gluePoses(a, b, ra, rb)
            // Hold the ground body fixed and solve the ship's orientation from the joint frames.
            val alignedShip = if (worldFirst) Quaterniond(pa.rot).mul(Quaterniond(pb.rot).invert())
                else Quaterniond(pb.rot).mul(Quaterniond(pa.rot).invert())
            val normal = alignedShip.transform(Vector3d(shipFace.stepX.toDouble(), shipFace.stepY.toDouble(), shipFace.stepZ.toDouble()))
            val oppositeGround = Vector3d(-worldFace.stepX.toDouble(), -worldFace.stepY.toDouble(), -worldFace.stepZ.toDouble())
            assertTrue(normal.distance(oppositeGround) < 1e-8)
            val groundPose = if (worldFirst) pa else pb
            assertEquals(world.pos.x + 0.5 + worldFace.stepX * 0.5, groundPose.pos.x(), 1e-8)
            assertEquals(world.pos.y + 0.5 + worldFace.stepY * 0.5, groundPose.pos.y(), 1e-8)
            assertEquals(world.pos.z + 0.5 + worldFace.stepZ * 0.5, groundPose.pos.z(), 1e-8)
        }
    }

    @Test fun `world world bind can be removed from either anchor without a physics joint`() {
        val link = rope(world.copy(pos = BlockPos(-4, 6, 3)), world)
        assertNull(link.jointId)
        for (anchor in listOf(link.a, link.b)) {
            val hit = WandLinkTargeting.removalDistance(link, Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 5.0,
                anchor.copy(face = anchor.face.opposite), null, null)
            assertEquals(5.0, hit)
        }
        assertFalse(WandLinkTargeting.removalDistance(link, Vec3.ZERO, Vec3(0.0, 0.0, 1.0), 5.0,
            link.a.copy(shipId = 42), null, null) != null)
    }

    @Test fun `secured binds are removable by their sagging strand regardless of body combination`() {
        for ((a, b) in listOf(world to world.copy(pos = BlockPos(-4, 6, 3)), world to ship, ship to world)) {
            val link = rope(a, b)
            val start = Vec3(-4.0, 6.0, 3.0); val end = Vec3(4.0, 6.0, 3.0)
            val curve = WandRopeCurve(start, end, link.length)
            val eye = curve.point(0.5).add(0.0, 0.0, -3.0)
            val direction = Vec3(0.0, 0.0, 1.0)
            assertEquals(3.0, WandLinkTargeting.removalDistance(link, eye, direction, 15.0, null, start, end)!!, 1e-8)
            // A nearer block stops the ray before the rope, even if the endpoints remain in reach.
            assertNull(WandLinkTargeting.removalDistance(link, eye, direction, 2.0, null, start, end))
            assertNull(WandLinkTargeting.removalDistance(link, eye, direction.reverse(), 15.0, null, start, end))
        }
    }

    @Test fun `overlapping strands are ranked by the ray hit instead of endpoint proximity`() {
        val link = rope(world, ship)
        val eye = Vec3.ZERO; val direction = Vec3(0.0, 0.0, 1.0)
        val near = WandLinkTargeting.removalDistance(link, eye, direction, 15.0, null,
            Vec3(-6.0, 0.0, 3.0), Vec3(6.0, 0.0, 3.0))!!
        val far = WandLinkTargeting.removalDistance(link.copy(length = 2.0), eye, direction, 15.0, null,
            Vec3(-1.0, 0.0, 5.0), Vec3(1.0, 0.0, 5.0))!!
        assertTrue(near < far)
    }
}
