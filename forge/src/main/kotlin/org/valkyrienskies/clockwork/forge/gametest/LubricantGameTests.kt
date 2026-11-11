package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantMaterials
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.core.api.physics.blockstates.FluidBoundarySolidState
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.mod.common.toDenseVoxelUpdate
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.fluid.FractionalFluidSources
import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraftforge.network.NetworkHooks
import org.valkyrienskies.core.impl.config.VSCoreConfig
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class LubricantGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableListOf<ServerShip>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private val chunks = mutableSetOf<ChunkPos>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = "lubricant")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "lubricant")
        fun cleanup(level: ServerLevel) {
            ships.forEach { ship -> level.shipObjectWorld.allShips.getById(ship.id)?.let { level.shipObjectWorld.deleteShip(it) } }
            ships.clear()
            players.forEach { level.server.playerList.remove(it) }; players.clear()
            channels.forEach { it.finishAndReleaseAll() }; channels.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun materialsPreserveEverythingExceptFriction(helper: GameTestHelper) {
            val original = HashSet<VsiBlockType>()
            var minecraftStates = 0
            for (block in BuiltInRegistries.BLOCK) for (state in block.stateDefinition.possibleStates) {
                minecraftStates++
                BlockStateInfo.get(state)?.second?.let(original::add)
            }
            val variants = HashSet<VsiBlockType>()
            for (type in original) {
                val state = vsCore.blockTypes.getState(type) ?: continue
                val coatedType = LubricantMaterials.lubricated(type)
                val coated = vsCore.blockTypes.getState(coatedType)!!
                variants.add(coatedType)
                helper.assertTrue(state.liquidState == coated.liquidState, "Lubrication changed fluid material")
                helper.assertTrue(state.displacementState == coated.displacementState, "Lubrication changed displacement")
                val solid = state.solidState ?: continue
                val slippery = coated.solidState!!
                helper.assertTrue(slippery.friction == minOf(solid.friction, LubricantMaterials.FRICTION), "Wrong friction")
                helper.assertTrue(solid.shape == slippery.shape && solid.elasticity == slippery.elasticity && solid.hardness == slippery.hardness,
                    "Lubrication changed collision shape or other material properties")
                helper.assertTrue((solid as? FluidBoundarySolidState)?.fluidBoundary == (slippery as? FluidBoundarySolidState)?.fluidBoundary,
                    "Lubrication changed water containment")
            }
            ClockworkMod.LOGGER.info("Lubricant material count: {} Minecraft states, {} original physics states, {} additional physics states",
                minecraftStates, original.size, (variants - original).size)
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun persistenceAndBlockReplacement(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(1, 2, 1))
            val bottom = Blocks.OAK_SLAB.defaultBlockState()
            level.setBlockAndUpdate(pos, bottom)
            val data = LubricantData.get(level)
            data.set(pos, bottom.block, true)
            LubricantMaterials.refresh(level, pos, true)
            val restored = LubricantData.load(data.save(CompoundTag()))
            helper.assertTrue(restored.contains(pos.asLong(), bottom.block), "Coating did not survive save/load")
            val top = bottom.setValue(SlabBlock.TYPE, SlabType.TOP)
            level.setBlockAndUpdate(pos, top)
            helper.startSequence().thenIdle(2).thenExecute {
                helper.assertTrue(data.contains(pos.asLong(), top.block), "Blockstate update erased coating")
                val info = LubricantMaterials.at(level, pos.x, pos.y, pos.z, top, BlockStateInfo.get(top))!!
                helper.assertTrue(info.second == LubricantMaterials.lubricated(BlockStateInfo.get(top)!!.second), "Changed slab shape lost low friction")
                level.removeBlock(pos, false)
                level.setBlockAndUpdate(pos, bottom)
            }.thenIdle(2).thenExecute {
                helper.assertTrue(!data.contains(pos.asLong(), bottom.block), "Replacement block inherited old coating")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun chunkRebuildPreservesCoatingAndPartialWater(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(1, 2, 1))
            val state = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true)
            level.setBlockAndUpdate(pos, state)
            val data = LubricantData.get(level)
            data.set(pos, state.block, true)
            val sources = FractionalFluidSources.forLevel(level)!!
            sources.setAmount(level.dimensionId, pos.x, pos.y, pos.z, "minecraft:water", java.math.BigDecimal(97))
            val section = level.getChunkAt(pos).getSection(level.getSectionIndex(pos.y))
            val chunk = Vector3i(pos.x shr 4, pos.y shr 4, pos.z shr 4)
            fun expected(coated: Boolean): org.valkyrienskies.core.internal.world.chunks.VsiTerrainUpdate {
                val builder = vsCore.newDenseTerrainUpdateBuilder(chunk.x, chunk.y, chunk.z)
                for (x in 0..15) for (y in 0..15) for (z in 0..15) {
                    val normal = BlockStateInfo.get(section.getBlockState(x, y, z))!!.second
                    val selected = x == (pos.x and 15) && y == (pos.y and 15) && z == (pos.z and 15)
                    builder.addBlock(x, y, z, if (coated && selected) LubricantMaterials.lubricated(normal) else normal,
                        if (selected) 97 else 255)
                }
                return builder.build()
            }
            helper.assertTrue(section.toDenseVoxelUpdate(chunk, level) == expected(true), "Chunk rebuild changed coating or partial water volume")
            data.set(pos, state.block, false)
            helper.assertTrue(section.toDenseVoxelUpdate(chunk, level) == expected(false), "Removal did not restore the original material")
            level.removeBlock(pos, false)
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 80)
        fun coatingFollowsSmallAssemblyAndWeld(helper: GameTestHelper) = moveAndWeld(helper, 1)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 80)
        fun coatingFollowsTemplateAssemblyAndWeld(helper: GameTestHelper) = moveAndWeld(helper, 9)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 160)
        fun coatedSurfacesSlideUnderLoadsThatNormalSurfacesHold(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 180, it.z) }
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "lubricant-test"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), 185.0, base.z.toDouble())
            player.setNoGravity(true); player.abilities.flying = true
            players.add(player)
            val data = LubricantData.get(level)
            val coatedFloor = mutableListOf<BlockPos>()
            val bodies = (0..1).map { lane ->
                val origin = base.offset(0, 0, lane * 6)
                for (x in -2..10) for (z in -1..2) {
                    val pos = origin.offset(x, 0, z)
                    level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState())
                    if (lane == 1) {
                        data.set(pos, Blocks.STONE, true)
                        LubricantMaterials.refresh(level, pos, true)
                        coatedFloor.add(pos)
                    }
                }
                val payload = (0..1).flatMap { x -> (0..1).map { z -> origin.offset(x, 2, z) } }.toSet()
                payload.forEach {
                    level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState())
                    if (lane == 1) { data.set(it, Blocks.STONE, true); LubricantMaterials.refresh(level, it, true) }
                }
                ShipAssembler.assembleToShip(level, payload.map { it.mutable() }.toSet(), 1.0).also {
                    ships.add(it)
                    it.isStatic = true
                    val bounds = it.shipAABB!!
                    for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4))
                        if (level.setChunkForced(x, z, true)) chunks.add(ChunkPos(x, z))
                }
            }
            var starts = emptyList<Vector3d>()
            val pushing = AtomicBoolean(false)
            helper.startSequence().thenIdle(15).thenExecute {
                bodies.forEach { it.isStatic = false }
            }.thenIdle(30).thenExecute {
                helper.assertTrue(bodies.all { kotlin.math.abs(it.velocity.y()) < 0.2 }, "Test payloads have not settled on the floor")
                starts = bodies.map { Vector3d(it.transform.positionInWorld) }
                pushing.set(true)
                val until = System.nanoTime() + 8_000_000_000L
                val ids = bodies.map { it.id }
                ClockworkMod.physTickOnce(level.dimensionId) { physics, _, again ->
                    if (pushing.get() && System.nanoTime() < until) {
                        for (id in ids) physics.getBodyById(id)?.let { it.applyWorldForce(Vector3d(it.inertiaData.mass * 2.0, 0.0, 0.0)) }
                        again()
                    }
                }
            }.thenIdle(25).thenExecute {
                pushing.set(false)
                val normalTravel = bodies[0].transform.positionInWorld.x() - starts[0].x
                val slipperyTravel = bodies[1].transform.positionInWorld.x() - starts[1].x
                ClockworkMod.LOGGER.info("Lubricant sliding check: normal={} blocks, coated={} blocks", normalTravel, slipperyTravel)
                helper.assertTrue(kotlin.math.abs(normalTravel) < 0.3, "Uncoated control did not hold the test load")
                helper.assertTrue(slipperyTravel > 0.6, "Lubricated surfaces did not slide more easily")
                coatedFloor.forEach { data.set(it, Blocks.STONE, false); LubricantMaterials.refresh(level, it, false) }
            }.thenSucceed()
        }

        private fun moveAndWeld(helper: GameTestHelper, count: Int) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 3, 1))
            val blocks = (0 until count).map { base.offset(it % 3, it / 3, 0) }.toSet()
            val data = LubricantData.get(level)
            for (pos in blocks) {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState())
                data.set(pos, Blocks.STONE, true)
                LubricantMaterials.refresh(level, pos, true)
            }
            val assembled = ShipAssembler.batchAssembleToShips(level, listOf(blocks.map { it.mutable() }.toSet()), 1.0).single()
            assembled.ship.isStatic = true
            ships.add(assembled.ship)
            val delta = Vector3d(assembled.toCenter).sub(assembled.fromCenter)
            val moved = blocks.map { it.offset(kotlin.math.round(delta.x).toInt(), kotlin.math.round(delta.y).toInt(), kotlin.math.round(delta.z).toInt()) }
            helper.startSequence().thenIdle(5).thenExecute {
                for (pos in blocks) helper.assertTrue(!data.contains(pos.asLong(), Blocks.STONE), "Coating remained at assembly origin")
                for (pos in moved) helper.assertTrue(data.contains(pos.asLong(), Blocks.STONE), "Assembly lost a coated block")
                val destination = base.offset(0, 6, 0)
                val moves = moved.associateWith { destination.offset(it.subtract(moved.first())) }
                helper.assertTrue(PhysBearingAssembler.moveBlocksWithTransform(level, moves, assembled.ship, null,
                    moved.first(), destination, emptyList()), "Weld failed")
                for ((from, to) in moves) {
                    helper.assertTrue(!data.contains(from.asLong(), Blocks.STONE), "Weld left old coating behind")
                    helper.assertTrue(data.contains(to.asLong(), Blocks.STONE), "Weld lost coating")
                    data.set(to, Blocks.STONE, false)
                    LubricantMaterials.refresh(level, to, false)
                }
            }.thenSucceed()
        }
    }
}
