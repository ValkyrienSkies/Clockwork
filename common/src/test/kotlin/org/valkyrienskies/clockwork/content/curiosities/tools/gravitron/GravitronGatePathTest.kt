package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.*
import org.valkyrienskies.mod.common.util.toJOML
import java.util.UUID

class GravitronGatePathTest {
    private val entrance = GatemakyrTarget(Vec3.ZERO, Direction.SOUTH)
    private val exit = GatemakyrTarget(Vec3(30.0, 12.0, 4.0), Direction.UP)
    private val passage = GatemakyrPassage(entrance.localFrame(), exit.localFrame())
    private val owner = UUID(0, 1)
    private fun packet(side: Int, target: GatemakyrTarget) = GatemakyrEffectPacket(ResourceLocation("test", "world"),
        owner, GatemakyrEffectPacket.Kind.RIFT, side, 0, target = target)
    private val gate = GatemakyrPlayerGate(packet(0, entrance), packet(1, exit), passage)
    private val gates = listOf(gate)
    private val eye = Vec3(0.0, 0.0, 4.0)

    @Test fun pushesThroughTheOpeningAndCanRetractBeforeTheAimRayReachesIt() {
        val beyond = Vec3(0.0, 0.0, -2.0)
        val remote = passage.point(beyond)
        assertSame(gate, GravitronGatePath.aimed(gates, eye, beyond, 42))
        val near = Vec3(0.0, 0.0, 1.0)
        assertNull(GravitronGatePath.aimed(gates, eye, near, 42))
        val path = requireNotNull(GravitronGatePath.toAnchor(gates, eye, remote, 42))
        assertEquals(6.0, path.apparentAnchor.distanceTo(eye), 1e-8)
        assertEquals(0.0, path.contact.length(), 1e-8)
        val atExit = GravitronForceInducerData.PortalTarget(passage.point(near).toJOML(), passage.rotation(),
            beyond.toJOML(), remote.toJOML())
        val hold = GravitronForceInducerData(near.toJOML(), Quaterniond(), near.toJOML(), portalTarget = atExit)
        assertSame(atExit, hold.targetThroughPortal(remote.toJOML()))
        assertNull(hold.targetThroughPortal(beyond.toJOML()))
        assertNull(GravitronGatePath.aimed(gates, eye, beyond.add(4.0, 0.0, 0.0), 42))
    }

    @Test fun adjacentCoplanarGatesKeepTheGrabThreadedUntilTheBlockReturns() {
        val other = entrance.copy(position = Vec3(3.0, 0.0, 0.0))
        val map = GatemakyrPassage(entrance.localFrame(), other.localFrame())
        val forward = GatemakyrPlayerGate(packet(0, entrance), packet(1, other), map)
        val reverse = GatemakyrPlayerGate(forward.partner, forward.packet, GatemakyrPassage(map.exit, map.entrance))
        val near = Vec3(0.0, 0.0, 0.1)
        val beyond = Vec3(0.0, 0.0, -3.5)
        val remote = map.point(beyond)
        val reference = GravitronGatePath.Reference(forward, near)
        assertTrue(reference.matches(forward))
        assertFalse(reference.matches(reverse))
        assertFalse(GravitronGatePath.atExit(forward, near, near))
        assertTrue(GravitronGatePath.atExit(forward, near, map.point(near.scale(-1.0))))
        assertTrue(eye.distanceTo(remote) < eye.distanceTo(beyond))
        assertTrue(GravitronGatePath.atExit(forward, beyond, remote))
        val path = GravitronGatePath.through(forward, eye, remote)
        assertEquals(0.0, beyond.distanceTo(path.apparentAnchor), 1e-8)
        assertTrue(path.contact.distanceTo(Vec3.ZERO) < 1e-8)
        assertFalse(path.bent)
        val sideways = GravitronGatePath.through(forward, eye.add(12.0, 0.0, 0.0), remote)
        assertTrue(sideways.bent)
        assertEquals(entrance.width * 0.5, sideways.contact.x, 1e-8)
        assertFalse(GravitronGatePath.atExit(forward, beyond, near))
    }

    @Test fun twoGateGrabAndOrbUseOnlyPhysicalLegsInOrder() {
        val second = GatemakyrTarget(passage.exit.position.add(passage.exit.normal.scale(3.0)), Direction.DOWN)
        val destination = GatemakyrTarget(Vec3(300.0, 5.0, 50.0), Direction.EAST)
        val next = GatemakyrPlayerGate(packet(0, second), packet(1, destination),
            GatemakyrPassage(second.localFrame(), destination.localFrame()))
        val route = listOf(gate, next)
        val target = Vec3(0.0, 0.0, -7.0)
        val held = GravitronGatePath.forward(route, target)
        assertEquals(0.0, target.distanceTo(GravitronGatePath.backward(route, held)), 1e-8)
        assertEquals(2, GravitronGatePath.depth(route, 1, target, held))
        assertEquals(1, GravitronGatePath.depth(route, 2, target, passage.point(target)))
        val legs = GravitronGatePath.legs(route, eye, held)
        val jumps = legs.map { it.contact to it.gate.passage.point(it.contact) }
        val shot = GatemakyrEffectPacket(ResourceLocation("test", "world"), owner, GatemakyrEffectPacket.Kind.SHOT, 0, 0,
            origin = eye, crossings = jumps)
        assertEquals(11.0, shot.flightLength(eye, held), 1e-8)
        assertEquals(0.0, jumps[0].second.distanceTo(shot.flightPoint(eye, held, 4.0 / 11.0)), 1e-8)
        val pieces = mutableListOf<Pair<Vec3, Vec3>>()
        shot.flightSegments(held, 0.25, 0.9) { _, a, b -> pieces.add(a to b); true }
        assertEquals(3, pieces.size)
        assertEquals(11 * 0.65, pieces.sumOf { (a, b) -> a.distanceTo(b) }, 1e-8)
    }

    @Test fun holdOrientationAndLaunchUseTheSameRotationAsTheGate() {
        for (face in Direction.values()) {
            val other = GatemakyrTarget(exit.position, face)
            val map = GatemakyrPassage(entrance.localFrame(), other.localFrame())
            val forward = Vec3(0.23, -0.47, -0.85).normalize()
            assertEquals(0.0, map.rotation().transform(forward.toJOML()).distance(map.direction(forward).toJOML()), 1e-8)
        }
    }
}
