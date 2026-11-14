package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
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
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.*
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.content.forces.GravitronController
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class GravitronControlGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableListOf<ServerShip>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val joints = mutableListOf<Int>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = "gravitron_controls")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "gravitron_controls")
        fun cleanup(level: ServerLevel) {
            players.forEach { GrabTool.dropShip(it, false) }
            joints.forEach { level.gtpa.removeJoint(it) }; joints.clear()
            ships.forEach { ship -> level.shipObjectWorld.allShips.getById(ship.id)?.let { level.shipObjectWorld.deleteShip(it) } }; ships.clear()
            players.forEach { level.removeTestPlayer(it) }; players.clear()
            channels.forEach { it.finishAndReleaseAll() }; channels.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "gravitron_controls", timeoutTicks = 140)
        fun rotationAndDistanceKeepCameraAndAnchorStable(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 220, it.z) }
            val player = player(level, base)
            val ship = assemble(level, base, false)
            val pos = anchor(ship)
            val state = GravitronState.getState(player)
            var before = Quaterniond()
            var distance = 0.0
            helper.startSequence().thenIdle(15).thenExecute {
                GrabTool.updateEquipment(player)
                helper.assertTrue(GrabTool.tryGrabShip(level, player, pos, Vec3.atCenterOf(pos), true), "Could not grab the test ship")
                before = Quaterniond(state.shipGrabbedRot!!)
                distance = state.shipGrabbedDistance!!
                GravitronInputPacket(GravitronInputPacket.Action.ROTATE, ship.id, 20f, 10f).apply(player)
                GravitronInputPacket(GravitronInputPacket.Action.DISTANCE, ship.id, 1f).apply(player)
                helper.assertTrue(kotlin.math.abs(before.dot(state.shipGrabbedRot!!)) < 0.999, "Mouse rotation did not change the ship target")
                helper.assertTrue(state.shipGrabbedDistance == distance - 1.0, "Scroll did not reel the ship closer")
                helper.assertTrue(player.xRot == 0f && player.yRot == 0f, "Ship rotation changed the camera")
            }.thenIdle(1).thenExecute {
                val rotation = Quaterniond(state.shipGrabbedRot!!)
                GravitronInputPacket(GravitronInputPacket.Action.ROTATE, ship.id + 100000, 40f, 0f).apply(player)
                GravitronInputPacket(GravitronInputPacket.Action.ROTATE, ship.id, Float.NaN, 0f).apply(player)
                GravitronInputPacket(GravitronInputPacket.Action.DISTANCE, ship.id, Float.POSITIVE_INFINITY).apply(player)
                helper.assertTrue(rotation == state.shipGrabbedRot && state.shipGrabbedDistance == distance - 1.0, "Invalid or stale input changed the grab")
                GravitronInputPacket(GravitronInputPacket.Action.DISTANCE, ship.id, -1f).apply(player)
                helper.assertTrue(state.shipGrabbedDistance == distance, "Scroll did not move the ship farther away")
            }.thenExecuteFor(35) { GrabTool.tick(player) }.thenExecute {
                helper.assertTrue(ship.velocity.isFinite && ship.angularVelocity.isFinite, "Rotation produced invalid motion")
                val worldAnchor = ship.shipToWorld.transformPosition(Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))
                helper.assertTrue(worldAnchor.distance(state.heldBlockPos!!) < 0.5, "Rotation pulled the grab anchor away from its target")
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                GravitronInputPacket(GravitronInputPacket.Action.DISTANCE, ship.id, 1f).apply(player)
                helper.assertTrue(state.shipID == null, "Changing tools retained the grab")
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id)?.getAttachment(GravitronController::class.java)?.data == null, "Released ship retained its force controller")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "gravitron_controls", timeoutTicks = 160)
        fun connectedMassTracksRigidLoadsAndJointRemoval(helper: GameTestHelper) = connected(helper, "fixed")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "gravitron_controls", timeoutTicks = 160)
        fun connectedMassStaysStableWithSpringPayload(helper: GameTestHelper) = connected(helper, "spring")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "gravitron_controls", timeoutTicks = 160)
        fun connectedMassStaysStableWithRopePayload(helper: GameTestHelper) = connected(helper, "rope")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "gravitron_controls", timeoutTicks = 180)
        fun localMassStaysStableWithJointedPayload(helper: GameTestHelper) = connected(helper, "revolute", false)

        private fun connected(helper: GameTestHelper, kind: String, fullMass: Boolean = true) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 220, it.z) }
            val player = player(level, base)
            val root = assemble(level, base, false)
            val payload = assemble(level, base.below(3), true)
            val extraPayloads = if (fullMass) emptyList() else listOf(6, 9).map { assemble(level, base.below(it), true) }
            val extraJoints = mutableListOf<Int>()
            val pos = anchor(root)
            val rootMass = root.inertiaData.mass
            val payloadMass = payload.inertiaData.mass
            var jointId: Int? = null
            helper.startSequence().thenIdle(15).thenExecute {
                val point = Vector3d(root.transform.positionInWorld)
                val poseA = VSJointPose(root.worldToShip.transformPosition(point, Vector3d()), Quaterniond())
                val poseB = VSJointPose(payload.worldToShip.transformPosition(
                    if (kind == "fixed" || kind == "revolute") point else payload.transform.positionInWorld, Vector3d()), Quaterniond())
                val joint = when (kind) {
                    "fixed" -> VSFixedJoint(root.id, poseA, payload.id, poseB)
                    "revolute" -> VSRevoluteJoint(root.id, poseA, payload.id, poseB)
                    "spring" -> VSSpringJoint(root.id, poseA, payload.id, poseB, restLength = 3f,
                        stiffness = (rootMass * 350).toFloat(), damping = (rootMass * 28).toFloat())
                    else -> VSDistanceJoint(root.id, poseA, payload.id, poseB, maxDistance = 3f)
                }
                level.gtpa.addJoint(joint) { id -> level.server.execute { jointId = id; joints.add(id) } }
                (listOf(payload) + extraPayloads).zipWithNext().forEach { (a, b) ->
                    val pivot = Vector3d(a.transform.positionInWorld).lerp(b.transform.positionInWorld, .5)
                    level.gtpa.addJoint(VSRevoluteJoint(a.id,
                        VSJointPose(a.worldToShip.transformPosition(pivot, Vector3d()), Quaterniond()), b.id,
                        VSJointPose(b.worldToShip.transformPosition(pivot, Vector3d()), Quaterniond()))) { id ->
                        level.server.execute { extraJoints.add(id); joints.add(id) }
                    }
                }
            }.thenWaitUntil {
                helper.assertTrue(jointId != null && extraJoints.size == extraPayloads.size, "Joint has not been created")
            }.thenExecute {
                GrabTool.updateEquipment(player)
                helper.assertTrue(GrabTool.tryGrabShip(level, player, pos, Vec3.atCenterOf(pos).add(if (fullMass) 0.0 else .45, .0, .0), true), "Could not grab connected root")
                payload.isStatic = false
                extraPayloads.forEach { it.isStatic = false }
                helper.assertTrue(GravitronLoad.mass(level, root, player.mainHandItem) == rootMass, "Default mode counted other ships")
                if (fullMass) GravitronInputPacket(GravitronInputPacket.Action.CONNECTED_MASS).apply(player)
                helper.assertTrue(GravitronLoad.enabled(player.mainHandItem) == fullMass, "Wrong mass mode")
                helper.assertTrue(kotlin.math.abs(GravitronLoad.mass(level, root, player.mainHandItem) - rootMass - (if (fullMass) payloadMass else 0.0)) < 0.01,
                    "Connected mode did not count the payload")
            }.thenExecuteFor(35) {
                if (!fullMass) player.yRot = (kotlin.math.sin(level.gameTime * .15) * 25).toFloat()
                GrabTool.tick(player)
                helper.assertTrue(root.velocity.isFinite && root.angularVelocity.isFinite && root.velocity.length() < 30 && root.angularVelocity.length() < 10,
                    "Moving a jointed load caused unstable motion")
            }.thenExecute {
                val data = level.shipObjectWorld.loadedShips.getById(root.id)?.getAttachment(GravitronController::class.java)?.data
                helper.assertTrue(data != null && kotlin.math.abs(data.supportedMass - rootMass - (if (fullMass) payloadMass else 0.0)) < 0.01, "Force controller did not receive the connected load")
                helper.assertTrue(root.velocity.isFinite && root.velocity.length() < 30 && root.angularVelocity.length() < 10, "Connected load destabilized the grabbed ship")
                helper.assertTrue(kotlin.math.abs(root.inertiaData.mass - rootMass) < 0.01, "Mass compensation changed the actual ship mass")
                level.gtpa.removeJoint(jointId!!)
            }.thenWaitUntil {
                helper.assertTrue(level.gtpa.getJointById(jointId!!) == null, "Removed joint is still registered")
            }.thenExecuteFor(10) { GrabTool.tick(player) }.thenExecute {
                helper.assertTrue(GravitronLoad.mass(level, root, player.mainHandItem) == rootMass, "Disconnected payload still contributes mass")
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(root.id)?.getAttachment(GravitronController::class.java)?.data?.supportedMass == rootMass, "Force controller retained stale connected mass")
                if (fullMass) GravitronInputPacket(GravitronInputPacket.Action.CONNECTED_MASS).apply(player)
                helper.assertTrue(!GravitronLoad.enabled(player.mainHandItem), "Mass toggle did not turn off")
                helper.assertTrue(root.velocity.isFinite && root.velocity.length() < 30, "Removing a joint left unstable motion")
            }.thenSucceed()
        }

        private fun anchor(ship: ServerShip): BlockPos = ship.shipAABB!!.let { BlockPos(it.minX(), it.minY(), it.minZ()) }

        private fun assemble(level: ServerLevel, pos: BlockPos, heavy: Boolean): ServerShip {
            level.setBlockAndUpdate(pos, (if (heavy) Blocks.NETHERITE_BLOCK else Blocks.OAK_LOG).defaultBlockState())
            return ShipAssembler.assembleToShip(level, setOf(pos.mutable()), 1.0).also {
                it.isStatic = true; ships.add(it)
                val bounds = it.shipAABB!!
                for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4))
                    if (level.setChunkForced(x, z, true)) chunks.add(ChunkPos(x, z))
            }
        }

        private fun player(level: ServerLevel, pos: BlockPos): ServerPlayer {
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive(); channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            return ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "gravitron-test")).also {
                level.server.playerList.placeNewPlayer(connection, it)
                it.moveTo(pos.x + 0.5, pos.y + 0.5 - it.eyeHeight, pos.z - 4.5, 0f, 0f)
                it.setNoGravity(true); it.abilities.flying = true
                it.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(ClockworkItems.CREATIVE_GRAVITRON.get()))
                players.add(it)
            }
        }
    }
}
