package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import java.util.UUID

internal class ShipTestObserver(private val level: ServerLevel, pos: BlockPos) : AutoCloseable {
    private val channel = EmbeddedChannel()
    private val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "ship-test"))

    init {
        val connection = object : Connection(PacketFlow.SERVERBOUND) {
            override fun send(packet: Packet<*>, listener: PacketSendListener?) {
                listener?.onSuccess()
            }
        }
        channel.pipeline().addLast("packet_handler", connection)
        channel.pipeline().fireChannelActive()
        NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
        level.server.playerList.placeNewPlayer(connection, player)
        player.setPos(pos.x + 0.5, pos.y + 10.0, pos.z + 0.5)
        player.setNoGravity(true)
        player.abilities.flying = true
    }

    override fun close() {
        level.server.playerList.remove(player)
        channel.finishAndReleaseAll()
    }
}

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class WanderliteGameTests {
    companion object {
        private const val BATCH = "wanderlite"
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableSetOf<Long>()
        private val forcedChunks = mutableSetOf<ChunkPos>()
        private val observers = mutableListOf<ShipTestObserver>()
        private var previousPhysicsSync = false
        private var previousLiftMultiplier = 0.0

        @JvmStatic
        @BeforeBatch(batch = BATCH)
        fun configurePhysics(level: ServerLevel) {
            previousPhysicsSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            previousLiftMultiplier = ClockworkConfig.SERVER.wanderOreForceMultiplier
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
            ClockworkConfig.SERVER.wanderOreForceMultiplier = 2.0
        }

        @JvmStatic
        @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            ships.forEach { id -> level.shipObjectWorld.allShips.getById(id)?.let(level.shipObjectWorld::deleteShip) }
            forcedChunks.forEach { level.setChunkForced(it.x, it.z, false) }
            ships.clear()
            forcedChunks.clear()
            observers.forEach { it.close() }
            observers.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousPhysicsSync
            ClockworkConfig.SERVER.wanderOreForceMultiplier = previousLiftMultiplier
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
        fun liftFollowsTheBlocksThroughAssemblyAndEdits(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 1, 1)).atY(220)
            observers.add(ShipTestObserver(level, base))
            val slabPos = base.east()
            val block = ClockworkBlocks.WANDERLITE_BLOCK.get()
            val slab = ClockworkBlocks.SMOOTH_WANDERLITE_SLAB.defaultState
            level.setBlockAndUpdate(base, block.defaultBlockState())
            level.setBlockAndUpdate(slabPos, slab)

            val assembly = ShipAssembler.assembleToShipFull(level, setOf(base, slabPos))
            val id = assembly.ship.id
            ships.add(id)
            val offset = Vector3d(assembly.toCenter).sub(assembly.fromCenter)
            val shipBase = BlockPos.containing(
                base.x + offset.x + 0.5,
                base.y + offset.y + 0.5,
                base.z + offset.z + 0.5
            )
            val shipSlab = shipBase.east()
            for (chunk in setOf(ChunkPos(shipBase), ChunkPos(shipSlab))) {
                if (level.setChunkForced(chunk.x, chunk.z, true)) forcedChunks.add(chunk)
            }

            val blockKey = shipBase.toJOML()
            val slabKey = shipSlab.toJOML()
            val blockMass = BlockStateInfo.get(block.defaultBlockState())!!.first
            val slabMass = BlockStateInfo.get(slab)!!.first
            val expected = mapOf(blockKey to blockMass, slabKey to slabMass)

            helper.startSequence().thenWaitUntil {
                val ship = level.shipObjectWorld.loadedShips.getById(id)
                    ?: throw GameTestAssertException("Wanderlite ship has not loaded")
                val control = WanderShipControl.getOrCreate(ship)
                helper.assertTrue(control.wanderBlocks == expected,
                    "Assembly did not register both lift blocks at their shipyard positions")
                helper.assertTrue(ship.velocity.y() > 0.0, "The assembled Wanderlite did not lift the ship")
            }.thenExecute {
                val ship = level.shipObjectWorld.loadedShips.getById(id)!!
                val control = WanderShipControl.getOrCreate(ship)
                val staleKey = shipBase.above().toJOML()
                control.wanderBlocks.remove(blockKey)
                control.wanderBlocks[staleKey] = blockMass
                control.onShipLoad()
                control.reconcileBlocks(level, ship)
                helper.assertTrue(control.wanderBlocks == expected,
                    "Reconciliation did not restore missing lift and remove stale lift")

                helper.assertTrue(block.activate(level, shipBase) == InteractionResult.PASS,
                    "Wanderlite on a ship consumed normal block interaction")
                val doubleSlab = slab.setValue(SlabBlock.TYPE, SlabType.DOUBLE)
                level.setBlockAndUpdate(shipSlab, doubleSlab)
                helper.assertTrue(control.wanderBlocks[slabKey] == BlockStateInfo.get(doubleSlab)!!.first,
                    "Changing the slab state removed its lift")
                level.setBlockAndUpdate(shipSlab, Blocks.STONE.defaultBlockState())
                helper.assertTrue(control.wanderBlocks == mapOf(blockKey to blockMass),
                    "Replacing Wanderlite left lift at the old position")
            }.thenSucceed()
        }
    }
}
