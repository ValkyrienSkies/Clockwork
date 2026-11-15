package org.valkyrienskies.clockwork.content.curiosities.magnet

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.Bootstrap
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections

class MagnetControlTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    private fun magnet(electromagnetic: Boolean): MagnetBlock {
        mockkObject(BuiltInRegistries.BLOCK)
        try {
            every { BuiltInRegistries.BLOCK.createIntrusiveHolder(any()) } returns mockk(relaxed = true)
            return MagnetBlock(BlockBehaviour.Properties.of(), electromagnetic)
        } finally {
            unmockkObject(BuiltInRegistries.BLOCK)
        }
    }

    @Test fun sneakingReversesBothMagnetsOnEveryPlacementFace() {
        val context = mockk<BlockPlaceContext>()
        val level = mockk<ServerLevel>()
        every { context.level } returns level
        every { context.clickedPos } returns BlockPos.ZERO
        every { level.getBestNeighborSignal(BlockPos.ZERO) } returns 7
        for (electromagnetic in listOf(false, true)) {
            val block = magnet(electromagnetic)
            for (face in Direction.entries) for (sneaking in listOf(false, true)) {
                every { context.clickedFace } returns face
                every { context.isSecondaryUseActive } returns sneaking
                val state = block.getStateForPlacement(context)
                assertEquals(if (sneaking) face.opposite else face, state.getValue(DirectionalBlock.FACING))
                assertEquals(if (electromagnetic) 7 else 0, state.getValue(MagnetBlock.POWER))
            }
        }
    }

    @Test fun blockAdapterExposesElectromagnetInputsAndSchedulesUpdates() {
        val level = mockk<ServerLevel>()
        val block = magnet(true)
        var state = block.defaultBlockState()
        every { level.hasChunkAt(BlockPos.ZERO) } returns true
        every { level.getBlockState(BlockPos.ZERO) } answers { state }
        every { level.getBlockEntity(BlockPos.ZERO) } returns null
        justRun { level.scheduleTick(BlockPos.ZERO, block, 1) }
        val device = ThinkableConnections.device(level, BlockPos.ZERO)!!
        for (side in Direction.entries) {
            assertTrue(device.acceptsThinkable(side))
            assertFalse(device.providesThinkable(side))
            assertNotNull(device.thinkablePortName(side))
        }
        device.receiveThinkable(Direction.UP, 15.0)
        state = state.setValue(MagnetBlock.POWER, 15)
        device.receiveThinkable(Direction.UP, null)
        verify(exactly = 2) { level.scheduleTick(BlockPos.ZERO, block, 1) }
        state = Blocks.AIR.defaultBlockState()
        assertFalse(device.acceptsThinkable(Direction.UP))
        device.receiveThinkable(Direction.UP, 15.0)
        verify(exactly = 2) { level.scheduleTick(BlockPos.ZERO, block, 1) }
        val permanent = magnet(false)
        assertFalse(permanent.acceptsThinkable(permanent.defaultBlockState(), Direction.UP))
    }

    @Test fun thinkablePowerOverridesRedstoneAndDisconnectRestoresIt() {
        val level = mockk<ServerLevel>()
        val block = magnet(true)
        var state = block.defaultBlockState()
        var input: ThinkableConnections.Input? = null
        var secondInput: ThinkableConnections.Input? = null
        every { level.getBestNeighborSignal(BlockPos.ZERO) } returns 11
        every { level.setBlock(BlockPos.ZERO, any(), 2) } answers { state = secondArg(); true }
        justRun { level.scheduleTick(BlockPos.ZERO, block, 1) }
        mockkObject(ThinkableConnections)
        try {
            every { ThinkableConnections.input(level, BlockPos.ZERO, any()) } answers {
                when (thirdArg<Direction>()) {
                    Direction.UP -> input
                    Direction.DOWN -> secondInput
                    else -> null
                }
            }
            fun update(expected: Int) {
                block.tick(state, level, BlockPos.ZERO, mockk())
                assertEquals(expected, state.getValue(MagnetBlock.POWER))
            }
            update(11)
            for ((value, power) in listOf(15.0 to 15, 0.0 to 0, 6.6 to 7, -4.0 to 0, 50.0 to 15,
                Double.NaN to 0, Double.POSITIVE_INFINITY to 0, null to 0)) {
                input = ThinkableConnections.Input(value)
                update(power)
            }
            secondInput = ThinkableConnections.Input(9.0)
            update(9)
            input = null
            update(9)
            secondInput = null
            update(11)
            verify(exactly = 10) { level.scheduleTick(BlockPos.ZERO, block, 1) }
        } finally {
            unmockkObject(ThinkableConnections)
        }
    }
}
