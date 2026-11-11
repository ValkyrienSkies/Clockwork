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
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantRemovePacket
import org.valkyrienskies.clockwork.ClockworkItems
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.core.api.physics.blockstates.FluidBoundarySolidState
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType
import org.valkyrienskies.core.internal.world.chunks.VsiTerrainUpdate
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
import java.math.BigDecimal
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
            players.forEach { level.removeTestPlayer(it) }; players.clear()
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
            sources.setAmount(level.dimensionId, pos.x, pos.y, pos.z, "minecraft:water", BigDecimal(97))
            val section = level.getChunkAt(pos).getSection(level.getSectionIndex(pos.y))
            val chunk = Vector3i(pos.x shr 4, pos.y shr 4, pos.z shr 4)
            fun expected(coated: Boolean): VsiTerrainUpdate {
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

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun leftClickRemovesAreaWithoutBreakingBlocks(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val data = LubricantData.get(level)
            val selected = listOf(pos, pos.east())
            val adjacent = listOf(pos.east(2), pos.east(3))
            for (block in selected + adjacent) {
                level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState())
                data.set(block, Blocks.STONE, true)
            }
            data.addArea(selected)
            data.addArea(adjacent)
            val player = testPlayer(level, pos)
            player.moveTo(pos.x + 0.5, pos.y + 0.5 - player.eyeHeight, pos.z - 2.5, 0f, 0f)
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(ClockworkItems.LUBRICANT.get()))
            player.mainHandItem.orCreateTag.putLong("Start", pos.asLong())
            LubricantRemovePacket.remove(player)
            helper.assertTrue(!player.mainHandItem.orCreateTag.contains("Start"), "Left-click did not cancel the pending selection")
            helper.assertTrue(data.contains(pos.asLong(), Blocks.STONE), "Canceling a selection removed an area")
            LubricantRemovePacket.remove(player)
            for (block in selected) {
                helper.assertTrue(!data.contains(block.asLong(), Blocks.STONE), "Left-click left part of the selected area coated")
                helper.assertTrue(level.getBlockState(block).block === Blocks.STONE, "Left-click broke the underlying block")
            }
            for (block in adjacent) helper.assertTrue(data.contains(block.asLong(), Blocks.STONE), "Left-click removed an adjacent application")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun overlappingAreasPersistAndRemoveIndependently(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val data = LubricantData()
            for (block in listOf(pos, pos.east(), pos.east(2))) {
                level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState())
                data.set(block, Blocks.STONE, true)
            }
            data.addArea(listOf(pos, pos.east()))
            data.addArea(listOf(pos.east(), pos.east(2)))
            val restored = LubricantData.load(data.save(CompoundTag()))
            helper.assertTrue(restored.removeArea(level, pos.east(2)) { it != pos.east() } == 0, "Removal bypassed a protected block")
            helper.assertTrue(restored.removeArea(level, pos.east(2)) { true } == 2, "Saved area boundaries were lost")
            helper.assertTrue(restored.contains(pos.east().asLong(), Blocks.STONE), "Removing overlap erased the other area's coating")
            helper.assertTrue(!restored.contains(pos.east(2).asLong(), Blocks.STONE), "Selected area's exclusive coating survived removal")
            helper.assertTrue(restored.removeArea(level, pos) { true } == 2, "The remaining area could not be removed")
            helper.assertTrue(!restored.contains(pos.east().asLong(), Blocks.STONE), "Overlap stayed coated after both areas were removed")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 40)
        fun olderCoatingsRemoveAsConnectedPatches(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val data = LubricantData()
            for (block in listOf(pos, pos.east(), pos.east(3))) {
                level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState())
                data.set(block, Blocks.STONE, true)
            }
            val tag = data.save(CompoundTag()).apply { remove("Areas"); remove("NextArea") }
            val restored = LubricantData.load(tag)
            helper.assertTrue(restored.removeArea(level, pos) { true } == 2, "Legacy connected patch was not removed")
            helper.assertTrue(restored.contains(pos.east(3).asLong(), Blocks.STONE), "Legacy removal crossed an uncoated gap")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "lubricant", timeoutTicks = 160)
        fun coatedSurfacesSlideUnderLoadsThatNormalSurfacesHold(helper: GameTestHelper) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 180, it.z) }
            testPlayer(level, base.above(5))
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
            data.addArea(blocks)
            val assembled = ShipAssembler.batchAssembleToShips(level, listOf(blocks.map { it.mutable() }.toSet()), 1.0).single()
            assembled.ship.isStatic = true
            ships.add(assembled.ship)
            val delta = Vector3d(assembled.toCenter).sub(assembled.fromCenter)
            val moved = blocks.map { it.offset(kotlin.math.round(delta.x).toInt(), kotlin.math.round(delta.y).toInt(), kotlin.math.round(delta.z).toInt()) }
            helper.startSequence().thenIdle(5).thenExecute {
                for (pos in blocks) helper.assertTrue(!data.contains(pos.asLong(), Blocks.STONE), "Coating remained at assembly origin")
                for (pos in moved) helper.assertTrue(data.contains(pos.asLong(), Blocks.STONE), "Assembly lost a coated block")
                helper.assertTrue(data.areaAt(moved.first()) == moved.map { it.asLong() }.toSet(), "Assembly lost the application boundary")
                val destination = base.offset(0, 6, 0)
                val moves = moved.associateWith { destination.offset(it.subtract(moved.first())) }
                helper.assertTrue(PhysBearingAssembler.moveBlocksWithTransform(level, moves, assembled.ship, null,
                    moved.first(), destination, emptyList()), "Weld failed")
                helper.assertTrue(data.areaAt(destination) == moves.values.map { it.asLong() }.toSet(), "Weld lost the application boundary")
                for ((from, to) in moves) {
                    helper.assertTrue(!data.contains(from.asLong(), Blocks.STONE), "Weld left old coating behind")
                    helper.assertTrue(data.contains(to.asLong(), Blocks.STONE), "Weld lost coating")
                    data.set(to, Blocks.STONE, false)
                    LubricantMaterials.refresh(level, to, false)
                }
            }.thenSucceed()
        }

        private fun testPlayer(level: ServerLevel, pos: BlockPos): ServerPlayer {
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            return ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "lubricant-test")).also {
                level.server.playerList.placeNewPlayer(connection, it)
                it.setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                it.setNoGravity(true); it.abilities.flying = true
                players.add(it)
            }
        }
    }
}
