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
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeAssembly
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandGridRotation
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSPrismaticJoint
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.BlockStateInfo
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
                    val reading = carriage.thinkableValue(Direction.UP)
                    helper.assertTrue(reading != null && abs(reading - (height(level, carriage.blockPos) - start.y - .5)) < .05,
                        "Gantry Thinkable did not report actual travel: $reading")
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
                    val reading = piston.thinkableValue(Direction.NORTH)
                    helper.assertTrue(reading != null && abs(reading - (height(level, origin) - base.y - .5)) < .05,
                        "Piston Thinkable did not report actual travel: $reading")
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
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 300)
        fun gluedPistonPreservesBothShipsMass(helper: GameTestHelper) = pistonMassAndGlue(helper, true)

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 300)
        fun heavyGluedPistonSurvivesRepeatedAssembly(helper: GameTestHelper) = pistonMassAndGlue(helper, false)

        private fun pistonMassAndGlue(helper: GameTestHelper, wrapGlue: Boolean) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            fixtureBlocks.add(start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val host = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            host.isStatic = true
            val box = host.shipAABB!!
            val base = BlockPos(box.minX(), box.minY(), box.minZ())
            forceChunk(level, base)
            forceChunk(level, base.west(2))
            val shaftOrigins = mutableSetOf(base)
            var shaftId = -1L
            var expectedMass = 0.0
            val assembledShafts = mutableSetOf<Long>()
            fun assertMass() {
                val shaft = level.shipObjectWorld.allShips.getById(shaftId)!!
                helper.assertTrue(host.inertiaData.mass > 0.0 && host.inertiaData.mass.isFinite(),
                    "Piston housing lost its mass: ${host.inertiaData.mass}")
                helper.assertTrue(shaft.inertiaData.mass > 0.0 && shaft.inertiaData.mass.isFinite(),
                    "Piston shaft has invalid mass: ${shaft.inertiaData.mass}")
                helper.assertTrue(abs(host.inertiaData.mass + shaft.inertiaData.mass - expectedMass) < .01,
                    "Piston assembly changed total mass: host=${host.inertiaData.mass}, shaft=${shaft.inertiaData.mass}, expected=$expectedMass")
            }
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(host.id) != null, "Host did not load")
            }.thenExecute {
                level.setBlockAndUpdate(base, ClockworkBlocks.PHYS_PISTON.defaultState.setValue(FACING, Direction.EAST))
                for (i in 1..2) level.setBlockAndUpdate(base.west(i), AllBlocks.PISTON_EXTENSION_POLE.defaultState.setValue(FACING, Direction.EAST))
                if (wrapGlue) {
                    level.setBlockAndUpdate(base.east(), Blocks.IRON_BLOCK.defaultBlockState())
                    for (i in -2..1) level.setBlockAndUpdate(base.east(i).above(), Blocks.OAK_PLANKS.defaultBlockState())
                    level.addFreshEntity(SuperGlueEntity(level, SuperGlueEntity.span(base.west(2), base.east().above())))
                } else {
                    for (x in 1..2) for (y in 0..1) level.setBlockAndUpdate(base.east(x).above(y), Blocks.NETHERITE_BLOCK.defaultBlockState())
                    level.addFreshEntity(SuperGlueEntity(level, SuperGlueEntity.span(base.east(), base.east(2).above())))
                }
            }.thenIdle(5).thenExecute {
                val piston = level.getBlockEntity(base) as PhysicalPistonBlockEntity
                expectedMass = host.inertiaData.mass + BlockStateInfo.get(ClockworkBlocks.PHYS_PISTON_HEAD.defaultState)!!.first
                host.isStatic = false
                use(helper, base, Direction.NORTH)
                val data = piston.saveWithoutMetadata()
                helper.assertTrue(data.contains("ShaftOrigin"), "Glued ship-mounted piston did not assemble")
                shaftId = data.getLong("ShaftShip")
                shaftOrigins.add(BlockPos.of(data.getLong("ShaftOrigin")))
                forceChunk(level, BlockPos.of(data.getLong("ShaftOrigin")))
                assertMass()
            }.thenIdle(20).thenExecute {
                assertMass()
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(shaftId) != null, "Shaft did not load")
                val piston = level.getBlockEntity(base) as PhysicalPistonBlockEntity
                piston.speed = -64f
            }.thenWaitUntil {
                val piston = level.getBlockEntity(base) as PhysicalPistonBlockEntity
                piston.speed = -64f
                helper.assertTrue((piston.thinkableValue(Direction.NORTH) ?: 0.0) > 1.8, "Piston did not extend (travel=${piston.thinkableValue(Direction.NORTH)}, speed=${piston.speed}, data=${piston.saveWithoutMetadata()}, hostMass=${host.inertiaData.mass}, shaftMass=${level.shipObjectWorld.allShips.getById(shaftId)?.inertiaData?.mass})")
            }.thenExecute {
                assertMass()
                (level.getBlockEntity(base) as PhysicalPistonBlockEntity).speed = 64f
            }.thenWaitUntil {
                val piston = level.getBlockEntity(base) as PhysicalPistonBlockEntity
                piston.speed = 64f
                helper.assertTrue((piston.thinkableValue(Direction.NORTH) ?: 2.0) < .1, "Piston did not retract")
            }.thenExecute {
                assertMass()
                (level.getBlockEntity(base) as PhysicalPistonBlockEntity).speed = 0f
                use(helper, base, Direction.NORTH)
                helper.assertTrue(abs(host.inertiaData.mass - (expectedMass - BlockStateInfo.get(ClockworkBlocks.PHYS_PISTON_HEAD.defaultState)!!.first)) < .01,
                    "Piston disassembly did not return the blocks' mass to the host")
            }.thenExecuteFor(80) {
                repeat(4) {
                    use(helper, base, Direction.NORTH)
                    val data = (level.getBlockEntity(base) as PhysicalPistonBlockEntity).saveWithoutMetadata()
                    if (data.contains("ShaftOrigin")) {
                        assembledShafts.add(data.getLong("ShaftShip"))
                        shaftOrigins.add(BlockPos.of(data.getLong("ShaftOrigin")))
                    }
                    val payload = (if (data.contains("ShaftOrigin")) BlockPos.of(data.getLong("ShaftOrigin")) else base).east()
                    helper.assertTrue(SuperGlueEntity.isGlued(level, payload, Direction.UP, null),
                        "Piston lost its payload glue while toggling")
                }
                val glue = level.allEntities.filterIsInstance<SuperGlueEntity>().filter { entity ->
                    entity.isAlive && shaftOrigins.any { entity.boundingBox.intersects(SuperGlueEntity.span(it.west(2), it.east(2).above())) }
                }
                helper.assertTrue(glue.size <= 2, "Piston assembly duplicated glue entities: ${glue.map { it.boundingBox }}")
                level.shipObjectWorld.allShips.filter { (it.id == host.id || it.id == shaftId || it.id in assembledShafts) && it.shipAABB != null }.forEach {
                    helper.assertTrue(it.inertiaData.mass > 0.0 && it.inertiaData.mass.isFinite(),
                        "Rapid piston toggling left ship ${it.id} with mass ${it.inertiaData.mass} and bounds ${it.shipAABB}")
                }
            }.thenIdle(5).thenExecute {
                helper.assertTrue(assembledShafts.size >= 10, "Piston only assembled ${assembledShafts.size} times during rapid toggling")
                level.shipObjectWorld.allShips.filter { it.id == host.id || it.id == shaftId || it.id in assembledShafts }.forEach {
                    helper.assertTrue(it.inertiaData.mass > 0.0 && it.inertiaData.mass.isFinite(),
                        "Rapid piston toggling left an empty ship ${it.id} with mass ${it.inertiaData.mass}")
                }
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 100)
        fun assemblyAndWeldPreserveGlueWithoutCopies(helper: GameTestHelper) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            val blocks = setOf(start, start.east())
            val remaining = blocks.map { it.above(2) }
            fixtureBlocks.addAll(blocks + remaining)
            (blocks + remaining).forEach { level.setBlockAndUpdate(it, Blocks.IRON_BLOCK.defaultBlockState()) }
            val originalGlue = SuperGlueEntity(level, SuperGlueEntity.span(start, start.east().above(2)))
            level.addFreshEntity(originalGlue)
            val first = ShipAssembler.batchAssembleToShips(level, listOf(blocks), 1.0).single().ship
            first.isStatic = true
            val firstBox = first.shipAABB!!
            val firstPos = BlockPos(firstBox.minX(), firstBox.minY(), firstBox.minZ())
            forceChunk(level, firstPos)
            val rotation = WandGridRotation.all.first { it.face(Direction.EAST) == Direction.UP }
            val destination = start.south(5)
            val finalDestination = start.south(9)
            lateinit var second: ServerShip
            lateinit var secondPos: BlockPos
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(first.id) != null, "Batch-assembled ship did not load")
            }.thenExecute {
                helper.assertTrue(originalGlue.isAlive && SuperGlueEntity.isGlued(level, start.above(2), Direction.EAST, null),
                    "Partial assembly removed glue from the blocks left behind")
                helper.assertTrue(SuperGlueEntity.isGlued(level, firstPos, Direction.EAST, null), "Batch assembly lost payload glue")
                val moves = setOf(firstPos, firstPos.east()).associateWith { destination.offset(rotation.offset(it.subtract(firstPos))) }
                fixtureBlocks.addAll(moves.values)
                helper.assertTrue(PhysBearingAssembler.moveBlocksWithTransform(level, moves, first, null, firstPos, destination, rotation.steps),
                    "Rotated weld failed")
                helper.assertTrue(SuperGlueEntity.isGlued(level, destination, Direction.UP, null), "Weld did not rotate the glue with its blocks")
                second = ShipAssembler.assembleToShipFull(level, moves.values.toSet()).ship
                second.isStatic = true
                val bounds = second.shipAABB!!
                secondPos = BlockPos(bounds.minX(), bounds.minY(), bounds.minZ())
                forceChunk(level, secondPos)
            }.thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(second.id) != null, "Reassembled ship did not load")
            }.thenExecute {
                helper.assertTrue(SuperGlueEntity.isGlued(level, secondPos, Direction.UP, null), "Template assembly lost rotated glue")
                val moved = DenseBlockPosSet().apply {
                    add(secondPos.x, secondPos.y, secondPos.z)
                    add(secondPos.x, secondPos.y + 1, secondPos.z)
                }
                fixtureBlocks.addAll(listOf(finalDestination, finalDestination.above()))
                helper.assertTrue(PhysBearingAssembler.moveBlocksFromTo(level, moved, true, secondPos, finalDestination, second, null),
                    "Bearing disassembly failed")
                helper.assertTrue(SuperGlueEntity.isGlued(level, finalDestination, Direction.UP, null), "Bearing disassembly lost glue")
                val locations = listOf(AABB(start).expandTowards(1.0, 2.0, 0.0), AABB(firstPos).inflate(4.0),
                    AABB(destination).inflate(3.0), AABB(secondPos).inflate(4.0), AABB(finalDestination).inflate(3.0))
                val glue = level.allEntities.filterIsInstance<SuperGlueEntity>().filter { entity ->
                    entity.isAlive && locations.any { it.intersects(entity.boundingBox) }
                }
                helper.assertTrue(glue.size == 2, "Assembly/weld round trip left ${glue.size} glue entities instead of two")
                glue.forEach { it.discard() }
            }.thenSucceed()
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
        fun gimbalAlignsExistingShipOnRotatedHost(helper: GameTestHelper) {
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
            }.thenIdle(3).thenExecute {
                bearing.maxAngleBehaviour!!.setValue(0)
                bearing.assembleNextTick = true
            }.thenWaitUntil {
                helper.assertTrue(bearing.isRunning, "Gimbal did not attach")
            }.thenExecute {
                helper.assertTrue(bearing.connectedShipId == target.id, "Gimbal attached to the wrong ship")
                helper.assertTrue(abs(target.transform.shipToWorldRotation.dot(targetRotation)) > .999999, "Gimbal rotated the existing ship on attachment")
                helper.assertTrue(target.transform.positionInWorld.distance(targetPosition) < .001, "Gimbal teleported the existing ship on attachment")
                target.isStatic = false
            }.thenIdle(35).thenExecute {
                val joint = bearing.joint as VSD6Joint
                val subAnchor = target.transform.shipToWorld.transformPosition(joint.pose0.pos, Vector3d())
                val hostAnchor = host.transform.shipToWorld.transformPosition(joint.pose1.pos, Vector3d())
                helper.assertTrue(subAnchor.distance(hostAnchor) < .01, "Gimbal did not bring the attachment anchors together")
                val subFrame = Quaterniond(target.transform.shipToWorldRotation).mul(joint.pose0.rot)
                val hostFrame = Quaterniond(hostRotation).mul(joint.pose1.rot)
                helper.assertTrue(abs(subFrame.dot(hostFrame)) > .999, "Gimbal did not align its attachment frames")
                val data = bearing.saveWithoutMetadata()
                bearing.load(data)
                val localAxis = bearing.getRenderBearingAxisLocal()
                val actualAxis = target.transform.shipToWorldRotation.transform(localAxis)
                val expectedAxis = hostRotation.transform(Vector3d(1.0, 0.0, 0.0))
                helper.assertTrue(actualAxis.distance(expectedAxis) < .01, "Gimbal lost its attachment orientation on reload")
                level.removeBlock(base, false)
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 360)
        fun extendonConnectsAndChangesLengthWithoutViolentCorrection(helper: GameTestHelper) {
            extendonStability(helper, false)
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 360)
        fun extendonPreservesRotatedShipAlignmentAndRestoresOldJoints(helper: GameTestHelper) {
            extendonStability(helper, true)
        }

        private fun extendonStability(helper: GameTestHelper, shipMounted: Boolean) {
            val level = helper.level
            val start = fixturePosition(helper)
            observe(level, start)
            val top = start.above(8)
            fixtureBlocks.add(top)
            fixtureBlocks.add(start)
            level.setBlockAndUpdate(top, ClockworkBlocks.EXTENDON.defaultState.setValue(FACING, Direction.DOWN))
            val host = if (shipMounted) ShipAssembler.assembleToShip(level, setOf(top), 1.0).also { it.isStatic = true } else null
            val topPos = host?.shipAABB?.let { BlockPos(it.minX(), it.minY(), it.minZ()) } ?: top
            forceChunk(level, topPos)
            level.setBlockAndUpdate(start, ClockworkBlocks.EXTENDON.defaultState.setValue(FACING, Direction.UP))
            level.setBlockAndUpdate(start.below(), Blocks.NETHERITE_BLOCK.defaultBlockState())
            val child = ShipAssembler.assembleToShip(level, setOf(start, start.below()), 1.0)
            child.isStatic = true
            val box = child.shipAABB!!
            val bottomPos = BlockPos(box.minX(), box.maxY() - 1, box.minZ())
            forceChunk(level, bottomPos)
            lateinit var upper: ExtendonBlockEntity
            lateinit var lower: ExtendonBlockEntity
            var initialRotation = Quaterniond()
            var lastLength = 8.0
            var contracted = 0.0
            var restoredLength = 0.0
            fun distance(): Double {
                val a = topPos.center.toJOML()
                host?.transform?.shipToWorld?.transformPosition(a)
                return child.transform.shipToWorld.transformPosition(bottomPos.center.toJOML()).distance(a)
            }
            fun checkMotion() {
                helper.assertTrue(child.velocity.isFinite && child.angularVelocity.isFinite, "Extendon produced non-finite motion")
                helper.assertTrue(child.velocity.length() < 14.0, "Extendon launched its payload: ${child.velocity}")
                helper.assertTrue(child.angularVelocity.length() < 3.0, "Extendon violently rotated its payload: ${child.angularVelocity}")
                val current = upper.distanceJoint!!
                val length = current.minDistance!!.toDouble()
                helper.assertTrue(abs(length - lastLength) <= .201, "Extendon target jumped from $lastLength to $length")
                helper.assertTrue((current.stiffness ?: 0f) > 0 && (current.damping ?: 0f) > 0, "Extendon lost its spring settings")
                lastLength = length
            }
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(child.id) != null, "Extendon payload did not load")
                upper = level.getBlockEntity(topPos) as ExtendonBlockEntity
                lower = level.getBlockEntity(bottomPos) as ExtendonBlockEntity
            }.thenExecute {
                if (shipMounted) level.shipObjectWorld.teleportShip(child, vsCore.newShipTeleportData(
                    newPos = child.transform.positionInWorld, newRot = Quaterniond().rotateY(.8)))
            }.thenIdle(3).thenExecute {
                initialRotation = Quaterniond(child.transform.shipToWorldRotation)
                upper.connectTo(lower)
                val joint = upper.distanceJoint!!
                helper.assertTrue(abs(joint.minDistance!! - distance()) < .01, "Extendon did not start at its actual separation")
                val angular = upper.sphericalJoint!!
                val frame0 = Quaterniond(host?.transform?.shipToWorldRotation ?: Quaterniond()).mul(angular.pose0.rot)
                val frame1 = Quaterniond(child.transform.shipToWorldRotation).mul(angular.pose1.rot)
                helper.assertTrue(abs(frame0.dot(frame1)) > .9999, "Extendon starts with mismatched angular frames")
                lastLength = joint.minDistance!!.toDouble()
                child.isStatic = false
            }.thenExecuteFor(65) {
                checkMotion()
            }.thenExecute {
                contracted = distance()
                helper.assertTrue(contracted < 3.0, "Extendon did not retract: $contracted")
                helper.assertTrue(abs(initialRotation.dot(child.transform.shipToWorldRotation)) > .98,
                    "Extendon forced the connected ship away from its original orientation")
                val kelvin = ClockworkMod.getKelvin(level)
                helper.assertTrue(kelvin.addGasAtTemperature(upper.getDuctNodePosition(), ClockworkGasses.HELIUM, 8.0, 300.0),
                    "Could not supply Extendon gas")
            }.thenExecuteFor(55) {
                checkMotion()
            }.thenExecute {
                helper.assertTrue(distance() > contracted + 2.0, "Extendon did not extend after receiving gas: ${distance()}")
                if (shipMounted) {
                    child.isStatic = true
                    upper.main = false
                    val legacy = upper.distanceJoint!!.copy(minDistance = .5f, maxDistance = 1000f, stiffness = null, damping = 1000f)
                    level.gtpa.updateJoint(upper.distanceJointId!!, legacy)
                }
            }.thenIdle(2).thenExecute {
                val saved = upper.saveWithoutMetadata()
                if (!shipMounted) {
                    saved.remove("DistanceJointId")
                    saved.remove("SphericalJointId")
                }
                upper.connectedJoint = null
                upper.connectedBe = null
                lower.connectedJoint = null
                lower.connectedBe = null
                upper.load(saved)
            }.thenIdle(3).thenExecute {
                helper.assertTrue(upper.connectedBe === lower && upper.distanceJoint!!.stiffness != null,
                    "Extendon failed to restore a damped connection")
                if (shipMounted) helper.assertTrue(abs(upper.distanceJoint!!.minDistance!! - distance()) < .7,
                    "Legacy Extendon did not restart from its actual separation")
                restoredLength = upper.distanceJoint!!.minDistance!!.toDouble()
                lastLength = restoredLength
            }.thenExecuteFor(12) {
                checkMotion()
            }.thenExecute {
                helper.assertTrue(upper.distanceJoint!!.minDistance!! > restoredLength + .5,
                    "Restored Extendon stopped responding to its gas supply")
                val distanceId = upper.distanceJointId!!
                val angularId = upper.sphericalJointId!!
                upper.disconnect()
                helper.runAfterDelay(3) {
                    helper.assertTrue(level.gtpa.getJointById(distanceId) == null && level.gtpa.getJointById(angularId) == null,
                        "Extendon left constraints behind after disconnecting")
                    helper.succeed()
                }
            }
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
