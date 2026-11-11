package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
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
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSPrismaticJoint
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
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
            fixtureBlocks.forEach { level.removeBlock(it, false) }
            fixtureBlocks.clear()
            level.shipObjectWorld.allShips.filter { it.id !in originalShips }.forEach {
                ShipAssembler.deleteShip(level, it, true, false)
            }
            originalShips.clear()
            forcedChunks.forEach { level.setChunkForced(it.x, it.z, false) }
            forcedChunks.clear()
            observers.forEach { (player, channel) ->
                level.server.playerList.remove(player)
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
