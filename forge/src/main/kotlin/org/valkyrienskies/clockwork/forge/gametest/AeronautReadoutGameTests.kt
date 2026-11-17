package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.*
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.util.UUID
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class AeronautReadoutGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = ArrayList<ServerShip>()
        private val observers = ArrayList<ServerPlayer>()
        private val connections = ArrayList<EmbeddedChannel>()
        private val assemblyObservers = ArrayList<ShipTestObserver>()
        private val forced = HashSet<ChunkPos>()
        private var previousSync = false

        @JvmStatic
        @BeforeBatch(batch = "aeronaut_readout")
        fun synchronizePhysics(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic
        @AfterBatch(batch = "aeronaut_readout")
        fun cleanup(level: ServerLevel) {
            ships.forEach { level.shipObjectWorld.deleteShip(it) }
            ships.clear()
            observers.forEach { level.server.playerList.remove(it) }
            observers.clear()
            connections.forEach { it.finishAndReleaseAll() }
            connections.clear()
            assemblyObservers.forEach(ShipTestObserver::close)
            assemblyObservers.clear()
            forced.forEach { level.setChunkForced(it.x, it.z, false) }
            forced.clear()
            AeronautShipReadout.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout")
        fun inspectionRequiresEquippedLoweredGoggles(helper: GameTestHelper) {
            val player = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "hud-test"))
            val state = player as MixinPlayerDuck
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "An unequipped player could inspect ships")
            player.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "Raised goggles could inspect ships")
            state.gogglesDown = true
            helper.assertTrue(AeronautShipRequestPacket.canInspect(player), "Lowered equipped goggles could not inspect ships")
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "Removing goggles retained inspection access")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout", timeoutTicks = 100)
        fun designCentersUpdateAfterRemovingSources(helper: GameTestHelper) {
            // VS needs a nearby observer to load the assembled ship in a headless server.
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            connections.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val observer = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "readout-test"))
            helper.level.server.playerList.placeNewPlayer(connection, observer)
            val observerPos = helper.absolutePos(BlockPos(2, 10, 2))
            observer.setPos(observerPos.x.toDouble(), observerPos.y.toDouble(), observerPos.z.toDouble())
            observer.setNoGravity(true)
            observers.add(observer)
            val left = BlockPos(1, 2, 1)
            val right = BlockPos(3, 2, 1)
            val middle = BlockPos(2, 2, 1)
            val rocket = BlockPos(2, 2, 2)
            helper.setBlock(left, ClockworkBlocks.WING.get().defaultBlockState())
            helper.setBlock(right, ClockworkBlocks.WING.get().defaultBlockState())
            helper.setBlock(middle, Blocks.STONE.defaultBlockState())
            helper.setBlock(rocket, ClockworkBlocks.SUGAR_ROCKET.get().defaultBlockState())
            val positions = listOf(left, right, middle, rocket).map { helper.absolutePos(it) }.toSet()
            val ship = ShipAssembler.assembleToShip(helper.level, positions, 1.0)
            ships.add(ship)
            ship.isStatic = true
            helper.runAfterDelay(5) {
                val loadedShip = helper.level.shipObjectWorld.loadedShips.getById(ship.id)
                helper.assertTrue(loadedShip != null, "Observer did not load the ship")
                val bounds = loadedShip!!.shipAABB!!
                fun local(pos: BlockPos) = Vector3d(bounds.minX() + pos.x - left.x + .5,
                    bounds.minY() + pos.y - left.y + .5, bounds.minZ() + pos.z - left.z + .5)
                val leftLocal = local(left)
                val rightLocal = local(right)
                val rocketLocal = local(rocket)
                val snapshot = AeronautShipReadout.sample(helper.level, loadedShip!!)
                helper.assertTrue(snapshot.mass > 0, "Ship mass was missing")
                helper.assertTrue(snapshot.potentialThrust != null && snapshot.potentialThrust!!.position.distance(rocketLocal) < 1e-6,
                    "Unlit rocket's potential thrust ${snapshot.potentialThrust} was not at $rocketLocal; active chunks=${loadedShip.activeChunksSet.size}")
                helper.assertTrue(snapshot.liveThrust == null, "An unlit rocket produced a live thrust marker")
                val expectedLift = Vector3d(leftLocal).add(rightLocal).mul(.5)
                helper.assertTrue(snapshot.potentialLift != null && snapshot.potentialLift!!.position.distance(expectedLift) < 1e-6,
                    "Two equal wings did not balance between their positions")
                helper.level.setBlockAndUpdate(BlockPos.containing(rightLocal.x, rightLocal.y, rightLocal.z), Blocks.AIR.defaultBlockState())
                helper.level.setBlockAndUpdate(BlockPos.containing(rocketLocal.x, rocketLocal.y, rocketLocal.z), Blocks.AIR.defaultBlockState())
                helper.runAfterDelay(6) {
                    val updated = AeronautShipReadout.sample(helper.level, loadedShip)
                    helper.assertTrue(updated.potentialThrust == null, "Removing a rocket left a stale thrust marker")
                    helper.assertTrue(updated.potentialLift != null && updated.potentialLift!!.position.distance(leftLocal) < 1e-6,
                        "Removing a wing did not invalidate the cached lift center")
                    helper.succeed()
                }
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout")
        fun snapshotSurvivesTheNetworkCodec(helper: GameTestHelper) {
            val center = AeronautForceCenter(Vector3d(30000000.5, 70.0, -100.5), Vector3d(), 200.0)
            val pad = AeronautAssemblyReadout.Anchor(73L, BlockPos(30000001, 72, -102))
            val hinge = AeronautAssemblyReadout.Anchor(null, BlockPos(35, 72, -104))
            val assembly = AeronautAssemblyReadout.Detail(2, listOf(AeronautAssemblyReadout.Foot(pad, 200.25)),
                listOf(Vector3d(35.5, 70.0, -101.25), Vector3d(36.5, 70.0, -103.25)),
                listOf(AeronautAssemblyReadout.Joint(hinge, AeronautAssemblyReadout.JointKind.HINGE, -11.5, lower = -60.0, upper = 45.0),
                    AeronautAssemblyReadout.Joint(pad, AeronautAssemblyReadout.JointKind.GIMBAL, 25.0, 42.0, error = 17.0)),
                listOf(AeronautAssemblyReadout.ContactFace(73L, Vector3d(30000001.25, 71.0, -101.75), 250.75)))
            val snapshot = AeronautShipReadout.Snapshot(1500.0, Vector3d(30000001.5, 71.0, -101.5), center, null, null, center,
                assembly, visibleGuides = 4)
            val buffer = FriendlyByteBuf(Unpooled.buffer())
            try {
                AeronautShipReadoutPacket(42L, helper.level.dimension().location(), snapshot).write(buffer)
                val decoded = AeronautShipReadoutPacket(buffer)
                helper.assertTrue(decoded.shipId == 42L && decoded.dimension == helper.level.dimension().location(), "Ship identity changed in transit")
                helper.assertTrue(decoded.snapshot.mass == 1500.0 && decoded.snapshot.massCenter == snapshot.massCenter, "Mass data changed in transit")
                helper.assertTrue(decoded.snapshot.liveThrust == null && decoded.snapshot.potentialLift == null, "Missing centers became phantom markers")
                helper.assertTrue(decoded.snapshot.potentialThrust == center && decoded.snapshot.liveLift == center, "Zero resultant forces lost their weighted centers")
                helper.assertTrue(decoded.snapshot.assembly == assembly, "Assembly support or joint data changed in transit")
                helper.assertTrue(decoded.snapshot.massVisible && decoded.snapshot.visibleGuides == snapshot.visibleGuides,
                    "Terrain visibility changed in transit")
            } finally { buffer.release() }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout", timeoutTicks = 180)
        fun airborneAssemblyIncludesConnectedMassAndJointReadings(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(3, 3, 3)).atY(220)
            assemblyObservers.add(ShipTestObserver(level, base))
            fun makeShip(pos: BlockPos, facing: Direction, ballast: Boolean): Pair<ServerShip, BlockPos> {
                level.setBlockAndUpdate(pos, ClockworkBlocks.EXTENDON.defaultState.setValue(FACING, facing))
                if (ballast) level.setBlockAndUpdate(pos.below(), Blocks.NETHERITE_BLOCK.defaultBlockState())
                val ship = ShipAssembler.assembleToShip(level, if (ballast) setOf(pos, pos.below()) else setOf(pos), 1.0)
                ship.isStatic = true
                ships.add(ship)
                val bounds = ship.shipAABB!!
                val anchor = BlockPos(bounds.minX(), bounds.maxY() - 1, bounds.minZ())
                val chunk = ChunkPos(anchor)
                if (level.setChunkForced(chunk.x, chunk.z, true)) forced.add(chunk)
                return ship to anchor
            }
            val (root, rootPos) = makeShip(base, Direction.UP, true)
            val (leaf, leafPos) = makeShip(base.above(8), Direction.DOWN, false)
            lateinit var extendon: ExtendonBlockEntity
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(root.id) != null &&
                    level.shipObjectWorld.loadedShips.getById(leaf.id) != null, "Assembly bodies did not load")
            }.thenExecute {
                extendon = level.getBlockEntity(rootPos) as ExtendonBlockEntity
                helper.assertTrue(extendon.tryConnect(level, leafPos), "Could not connect the airborne assembly")
            }.thenWaitUntil {
                val loaded = level.shipObjectWorld.loadedShips.getById(root.id)!!
                val snapshot = AeronautShipReadout.sample(level, loaded)
                val expectedMass = root.inertiaData.mass + leaf.inertiaData.mass
                val expectedWorld = Vector3d(root.transform.positionInWorld).mul(root.inertiaData.mass)
                    .fma(leaf.inertiaData.mass, leaf.transform.positionInWorld).div(expectedMass)
                val center = root.transform.shipToWorld.transformPosition(snapshot.massCenter, Vector3d())
                helper.assertTrue(snapshot.assembly.bodyCount == 2 && abs(snapshot.mass - expectedMass) < 0.01 &&
                    center.distance(expectedWorld) < 0.01, "Airborne assembly missed the mass-weighted center")
                helper.assertTrue(snapshot.assembly.outline.isEmpty() && snapshot.assembly.feet.isEmpty(), "Airborne assembly gained support")
                val joint = snapshot.assembly.joints.firstOrNull { it.anchor.pos == rootPos }
                helper.assertTrue(joint?.kind == AeronautAssemblyReadout.JointKind.EXTENDON && abs(joint.measured - 8.0) < 0.1 &&
                    joint.target == 1.5, "Extendon target and measured position were not reported together: $joint")
            }.thenExecute {
                val inspector = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "visibility-test"))
                inspector.setPos(base.x + 0.5, base.y + 0.5 - inspector.eyeHeight, base.z - 7.5)
                inspector.yRot = 0f
                inspector.xRot = 0f
                inspector.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
                (inspector as MixinPlayerDuck).gogglesDown = true
                val open = AeronautShipRequestPacket.inspect(inspector, root.id)
                helper.assertTrue(open?.massVisible == true && open.assembly.joints.map { it.anchor.pos }.toSet() == setOf(rootPos, leafPos),
                    "An exposed assembly lost its mass or connected joint readouts: $open")
                val upper = base.north(4).above(4)
                val lower = base.north().below()
                val fullWall = (-1..1).flatMap { x -> (-1..6).map { y -> base.north(4).offset(x, y, 0) } }
                try {
                    level.setBlockAndUpdate(upper, Blocks.STONE.defaultBlockState())
                    val partial = AeronautShipRequestPacket.inspect(inspector, root.id)
                    helper.assertTrue(partial?.massVisible == true && partial.assembly.joints.map { it.anchor.pos } == listOf(rootPos),
                        "The server sent a joint behind terrain or hid the exposed one: $partial")
                    level.setBlockAndUpdate(lower, Blocks.STONE.defaultBlockState())
                    val masked = AeronautShipRequestPacket.inspect(inspector, root.id)
                    helper.assertTrue(masked?.massVisible == false && masked.mass == 0.0 && masked.massCenter.lengthSquared() == 0.0 &&
                        masked.assembly.joints.map { it.anchor.pos } == listOf(rootPos), "The server sent a covered center of mass: $masked")
                    fullWall.forEach { level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState()) }
                    helper.assertTrue(AeronautShipRequestPacket.inspect(inspector, root.id) == null,
                        "A direct request obtained telemetry for a wholly covered ship")
                } finally {
                    (fullWall + lower).forEach { level.removeBlock(it, false) }
                }
                extendon.disconnect()
            }.thenWaitUntil {
                val loaded = level.shipObjectWorld.loadedShips.getById(root.id)!!
                val snapshot = AeronautShipReadout.sample(level, loaded)
                helper.assertTrue(snapshot.assembly.bodyCount == 1 && snapshot.mass == root.inertiaData.mass &&
                    snapshot.assembly.joints.isEmpty(), "Disconnected limb remained in the goggles readout")
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout", timeoutTicks = 220)
        fun ordinaryBlocksReportOneLoadedFace(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(3, 3, 3)).atY(205)
            assemblyObservers.add(ShipTestObserver(level, base))
            for (x in -1..3) for (z in -1..2) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.STONE.defaultBlockState())
            val blocks = (0..2).flatMap { x -> (0..1).map { z -> base.offset(x, 2, z) } }.toSet()
            blocks.forEach { level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState()) }
            val ship = ShipAssembler.assembleToShip(level, blocks, 1.0)
            ships.add(ship)
            ship.isStatic = true
            val bounds = ship.shipAABB!!
            for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4)) {
                if (level.setChunkForced(x, z, true)) forced.add(ChunkPos(x, z))
            }
            fun detail(): AeronautAssemblyReadout.Detail {
                val loaded = level.shipObjectWorld.loadedShips.getById(ship.id)
                helper.assertTrue(loaded != null, "The test ship has not loaded")
                return AeronautShipReadout.sample(level, loaded!!).assembly
            }
            fun planted(result: AeronautAssemblyReadout.Detail) {
                helper.assertTrue(result.feet.isEmpty() && result.outline.size >= 3 && result.contactFaces.size == 1,
                    "A plain stone plate did not report one supporting face: $result at ${ship.transform.positionInWorld} by $base")
                val load = result.contactFaces.single().load
                helper.assertTrue(load in ship.inertiaData.mass * 4.0..ship.inertiaData.mass * 16.0,
                    "Expected a standing load close to the ship's weight: $load")
            }
            var steadyAt = 0L
            helper.startSequence().thenWaitUntil { detail() }.thenExecute {
                ship.isStatic = false
            }.thenWaitUntil { planted(detail()) }.thenExecute {
                steadyAt = level.gameTime + 50
            }.thenWaitUntil {
                val result = detail()
                helper.assertTrue(level.gameTime >= steadyAt, "Waiting for the plate to settle while inspected")
                planted(result)
            }.thenExecute {
                ship.isStatic = true
                val lifted = Vector3d(ship.transform.positionInWorld).add(0.0, 6.0, 0.0)
                level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(lifted, ship.transform.shipToWorldRotation,
                    Vector3d(), Vector3d()))
            }.thenWaitUntil {
                val result = detail()
                helper.assertTrue(result.outline.isEmpty() && result.contactFaces.isEmpty(), "Lifting the plate left a stale support face")
            }.thenSucceed()
        }
    }
}
