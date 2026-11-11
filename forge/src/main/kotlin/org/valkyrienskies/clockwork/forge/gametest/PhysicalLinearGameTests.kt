package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.STATE
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
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
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeAssembly
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSPrismaticJoint
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.util.UUID
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class PhysicalLinearGameTests {
    companion object {
        private const val BATCH = "physical_linear"
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val originalShips = mutableSetOf<Long>()
        private val forcedChunks = mutableSetOf<ChunkPos>()
        private val observers = mutableListOf<Pair<ServerPlayer, EmbeddedChannel>>()
        private val fixtureBlocks = mutableSetOf<BlockPos>()
        private val fixtureSprings = mutableListOf<WandLinks.Link>()
        private var synchronizePhysics = false

        @JvmStatic
        @BeforeBatch(batch = BATCH)
        fun prepare(level: ServerLevel) {
            synchronizePhysics = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
            level.shipObjectWorld.allShips.forEach { originalShips.add(it.id) }
        }

        @JvmStatic
        @AfterBatch(batch = BATCH)
        fun cleanUp(level: ServerLevel) {
            fixtureSprings.forEach { WandLinks.get(level).remove(level, it) }
            fixtureSprings.clear()
            fixtureBlocks.forEach { level.removeBlock(it, false) }
            fixtureBlocks.clear()
            level.shipObjectWorld.allShips.filter { it.id !in originalShips }.forEach {
                ShipAssembler.deleteShip(level, it, true, false)
            }
            originalShips.clear()
            forcedChunks.forEach { level.setChunkForced(it.x, it.z, false) }
            forcedChunks.clear()
            observers.forEach { (player, channel) ->
                level.removeTestPlayer(player)
                channel.finishAndReleaseAll()
            }
            observers.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = synchronizePhysics
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 160)
        fun gantryStaysAssembledAndChangesMode(helper: GameTestHelper) {
            val level = helper.level
            val rail = fixturePosition(helper)
            val start = rail.east()
            observe(level, start)
            for (i in -2..4) {
                fixtureBlocks.add(rail.above(i))
                fixtureBlocks.add(start.above(i))
                level.removeBlock(start.above(i), false)
                level.setBlockAndUpdate(rail.above(i), AllBlocks.GANTRY_SHAFT.defaultState.setValue(FACING, Direction.UP))
            }
            level.setBlockAndUpdate(start, ClockworkBlocks.PHYS_GANTRY.get().defaultBlockState()
                .setValue(FACING, Direction.EAST).setValue(AXIS_ALONG_FIRST_COORDINATE, false))
            val before = level.shipObjectWorld.allShips.map { it.id }.toSet()
            use(helper, start, Direction.EAST)
            val ship = level.shipObjectWorld.allShips.single { it.id !in before }
            val box = ship.shipAABB!!
            val carriagePos = BlockPos(box.minX(), box.minY(), box.minZ())
            forceChunk(level, carriagePos)
            val carriage = level.getBlockEntity(carriagePos) as PhysicalGantryBlockEntity
            val motor = motor(level, rail.below(3), Direction.UP)
            var stoppedHeight = 0.0

            helper.startSequence()
                .thenIdle(3)
                .thenExecute { motor.generatedSpeed.setValue(-64) }
                .thenWaitUntil {
                    val joint = level.gtpa.getAllJoints().values.filterIsInstance<VSPrismaticJoint>()
                        .firstOrNull { it.shipId1 == ship.id }
                    helper.assertTrue(joint != null, "Gantry did not create its slide joint (loaded=${level.shipObjectWorld.loadedShips.getById(ship.id) != null}, state=${carriage.saveWithoutMetadata()})")
                    helper.assertTrue(height(level, carriagePos) > start.y + 1.5, "Gantry shaft did not drive the carriage upward (height=${height(level, carriagePos)}, start=${start.y})")
                }
                .thenExecute { motor.generatedSpeed.setValue(0) }
                .thenIdle(5)
                .thenExecute { stoppedHeight = height(level, carriage.blockPos) }
                .thenIdle(10)
                .thenExecute {
                    helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId1 == ship.id && it is VSFixedJoint },
                        "Locked gantry did not hold its position after the shaft stopped")
                    helper.assertTrue(abs(height(level, carriage.blockPos) - stoppedHeight) < .15, "Locked gantry moved after the shaft stopped")
                    carriage.getBehaviour(ScrollValueBehaviour.TYPE).setValue(1)
                }
                .thenWaitUntil {
                    val id = level.getShipManagingPos(carriage.blockPos)!!.id
                    helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId1 == id && it is VSPrismaticJoint },
                        "Free gantry did not create a slide joint")
                    helper.assertTrue(height(level, carriage.blockPos) < stoppedHeight - .5, "Free gantry did not slide under gravity")
                }
                .thenExecute {
                    use(helper, carriage.blockPos, Direction.EAST)
                    helper.assertTrue(carriage.isRemoved, "Right-click did not disassemble the carriage")
                    helper.assertTrue((-2..4).any { level.getBlockState(start.above(it)).let { state ->
                        ClockworkBlocks.PHYS_GANTRY.has(state) && !state.getValue(PhysicalGantryBlock.ASSEMBLED)
                    } }, "Gantry did not return to the track")
                }
                .thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 120)
        fun gantryCompressesSpringUnderDrive(helper: GameTestHelper) {
            val level = helper.level
            val rail = fixturePosition(helper)
            val start = rail.east()
            val stop = start.above(4)
            observe(level, start)
            for (i in -2..3) {
                fixtureBlocks.add(rail.above(i))
                level.setBlockAndUpdate(rail.above(i), AllBlocks.GANTRY_SHAFT.defaultState.setValue(FACING, Direction.UP))
            }
            fixtureBlocks.addAll(listOf(start, stop))
            level.setBlockAndUpdate(stop, Blocks.STONE.defaultBlockState())
            level.setBlockAndUpdate(start, ClockworkBlocks.PHYS_GANTRY.defaultState
                .setValue(FACING, Direction.EAST).setValue(AXIS_ALONG_FIRST_COORDINATE, false))
            val before = level.shipObjectWorld.allShips.map { it.id }.toSet()
            use(helper, start, Direction.EAST)
            val carriage = level.shipObjectWorld.allShips.single { it.id !in before }
            val box = carriage.shipAABB!!
            val carriagePos = BlockPos(box.minX(), box.minY(), box.minZ())
            forceChunk(level, carriagePos)
            val motor = motor(level, rail.below(3), Direction.UP)
            motor.generatedSpeed.setValue(0)
            val moving = WandAnchor.at(level, carriagePos, Direction.UP)
            val fixed = WandAnchor.at(level, stop, Direction.DOWN)
            lateinit var spring: WandLinks.Link

            fun assertCompressed() {
                helper.assertTrue(WandLinks.get(level).links[spring.id] === spring, "Spring detached under compression")
                val distance = moving.world(level)!!.distanceTo(fixed.world(level)!!)
                val compression = spring.appliedLength - distance
                helper.assertTrue(compression >= 0.8, "Driven gantry compressed the spring by only $compression blocks")
            }

            helper.startSequence()
                .thenWaitUntil {
                    helper.assertTrue(level.gtpa.getAllJoints().values.any { it is VSFixedJoint && it.shipId1 == carriage.id },
                        "Gantry did not hold its starting position")
                }
                .thenExecute {
                    val length = moving.world(level)!!.distanceTo(fixed.world(level)!!)
                    val link = WandLinks.get(level).add(level, moving, fixed, rope = true, length = length, physical = true, spring = true)
                    helper.assertTrue(link != null, "Could not attach a spring to the carriage")
                    spring = link!!
                    fixtureSprings.add(spring)
                }
                .thenWaitUntil { helper.assertTrue(spring.jointId != null, "Spring joint did not load") }
                .thenExecute { motor.generatedSpeed.setValue(-64) }
                .thenWaitUntil { assertCompressed() }
                .thenExecuteFor(15) { assertCompressed() }
                .thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 180)
        fun pistonBrakesAndRepairsAnEditedShaft(helper: GameTestHelper) {
            val level = helper.level
            val base = fixturePosition(helper)
            observe(level, base)
            val redstone = base.north()
            val fixture = listOf(base, base.west(), base.below(), base.below(2), base.above(), redstone)
            fixtureBlocks.addAll(fixture)
            fixture.forEach { level.removeBlock(it, false) }
            level.setBlockAndUpdate(base, ClockworkBlocks.PHYS_PISTON.get().defaultBlockState()
                .setValue(FACING, Direction.UP).setValue(AXIS_ALONG_FIRST_COORDINATE, true))
            level.setBlockAndUpdate(base.above(), Blocks.STONE.defaultBlockState())
            for (i in 1..2) level.setBlockAndUpdate(base.below(i), AllBlocks.PISTON_EXTENSION_POLE.defaultState.setValue(FACING, Direction.UP))
            use(helper, base, Direction.NORTH)
            val piston = level.getBlockEntity(base) as PhysicalPistonBlockEntity
            val assembled = piston.saveWithoutMetadata()
            helper.assertTrue(assembled.contains("ShaftOrigin"), "Piston failed to assemble with Create poles")
            val origin = BlockPos.of(assembled.getLong("ShaftOrigin"))
            val shipId = assembled.getLong("ShaftShip")
            helper.assertTrue(assembled.getInt("Tail") == -2 && AllBlocks.PISTON_EXTENSION_POLE.has(level.getBlockState(origin.below(2))),
                "Piston did not copy the full shaft (assembly=$assembled, head=${level.getBlockState(origin)}, first=${level.getBlockState(origin.below())}, second=${level.getBlockState(origin.below(2))})")
            helper.assertTrue(level.isEmptyBlock(base.above()), "Piston left its payload in world blocks")
            forceChunk(level, origin)
            val motor = motor(level, base.west(), Direction.EAST)
            var stoppedHeight = 0.0

            helper.startSequence()
                .thenIdle(3)
                .thenExecute { motor.generatedSpeed.setValue(-64) }
                .thenWaitUntil {
                    val height = height(level, origin) - base.y - .5
                    val joints = level.gtpa.getAllJoints().values.filter { it.shipId1 == shipId }
                    helper.assertTrue(height > 1.8, "Piston did not extend two poles (travel=$height, piston RPM=${piston.speed}, state=${piston.saveWithoutMetadata()}, joints=$joints)")
                }
                .thenExecute {
                    val stack = ItemStack(AllBlocks.PISTON_EXTENSION_POLE.get())
                    use(helper, base, Direction.DOWN, stack)
                    helper.assertTrue(AllBlocks.PISTON_EXTENSION_POLE.has(level.getBlockState(origin.below(3))),
                        "Adding a pole at the assembled housing did not extend the shaft")
                }
                .thenWaitUntil {
                    helper.assertTrue(height(level, origin) > base.y + 3.3, "Piston did not gain travel from the new pole")
                }
                .thenExecute { motor.generatedSpeed.setValue(0) }
                .thenIdle(6)
                .thenExecute { stoppedHeight = height(level, origin) }
                .thenIdle(10)
                .thenExecute {
                    helper.assertTrue(abs(height(level, origin) - stoppedHeight) < .15, "Unpowered piston brake slipped")
                    level.setBlockAndUpdate(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState())
                }
                .thenWaitUntil {
                    helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId1 == shipId && it is VSPrismaticJoint },
                        "Redstone did not release the piston brake")
                    helper.assertTrue(height(level, origin) < stoppedHeight - .5, "Released piston did not slide under gravity")
                }
                .thenExecute {
                    motor.generatedSpeed.setValue(-64)
                }
                .thenWaitUntil {
                    helper.assertTrue(height(level, origin) > base.y + 3.3, "Released piston did not respond to the drive")
                }
                .thenExecute {
                    level.removeBlock(redstone, false)
                    level.destroyBlock(origin.below(), false)
                }
                .thenWaitUntil {
                    helper.assertTrue(ClockworkBlocks.PHYS_PISTON_HEAD.has(level.getBlockState(origin.below(2))),
                        "Piston head did not move to the last connected pole")
                    helper.assertTrue(level.getBlockState(base).getValue(STATE) == PistonState.MOVING,
                        "Breaking a pole detached the connected shaft from the piston")
                    helper.assertTrue(level.isEmptyBlock(origin.above()), "The snapped-off payload stayed on the shaft ship")
                }
                .thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 100)
        fun pistonKeepsItsAlignmentWhenAssembledOnAShip(helper: GameTestHelper) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            fixtureBlocks.add(start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val host = ShipAssembler.assembleToShip(level, setOf(start), 0.8)
            host.isStatic = true
            val box = host.shipAABB!!
            val base = BlockPos(box.minX(), box.minY(), box.minZ()).south()
            forceChunk(level, base)
            forceChunk(level, base.west(2))

            lateinit var origin: BlockPos
            var shaftId = -1L
            fun assertAligned(tolerance: Double) {
                val shaft = level.shipObjectWorld.allShips.getById(shaftId)!!
                val expected = host.transform.shipToWorld.transformPosition(base.center.toJOML())
                val actual = shaft.transform.shipToWorld.transformPosition(origin.center.toJOML())
                helper.assertTrue(actual.distance(expected) < tolerance,
                    "Piston shaft shifted from the housing on assembly (offset=${actual.sub(expected)})")
            }

            helper.startSequence()
                .thenWaitUntil {
                    helper.assertTrue(level.shipObjectWorld.loadedShips.getById(host.id) != null, "Host ship did not load")
                }
                .thenExecute {
                    level.setBlockAndUpdate(base, ClockworkBlocks.PHYS_PISTON.defaultState.setValue(FACING, Direction.EAST))
                    level.setBlockAndUpdate(base.east(), Blocks.IRON_BLOCK.defaultBlockState())
                    for (i in 1..2) level.setBlockAndUpdate(base.west(i), AllBlocks.PISTON_EXTENSION_POLE.defaultState.setValue(FACING, Direction.EAST))
                }
                .thenIdle(5)
                .thenExecute {
                    host.isStatic = false
                    level.shipObjectWorld.teleportShip(host, vsCore.newShipTeleportData(
                        newPos = host.transform.positionInWorld,
                        newRot = Quaterniond().rotateY(0.6),
                        newVel = Vector3d(0.5, 0.0, 0.0),
                        newOmega = Vector3d(0.0, 0.5, 0.0)
                    ))
                }
                .thenIdle(3)
                .thenExecute {
                    val piston = level.getBlockEntity(base) as? PhysicalPistonBlockEntity
                    helper.assertTrue(piston != null, "Missing piston on the host ship")
                    use(helper, base, Direction.NORTH)
                    val data = piston!!.saveWithoutMetadata()
                    helper.assertTrue(data.contains("ShaftOrigin"), "Ship-mounted piston did not assemble")
                    origin = BlockPos.of(data.getLong("ShaftOrigin"))
                    shaftId = data.getLong("ShaftShip")
                    forceChunk(level, origin)
                    assertAligned(.01)
                }
                .thenWaitUntil {
                    helper.assertTrue(level.gtpa.getAllJoints().values.any { it is VSFixedJoint && it.shipId0 == host.id && it.shipId1 == shaftId },
                        "Piston did not engage its brake against the host ship")
                }
                .thenIdle(5)
                .thenExecute { assertAligned(.05) }
                .thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 120)
        fun gantryPayloadDoesNotShiftOnAssembly(helper: GameTestHelper) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            val rail = start.west()
            fixtureBlocks.addAll(listOf(rail, start, start.east()))
            level.setBlockAndUpdate(rail, AllBlocks.GANTRY_SHAFT.defaultState.setValue(FACING, Direction.UP))
            level.setBlockAndUpdate(start, ClockworkBlocks.PHYS_GANTRY.defaultState
                .setValue(FACING, Direction.EAST).setValue(AXIS_ALONG_FIRST_COORDINATE, false))
            level.setBlockAndUpdate(start.east(), Blocks.NETHERITE_BLOCK.defaultBlockState())
            val glue = SuperGlueEntity(level, AABB(start).expandTowards(1.0, 0.0, 0.0))
            level.addFreshEntity(glue)
            val before = level.shipObjectWorld.allShips.map { it.id }.toSet()
            use(helper, start, Direction.EAST)
            val ship = level.shipObjectWorld.allShips.single { it.id !in before }
            val box = ship.shipAABB!!
            val carriage = BlockPos(box.minX(), box.minY(), box.minZ())
            forceChunk(level, carriage)
            helper.assertTrue(level.getBlockState(carriage.east()).`is`(Blocks.NETHERITE_BLOCK), "Gantry did not collect its heavy payload")
            fun assertAligned() {
                val actual = ship.transform.shipToWorld.transformPosition(carriage.center.toJOML())
                helper.assertTrue(actual.distance(start.center.toJOML()) < .02,
                    "Gantry shifted on assembly: ${actual.sub(start.center.toJOML())}")
            }
            assertAligned()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.gtpa.getAllJoints().values.any { it is VSFixedJoint && it.shipId1 == ship.id }, "Gantry did not lock")
            }.thenIdle(5).thenExecute { assertAligned() }.thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 120)
        fun hingePayloadKeepsItsPositionOnARotatedShip(helper: GameTestHelper) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val host = ShipAssembler.assembleToShip(level, setOf(start), 0.8)
            host.isStatic = true
            val box = host.shipAABB!!
            val base = BlockPos(box.minX(), box.minY(), box.minZ()).east(2)
            forceChunk(level, base)
            level.setBlockAndUpdate(base, ClockworkBlocks.HINGE.defaultState.setValue(HingeBlock.FACING, Direction.EAST))
            level.setBlockAndUpdate(base.east(), Blocks.NETHERITE_BLOCK.defaultBlockState())
            level.setBlockAndUpdate(base.east().above(), Blocks.OAK_PLANKS.defaultBlockState())
            level.addFreshEntity(SuperGlueEntity(level, AABB(base.east()).expandTowards(0.0, 1.0, 0.0)))
            fixtureBlocks.add(base)
            lateinit var link: HingeLinks.Link
            helper.startSequence().thenIdle(5).thenExecute {
                level.shipObjectWorld.teleportShip(host, vsCore.newShipTeleportData(
                    newPos = host.transform.positionInWorld, newRot = Quaterniond().rotateXYZ(.3, .6, .2)))
            }.thenIdle(3).thenExecute {
                helper.assertTrue(HingeAssembly.assemble(level, base) == null, "Ship-mounted hinge did not assemble")
                link = HingeLinks.get(level).at(base)!!
                val child = level.shipObjectWorld.allShips.getById(link.b.shipId)!!
                forceChunk(level, link.b.pos)
                val expected = host.transform.shipToWorld.transformPosition(base.east().center.toJOML())
                val actual = child.transform.shipToWorld.transformPosition(link.b.pos.east().center.toJOML())
                helper.assertTrue(actual.distance(expected) < .001, "Hinge payload shifted on assembly: ${actual.sub(expected)}")
                val pinA = host.transform.shipToWorld.transformPosition(base.center.toJOML())
                val pinB = child.transform.shipToWorld.transformPosition(link.b.pos.center.toJOML())
                helper.assertTrue(pinA.distance(pinB) < .001, "Hinge began with separated pivots")
            }.thenWaitUntil {
                helper.assertTrue(link.jointId != null, "Hinge did not create its joint")
            }.thenIdle(5).thenExecute {
                helper.assertTrue(HingeLinks.worldPoint(level, link.a)!!.distanceTo(HingeLinks.worldPoint(level, link.b)!!) < .05,
                    "Hinge pivots separated after physics began")
                HingeLinks.get(level).remove(level, link)
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 160)
        fun gimbalPreservesExistingShipOrientation(helper: GameTestHelper) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val host = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            host.isStatic = true
            val box = host.shipAABB!!
            val base = BlockPos(box.minX(), box.minY(), box.minZ()).east(2)
            forceChunk(level, base)
            level.setBlockAndUpdate(base, ClockworkBlocks.GIMBAL_BEARING.defaultState.setValue(FACING, Direction.EAST))
            fixtureBlocks.add(base)
            val seed = start.south(8)
            level.setBlockAndUpdate(seed, Blocks.STONE.defaultBlockState())
            val target = ShipAssembler.assembleToShip(level, setOf(seed), 1.0)
            target.isStatic = true
            val targetBox = target.shipAABB!!
            forceChunk(level, BlockPos(targetBox.minX(), targetBox.minY(), targetBox.minZ()))
            val hostRotation = Quaterniond().rotateXYZ(.2, .5, .1)
            val targetRotation = Quaterniond(hostRotation).rotateXYZ(.35, .6, -.2)
            var targetPosition = Vector3d()
            val bearing = level.getBlockEntity(base) as GimbalBearingBlockEntity
            helper.startSequence().thenIdle(5).thenExecute {
                level.shipObjectWorld.teleportShip(host, vsCore.newShipTeleportData(
                    newPos = host.transform.positionInWorld, newRot = hostRotation))
            }.thenIdle(3).thenExecute {
                targetPosition = host.transform.shipToWorld.transformPosition(base.east().center.toJOML())
                level.shipObjectWorld.teleportShip(target, vsCore.newShipTeleportData(newPos = targetPosition, newRot = targetRotation))
            }.thenIdle(3).thenExecute { bearing.assembleNextTick = true }.thenWaitUntil {
                helper.assertTrue(bearing.isRunning, "Gimbal did not attach")
            }.thenExecute {
                helper.assertTrue(bearing.connectedShipId == target.id, "Gimbal attached to the wrong ship")
                helper.assertTrue(abs(target.transform.shipToWorldRotation.dot(targetRotation)) > .999999, "Gimbal rotated the existing ship on attachment")
                helper.assertTrue(target.transform.positionInWorld.distance(targetPosition) < .001, "Gimbal teleported the existing ship on attachment")
                val joint = bearing.joint as VSD6Joint
                val subAnchor = target.transform.shipToWorld.transformPosition(joint.pose0.pos, Vector3d())
                val hostAnchor = host.transform.shipToWorld.transformPosition(joint.pose1.pos, Vector3d())
                helper.assertTrue(subAnchor.distance(hostAnchor) < .001, "Gimbal started with separated anchors")
                val subFrame = Quaterniond(targetRotation).mul(joint.pose0.rot)
                val hostFrame = Quaterniond(hostRotation).mul(joint.pose1.rot)
                helper.assertTrue(abs(subFrame.dot(hostFrame)) > .999999, "Gimbal started with mismatched angular frames")
                val data = bearing.saveWithoutMetadata()
                bearing.load(data)
                val localAxis = bearing.getRenderBearingAxisLocal()
                val actualAxis = targetRotation.transform(localAxis)
                val expectedAxis = hostRotation.transform(Vector3d(1.0, 0.0, 0.0))
                helper.assertTrue(actualAxis.distance(expectedAxis) < .001, "Gimbal lost its attachment orientation on reload")
                level.removeBlock(base, false)
            }.thenSucceed()
        }

        private fun motor(level: ServerLevel, pos: BlockPos, facing: Direction): CreativeMotorBlockEntity {
            fixtureBlocks.add(pos)
            level.setBlockAndUpdate(pos, AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, facing))
            return level.getBlockEntity(pos) as CreativeMotorBlockEntity
        }

        private fun use(helper: GameTestHelper, pos: BlockPos, side: Direction, stack: ItemStack = ItemStack.EMPTY) {
            val player = helper.makeMockPlayer()
            player.setItemInHand(InteractionHand.MAIN_HAND, stack)
            val hit = BlockHitResult(pos.center, side, pos, false)
            helper.level.getBlockState(pos).use(helper.level, player, InteractionHand.MAIN_HAND, hit)
        }

        private fun height(level: ServerLevel, pos: BlockPos): Double {
            val point = pos.center.toJOML()
            level.getShipManagingPos(pos)?.transform?.shipToWorld?.transformPosition(point)
            return point.y
        }

        private fun forceChunk(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) forcedChunks.add(chunk)
        }

        private fun fixturePosition(helper: GameTestHelper): BlockPos {
            val pos = helper.absolutePos(BlockPos(2, 0, 2))
            return BlockPos(pos.x, 220, pos.z)
        }

        private fun observe(level: ServerLevel, pos: BlockPos) {
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) {
                    listener?.onSuccess()
                }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "actuator-test"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(pos.x.toDouble(), pos.y + 10.0, pos.z.toDouble())
            player.setNoGravity(true)
            player.abilities.flying = true
            observers.add(player to channel)
        }
    }
}
