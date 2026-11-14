package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotPhysics
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.joints.VSSpringJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSJoint
import org.joml.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class RopeTensionGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private var previousSync = false
        private val ships = mutableListOf<ServerShip>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val links = mutableListOf<WandLinks.Link>()
        private val payloadJoints = mutableListOf<Int>()

        @JvmStatic @BeforeBatch(batch = "rope_tension")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "rope_tension")
        fun cleanup(level: ServerLevel) {
            payloadJoints.clear()
            links.forEach { WandLinks.get(level).remove(level, it) }; links.clear()
            ships.forEach { level.shipObjectWorld.deleteShip(it) }; ships.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            players.forEach { level.removeTestPlayer(it) }; players.clear()
            channels.forEach { it.finishAndReleaseAll() }; channels.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstDoesNotPullSlackRope(helper: GameTestHelper) = slack(helper, false, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondDoesNotPullSlackRope(helper: GameTestHelper) = slack(helper, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, true, hanging = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, false, hanging = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun magicalRopeDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, true, physical = false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun magicalRopeSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, false, hanging = true, physical = false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun shorteningRopeUpdatesLiveJoint(helper: GameTestHelper) = slack(helper, true, true, hanging = true, reeling = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 120)
        fun redstoneReleasesFirstEndpointAndCatchesLoad(helper: GameTestHelper) = slack(helper, true, true, hanging = true, release = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 120)
        fun redstoneReleasesSecondEndpointAndCatchesLoad(helper: GameTestHelper) = slack(helper, true, false, hanging = true, release = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 120)
        fun releasedWinchStopsAtMaximumExtension(helper: GameTestHelper) = slack(helper, true, true, hanging = true, release = true, catchRelease = false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 140)
        fun winchUpdatesMassWithFirstEndpointFixed(helper: GameTestHelper) = slack(helper, true, true, hanging = true, massChange = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 140)
        fun winchUpdatesMassWithSecondEndpointFixed(helper: GameTestHelper) = slack(helper, true, false, hanging = true, massChange = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 180)
        fun ropeAndSpringTrackConnectedPayload(helper: GameTestHelper) = slack(helper, true, true, hanging = true, massChange = true, connectedLoad = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 180)
        fun magicalRopeTracksConnectedPayload(helper: GameTestHelper) = slack(helper, true, false, hanging = true, physical = false, massChange = true, connectedLoad = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 180)
        fun ropeAndSpringTrackSpringLinkedPayload(helper: GameTestHelper) = slack(helper, true, true, hanging = true, massChange = true, connectedLoad = true, springPayload = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 200)
        fun heavySpringStaysStableAsWorldWinchTightens(helper: GameTestHelper) = heavySpring(helper, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 200)
        fun heavySpringStaysStableAfterWinchRopeSnaps(helper: GameTestHelper) = heavySpring(helper, true, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 200)
        fun heavySpringStaysStableWhenWinchIsUnfrozen(helper: GameTestHelper) = heavySpring(helper, true, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 200)
        fun overloadedWinchRopeClearsItsJointAndSpringLoad(helper: GameTestHelper) = heavySpring(helper, true, true, overload = true)

        private fun heavySpring(helper: GameTestHelper, shipWinch: Boolean, snap: Boolean, overload: Boolean = false) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 220, it.z) }
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "spring-rope"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), base.y + 5.0, base.z.toDouble())
            player.setNoGravity(true); player.abilities.flying = true
            players.add(player)
            fun ship(pos: BlockPos, state: BlockState): ServerShip {
                level.setBlockAndUpdate(pos, state)
                return ShipAssembler.assembleToShip(level, setOf(pos.mutable()), 1.0).also {
                    it.isStatic = true
                    ships.add(it)
                    val box = it.shipAABB ?: error("Missing spring test ship bounds")
                    val chunk = ChunkPos(BlockPos(box.minX(), box.minY(), box.minZ()))
                    if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
                }
            }
            fun anchor(ship: ServerShip, face: Direction): WandAnchor {
                val box = ship.shipAABB!!
                return WandAnchor(BlockPos(box.minX(), box.minY(), box.minZ()), face, ship.id)
            }
            val light = ship(base.below(4), Blocks.OAK_LOG.defaultBlockState())
            val heavy = ship(base.below(8).east(), Blocks.NETHERITE_BLOCK.defaultBlockState())
            val winchState = ClockworkBlocks.ROPE_WINCH.getDefaultState()
            val winchPos = if (overload) base.above(12) else base
            val host = if (shipWinch) ship(winchPos, winchState) else null
            if (host == null) level.setBlockAndUpdate(winchPos, winchState)
            val winch = (host?.let { anchor(it, Direction.DOWN) } ?: WandAnchor(winchPos, Direction.DOWN)).copy(winch = true)
            val a = anchor(light, Direction.DOWN)
            val b = anchor(heavy, Direction.UP)
            val data = WandLinks.get(level)
            var spring: WandLinks.Link? = null
            var rope: WandLinks.Link? = null
            var taut = false
            var released = false
            var peakSpin = 0.0
            fun stable() {
                helper.assertTrue(light.velocity.isFinite && heavy.velocity.isFinite && light.angularVelocity.isFinite && heavy.angularVelocity.isFinite,
                    "Spring developed nonfinite motion")
                peakSpin = maxOf(peakSpin, light.angularVelocity.length(), heavy.angularVelocity.length())
                helper.assertTrue(peakSpin < 12.0, "Spring spun out: peak=$peakSpin, springMass=${spring!!.mass}, rope=${data.links.containsKey(rope!!.id)}")
                helper.assertTrue(light.velocity.distance(heavy.velocity) < if (released) 150.0 else 20.0,
                    "Spring developed excessive relative velocity: ${light.velocity.distance(heavy.velocity)}, mass=${spring!!.mass}, ropeMass=${rope!!.mass}, spin=$peakSpin")
                helper.assertTrue(data.links.containsKey(spring!!.id), "Spring broke during the stability fixture")
            }
            helper.startSequence().thenIdle(8).thenExecute {
                helper.assertTrue(heavy.inertiaData.mass > light.inertiaData.mass * 10, "Fixture needs a large mass ratio")
                spring = data.add(level, a, b, true, a.world(level)!!.distanceTo(b.world(level)!!), physical = true, spring = true)!!
                rope = data.add(level, winch, a.copy(face = Direction.UP), true,
                    winch.world(level)!!.distanceTo(a.copy(face = Direction.UP).world(level)!!) + 1.0, physical = true)!!
                links.add(spring!!); links.add(rope!!)
            }.thenWaitUntil {
                helper.assertTrue(spring!!.jointId != null && spring!!.alignmentId != null && rope!!.jointId != null, "Fixture joints are not ready")
            }.thenExecute {
                light.isStatic = false
                heavy.isStatic = false
            }.thenExecuteFor(45) {
                if (data.links.containsKey(rope!!.id)) {
                    data.resizeRope(level, rope!!, maxOf(2.5, rope!!.length - 0.125))
                    val distance = rope!!.a.world(level)!!.distanceTo(rope!!.b.world(level)!!)
                    taut = taut || distance >= rope!!.length
                }
                stable()
            }.thenExecute {
                helper.assertTrue(taut, "Winch never took up its slack")
                if (snap) {
                    helper.assertTrue(data.links.containsKey(rope!!.id), "Winch rope snapped before the break test")
                    if (overload) data.resizeRope(level, rope!!, 0.5)
                    else level.gtpa.removeJoint(rope!!.jointId!!)
                } else if (host != null) host.isStatic = false
                released = snap || host != null
            }.thenExecuteFor(50) {
                stable()
            }.thenExecute {
                if (snap) {
                    helper.assertTrue(!data.links.containsKey(rope!!.id), "Rope break did not clear the saved link")
                    helper.assertTrue(level.gtpa.getJointById(rope!!.jointId!!) == null, "Broken rope retained its native joint")
                    data.remove(level, rope!!)
                }
                helper.assertTrue(light.velocity.distance(heavy.velocity) < 10.0,
                    "Spring motion did not settle after release: ${light.velocity.distance(heavy.velocity)}")
                val maximumLoad = light.inertiaData.mass + if (!snap && host != null) host.inertiaData.mass else 0.0
                val maximumMass = (1.0 / (1.0 / maximumLoad + 1.0 / heavy.inertiaData.mass)).coerceAtLeast(100.0)
                helper.assertTrue(spring!!.mass <= maximumMass * 1.01,
                    "Spring retained anchored mass: ${spring!!.mass} instead of at most $maximumMass")
            }.thenSucceed()
        }

        private fun slack(helper: GameTestHelper, vertical: Boolean, staticFirst: Boolean, hanging: Boolean = false, physical: Boolean = true, reeling: Boolean = false, release: Boolean = false, catchRelease: Boolean = true, massChange: Boolean = false, connectedLoad: Boolean = false, springPayload: Boolean = false) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 180, it.z) }
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "rope-physics"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), base.y + 5.0, base.z.toDouble())
            player.setNoGravity(true); player.abilities.flying = true
            players.add(player)
            fun ship(pos: BlockPos, fixed: Boolean): ServerShip {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState())
                return ShipAssembler.assembleToShip(level, setOf(pos.mutable()), 1.0).also {
                    it.isStatic = fixed
                    ships.add(it)
                }
            }
            val fixed = ship(base, true)
            val moving = ship(if (hanging) base.below(6) else if (vertical) base.above(6) else base.offset(6, 0, 0), true)
            val control = ship(base.offset(3, 6, 3), false)
            val payload = if (connectedLoad) ship(base.offset(4, -6, 0), true) else null
            fun anchor(ship: ServerShip): WandAnchor {
                val box = ship.shipAABB ?: error("Missing rope test ship bounds")
                val pos = BlockPos(box.minX(), box.minY(), box.minZ())
                val chunk = ChunkPos(pos)
                if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
                return WandAnchor(pos, Direction.NORTH, ship.id)
            }
            val a = anchor(fixed).let { if (release || (massChange && physical)) it.copy(face = Direction.DOWN, winch = true) else it }
            val b = anchor(moving)
            anchor(control)
            var payloadPoint = Vector3d()
            if (payload != null) {
                val payloadAnchor = anchor(payload)
                payloadAnchor.local().let { payloadPoint = Vector3d(it.x, it.y, it.z) }
                BlockPos.betweenClosed(payloadAnchor.pos.offset(-1, -1, -1), payloadAnchor.pos.offset(1, 1, 1))
                    .map { it.immutable() }.filter { it != payloadAnchor.pos }
                    .forEach { level.setBlockAndUpdate(it, Blocks.IRON_BLOCK.defaultBlockState()) }
            }
            if (release || (massChange && physical)) {
                level.setBlockAndUpdate(a.pos, ClockworkBlocks.ROPE_WINCH.getDefaultState())
                (level.getBlockEntity(a.pos) as RopeWinchBlockEntity).maximum.setValue(if (catchRelease) 24 else 12)
            }
            var link: WandLinks.Link? = null
            var movingAnchor: WandAnchor? = null
            var initial = Vec3.ZERO
            var peakDistance = 0.0
            var originalLength = 0.0
            var originalMass = 0.0
            var originalJoint: Int? = null
            var payloadJoint: Int? = null
            var spring: WandLinks.Link? = null
            val ballast = BlockPos.betweenClosed(b.pos.offset(-1, -1, -1), b.pos.offset(1, 1, 1))
                .map { it.immutable() }.filter { it != b.pos }
            fun assertUpdatedMass() {
                val mass = moving.inertiaData.mass + if (payloadJoint != null) payload!!.inertiaData.mass else 0.0
                helper.assertTrue(abs(link!!.mass - mass) < mass * 0.01, "Winch mass did not track its dynamic load: ${link!!.mass} / $mass")
                helper.assertTrue(link!!.jointId == originalJoint, "Mass change replaced the rope joint")
                val joint = level.gtpa.getJointById(link!!.jointId!!) as? VSDistanceJoint
                helper.assertTrue(joint != null, "Winch joint disappeared after mass change")
                val stiffness = joint!!.stiffness?.toDouble() ?: (1.0 / joint.compliance)
                helper.assertTrue(abs(stiffness / link!!.mass - if (physical) RopeKnotPhysics.STIFFNESS else 625.0) < 0.1,
                    "Native rope stiffness did not update: stiffness=$stiffness, applied=${link!!.mass}, load=$mass, payload=$payloadJoint")
                if (joint.damping != null) helper.assertTrue(abs(joint.damping!!.toDouble() / link!!.mass - if (physical) RopeKnotPhysics.DAMPING else 40.0) < 0.1,
                    "Native rope damping did not update with mass")
                spring?.let {
                    helper.assertTrue(abs(it.mass - mass) < mass * 0.01, "Spring and rope disagree on connected mass")
                    val native = it.jointId?.let { id -> level.gtpa.getJointById(id) } as? VSSpringJoint
                    helper.assertTrue(native != null && abs(native.stiffness.toDouble() / it.mass - 87.5) < 0.1,
                        "Native spring stiffness did not track connected mass")
                }
            }
            val stretchAllowance = if (release && catchRelease) 2.0 else 1.0
            helper.startSequence().thenIdle(8).thenWaitUntil {
                helper.assertTrue(control.velocity.y() < -0.1,
                    "Unlinked control ship is not falling: velocity=${control.velocity}, mass=${control.inertiaData.mass}, static=${control.isStatic}")
            }.thenExecute {
                moving.isStatic = false
                movingAnchor = b
                val length = a.world(level)!!.distanceTo(b.world(level)!!) + 3.0
                link = WandLinks.get(level).add(level, if (staticFirst) a else b, if (staticFirst) b else a,
                    true, length, physical = physical)
                helper.assertTrue(link != null, "Failed to create rope")
                links.add(link!!)
            }.thenWaitUntil {
                helper.assertTrue(link?.jointId != null, "Rope joint was not created")
            }.thenExecute {
                initial = movingAnchor!!.world(level)!!
                originalLength = link!!.length
                if (reeling) WandLinks.get(level).resizeRope(level, link!!, link!!.length - 1.0)
                if (release) level.setBlockAndUpdate(a.pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState())
                helper.assertTrue(initial.y > 100 && !moving.isStatic, "Rope fixture must be dynamic and clear of terrain: $initial")
            }.thenIdle(if (release) 20 else 0).thenExecute {
                if (release) {
                    val distance = a.world(level)!!.distanceTo(b.world(level)!!)
                    helper.assertTrue(distance > originalLength + 1.0, "Released winch restrained its load: $distance / $originalLength")
                    helper.assertTrue(link!!.ropeLimit == (if (catchRelease) 24.0 else 12.0) && link!!.stress == 0f, "Released rope remained under tension")
                    if (catchRelease) {
                        level.removeBlock(a.pos.above(), false)
                        WandLinks.get(level).updateWinchRelease(level, link!!)
                        helper.assertTrue(link!!.releasedMaximum == null && link!!.length >= distance,
                            "Winch did not retain the paid-out rope on re-engagement")
                    }
                }
            }.thenExecuteFor(if (hanging) 40 else 8) {
                if (hanging) {
                    val distance = a.world(level)!!.distanceTo(b.world(level)!!)
                    peakDistance = maxOf(peakDistance, distance)
                    helper.assertTrue(distance < link!!.length + stretchAllowance, "Rope did not arrest its falling load: $distance / ${link!!.length}")
                }
            }.thenExecute {
                val current = movingAnchor!!.world(level)!!
                val distance = link!!.a.world(level)!!.distanceTo(link!!.b.world(level)!!)
                helper.assertTrue(WandLinks.get(level).links.containsKey(link!!.id),
                    "Rope broke under gravity: distance=$distance, length=${link!!.length}, missing=${link!!.missingTicks}, age=${link!!.age}")
                helper.assertTrue(link!!.missingTicks == 0, "Native rope joint is missing")
                if (hanging) {
                    if (release && !catchRelease) helper.assertTrue(link!!.length == 12.0, "Released winch did not stop at its configured maximum")
                    helper.assertTrue(peakDistance > link!!.length && peakDistance < link!!.length + stretchAllowance,
                        "Rope should stretch slightly and support its load: peak=$peakDistance / ${link!!.length}")
                    return@thenExecute
                }
                helper.assertTrue(distance < link!!.length - 0.5, "Fixture exhausted its slack: $distance / ${link!!.length}")
                if (vertical) helper.assertTrue(current.y < initial.y - 0.5,
                    "Rope propped up a ship: initial=$initial, current=$current")
                else helper.assertTrue(abs(current.x - initial.x) < 0.1 && abs(current.z - initial.z) < 0.1,
                    "Slack rope pulled toward the static ship: initial=$initial, current=$current")
            }.thenExecute {
                if (massChange) {
                    originalMass = moving.inertiaData.mass
                    originalJoint = link!!.jointId
                    assertUpdatedMass()
                    if (connectedLoad) {
                        val springA = a.copy(winch = false, face = Direction.DOWN)
                        val springB = b.copy(face = Direction.UP)
                        spring = WandLinks.get(level).add(level, springA, springB, true,
                            springA.world(level)!!.distanceTo(springB.world(level)!!), physical = true, spring = true)
                        helper.assertTrue(spring != null, "Failed to create parallel spring")
                        links.add(spring!!)
                        payload!!.isStatic = false
                        val point = b.world(level)!!
                        val local = b.local()
                        val rootPose = VSJointPose(Vector3d(local.x, local.y, local.z), Quaterniond())
                        val joint: VSJoint = if (springPayload) {
                            val other = payload!!.transform.shipToWorld.transformPosition(payloadPoint, Vector3d())
                            VSSpringJoint(moving.id, rootPose, payload!!.id, VSJointPose(payloadPoint, Quaterniond()),
                                restLength = other.distance(point.x, point.y, point.z).toFloat(),
                                stiffness = (payload!!.inertiaData.mass * 350.0).toFloat(), damping = (payload!!.inertiaData.mass * 28.0).toFloat())
                        } else VSRevoluteJoint(moving.id, rootPose, payload!!.id,
                            VSJointPose(payload!!.transform.worldToShip.transformPosition(Vector3d(point.x, point.y, point.z)), Quaterniond()))
                        level.gtpa.addJoint(joint) { id -> level.server.execute {
                            if (id >= 0) { payloadJoint = id; payloadJoints.add(id) }
                        } }
                    } else ballast.forEach { level.setBlockAndUpdate(it, Blocks.IRON_BLOCK.defaultBlockState()) }
                }
            }.thenWaitUntil {
                if (massChange) {
                    if (connectedLoad) {
                        helper.assertTrue(payloadJoint != null && payload!!.inertiaData.mass > 25_000.0,
                            "Payload was not attached: joint=$payloadJoint, mass=${payload!!.inertiaData.mass}")
                        helper.assertTrue(abs(moving.inertiaData.mass - originalMass) < 1.0, "Payload must remain a separate ship")
                    } else helper.assertTrue(moving.inertiaData.mass > maxOf(25_000.0, originalMass * 2), "Ballast did not increase ship mass")
                    assertUpdatedMass()
                }
            }.thenExecute {
                if (connectedLoad) {
                    level.gtpa.removeJoint(payloadJoint!!)
                    payloadJoints.remove(payloadJoint!!)
                    payloadJoint = null
                } else if (massChange) ballast.forEach { level.removeBlock(it, false) }
            }.thenWaitUntil {
                if (massChange) {
                    helper.assertTrue(abs(moving.inertiaData.mass - originalMass) < 1.0, "Removing ballast did not restore ship mass")
                    assertUpdatedMass()
                }
            }.thenSucceed()
        }
    }
}
