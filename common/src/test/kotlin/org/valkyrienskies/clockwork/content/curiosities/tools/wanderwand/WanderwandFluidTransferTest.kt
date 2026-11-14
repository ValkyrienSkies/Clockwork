package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import io.mockk.*
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.server.Bootstrap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.joml.Matrix4d
import org.joml.Vector3d
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.fluid.FractionalFluidSources
import org.valkyrienskies.mod.common.fluid.WorldFluidOutflowManager
import org.valkyrienskies.mod.util.updateBlock
import java.math.BigDecimal

class WanderwandFluidTransferTest {
    companion object {
        private const val DIM = "minecraft:overworld"
        private const val WATER = "minecraft:water"
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    private val level = mockk<ServerLevel>(relaxed = true)
    private val server = mockk<MinecraftServer>(moreInterfaces = arrayOf(IShipObjectWorldServerProvider::class))
    private val ship = mockk<ServerShip>(relaxed = true)
    private val targetShip = mockk<ServerShip>(relaxed = true)
    private val blocks = mutableMapOf<BlockPos, BlockState>()
    private val chunks = mutableMapOf<ChunkPos, LevelChunk>()
    private var sources = FractionalFluidSources()
    private val parcels = WorldFluidOutflowManager(8, 8)

    @BeforeEach fun setup() {
        mockkObject(BlockStateInfo)
        mockkStatic("org.valkyrienskies.mod.common.VSGameUtilsKt")
        mockkStatic("org.valkyrienskies.mod.util.RelocationUtilKt")
        every { level.server } returns server
        ServerLevel::class.java.getDeclaredField("chunkSource").apply { isAccessible = true }.set(level, mockk<net.minecraft.server.level.ServerChunkCache>(relaxed = true))
        ServerLevel::class.java.getDeclaredField("blockTicks").apply { isAccessible = true }.set(level, mockk<net.minecraft.world.ticks.LevelTicks<net.minecraft.world.level.block.Block>>(relaxed = true))
        every { level.dimensionId } returns DIM
        every { level.hasChunkAt(any()) } returns true
        every { level.getShipManagingPos(any<BlockPos>()) } returns ship
        every { (server as IShipObjectWorldServerProvider).fractionalFluidSources } answers { sources }
        every { (server as IShipObjectWorldServerProvider).fluidOutflowManager } returns parcels
        every { ship.transform.scaling } returns Vector3d(1.0)
        every { ship.transform.positionInWorld } returns Vector3d()
        every { ship.shipToWorld } returns Matrix4d()
        every { ship.velocity } returns Vector3d()
        every { ship.angularVelocity } returns Vector3d()
        every { targetShip.transform.scaling } returns Vector3d(1.0)
        every { targetShip.transform.positionInWorld } returns Vector3d()
        every { targetShip.shipToWorld } returns Matrix4d()
        every { targetShip.velocity } returns Vector3d()
        every { targetShip.angularVelocity } returns Vector3d()
        every { level.getBlockState(any()) } answers { blocks[firstArg()] ?: Blocks.AIR.defaultBlockState() }
        every { level.getChunkAt(any()) } answers {
            val pos = ChunkPos(firstArg<BlockPos>())
            chunks.getOrPut(pos) {
                mockk<LevelChunk>(relaxed = true).also { chunk ->
                    every { chunk.level } returns level
                    every { chunk.pos } returns pos
                    every { chunk.setBlockState(any(), any(), any()) } answers {
                        val p = firstArg<BlockPos>().immutable()
                        val state = secondArg<BlockState>()
                        val old = blocks.put(p, state)
                        sources.reconcileFluid(DIM, p.x, p.y, p.z, if (state.fluidState.isEmpty) null else WATER)
                        old
                    }
                }
            }
        }
        every { level.getChunk(any<BlockPos>()) } answers { level.getChunkAt(firstArg()) }
        every { level.getBlockEntity(any()) } returns null
        every { BlockStateInfo.onSourceAmountChanged(any(), any(), any(), any()) } just Runs
        every { updateBlock(level, any(), any(), any()) } just Runs
    }

    @AfterEach fun cleanup() { unmockkAll() }

    private fun seed(pos: BlockPos, amount: BigDecimal) {
        blocks[pos] = Blocks.WATER.defaultBlockState()
        assertTrue(sources.setAmount(DIM, pos.x, pos.y, pos.z, WATER, amount))
    }

    private fun amount(pos: BlockPos): BigDecimal = if (level.getBlockState(pos).fluidState.isEmpty) BigDecimal.ZERO
        else sources.amount(DIM, pos.x, pos.y, pos.z, WATER) ?: BigDecimal(255)

    @Test fun `rotated welds preserve partial and zero source amounts through reload`() {
        val origin = BlockPos(-20000123, 6, 20000037)
        val destination = BlockPos(20000017, 22, -20000043)
        val offsets = listOf(BlockPos.ZERO, BlockPos(1, 0, 0), BlockPos(0, 2, 1))
        val amounts = listOf(BigDecimal.ZERO, BigDecimal("29.125"), BigDecimal("127.000000000000000001"))
        for (rotation in WandGridRotation.all) {
            sources = FractionalFluidSources(); blocks.clear()
            val moves = offsets.associate { origin.offset(it) to destination.offset(rotation.offset(it)) }
            offsets.zip(amounts).forEach { (offset, units) -> seed(origin.offset(offset), units) }
            assertTrue(PhysBearingAssembler.moveBlocksWithTransform(level, moves, ship, targetShip, origin, destination, rotation.steps))
            sources = FractionalFluidSources().also { it.load(sources.save()) }
            sources.recoverTransfers(mapOf(DIM to level), parcels)
            moves.entries.zip(amounts).forEach { (move, units) ->
                assertEquals(0, amount(move.key).compareTo(BigDecimal.ZERO))
                assertEquals(0, amount(move.value).compareTo(units), "Water at ${move.value}")
            }
            assertEquals(0, parcels.save().getList("parcels", 10).size)
        }
    }

    @Test fun `failed weld restores fractional source ownership and leaves no destination water`() {
        val first = BlockPos(1, 5, 1)
        val second = BlockPos(2, 5, 1)
        val destination = BlockPos(33, 5, 1)
        seed(first, BigDecimal("29.125")); seed(second, BigDecimal("0.25"))
        every { level.getBlockEntity(second) } throws IllegalStateException("copy failure")
        val moves = linkedMapOf(first to destination, second to destination.east())
        assertFalse(PhysBearingAssembler.moveBlocksWithTransform(level, moves, ship, targetShip, first, destination, emptyList()))
        sources = FractionalFluidSources().also { it.load(sources.save()) }
        sources.recoverTransfers(mapOf(DIM to level), parcels)
        assertEquals(0, amount(first).compareTo(BigDecimal("29.125")))
        assertEquals(0, amount(second).compareTo(BigDecimal("0.25")))
        moves.values.forEach { assertEquals(0, amount(it).compareTo(BigDecimal.ZERO)) }
        assertEquals(0, parcels.save().getList("parcels", 10).size)
    }

    @Test fun `insufficient transfer capacity leaves both original sources intact`() {
        sources = FractionalFluidSources(40000)
        val first = BlockPos(1, 5, 1)
        val second = BlockPos(2, 5, 1)
        val destination = BlockPos(33, 5, 1)
        seed(first, BigDecimal("29.125")); seed(second, BigDecimal("0.25"))
        val before = sources.save()
        val moves = linkedMapOf(first to destination, second to destination.east())
        assertFalse(PhysBearingAssembler.moveBlocksWithTransform(level, moves, ship, targetShip, first, destination, emptyList()))
        assertEquals(before, sources.save())
        assertEquals(0, amount(first).compareTo(BigDecimal("29.125")))
        assertEquals(0, amount(second).compareTo(BigDecimal("0.25")))
        moves.values.forEach { assertEquals(0, amount(it).compareTo(BigDecimal.ZERO)) }
        assertEquals(0, parcels.save().getList("parcels", 10).size)
    }
}