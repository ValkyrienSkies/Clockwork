package org.valkyrienskies.clockwork.compat.thinkable

import io.mockk.mockk
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.justRun
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.api.thinkable.Thinkables
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace

class ThinkableProviderRegistryTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test fun entityProviderTakesPriorityAndNullFallsBackToBlock() {
        val registry = ThinkableProviderRegistry()
        val state = Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
        val entity = BlockEntityType.DAYLIGHT_DETECTOR.create(BlockPos.ZERO, state)
        val context = ThinkableContext(mockk<Level>(), BlockPos.ZERO, state, entity)
        var entityOutput: Thinkable? = output(0.125)
        registry.registerBlock(Blocks.DAYLIGHT_DETECTOR) { output(-7.75) }
        registry.registerBlockEntity(BlockEntityType.DAYLIGHT_DETECTOR) { _, _ -> entityOutput }
        assertEquals(0.125, registry.find(context)!!.thinkableValue(Direction.UP))
        entityOutput = null
        assertEquals(-7.75, registry.find(context)!!.thinkableValue(Direction.UP))
    }

    @Test fun duplicateRegistrationDoesNotReplaceFirstProvider() {
        val registry = ThinkableProviderRegistry()
        registry.registerBlock(Blocks.STONE) { output(1.25) }
        assertThrows(IllegalStateException::class.java) { registry.registerBlock(Blocks.STONE) { output(9.75) } }
        val context = ThinkableContext(mockk<Level>(), BlockPos.ZERO, Blocks.STONE.defaultBlockState(), null)
        assertEquals(1.25, registry.find(context)!!.thinkableValue(Direction.UP))
        registry.registerBlockEntity(BlockEntityType.DAYLIGHT_DETECTOR) { _, _ -> output(1.0) }
        assertThrows(IllegalStateException::class.java) {
            registry.registerBlockEntity(BlockEntityType.DAYLIGHT_DETECTOR) { _, _ -> output(2.0) }
        }
    }

    @Test fun registryFreezesAndContextCopiesMutablePosition() {
        val registry = ThinkableProviderRegistry()
        registry.freeze()
        assertThrows(IllegalStateException::class.java) { registry.registerBlock(Blocks.STONE) { output(1.0) } }
        val pos = BlockPos.MutableBlockPos(1, 2, 3)
        val context = ThinkableContext(mockk<Level>(), pos, Blocks.STONE.defaultBlockState(), null)
        pos.set(4, 5, 6)
        assertEquals(BlockPos(1, 2, 3), context.pos())
    }

    @Test fun clientLookupExposesPortsWithoutCallingServerCallbacks() {
        mockkObject(CreateThinkableProviders)
        justRun { CreateThinkableProviders.register(any()) }
        ThinkableCompat.initialize { registry ->
            registry.registerBlock(Blocks.MOSS_BLOCK) {
                object : Thinkable {
                    override fun providesThinkable(side: Direction) = side == Direction.UP
                    override fun acceptsThinkable(side: Direction) = side == Direction.DOWN
                    override fun thinkableValue(side: Direction): Double? = throw AssertionError("Client read server data")
                    override fun thinkableValue(side: Direction, reading: String): Double? = throw AssertionError("Client read named server data")
                    override fun thinkableReadings(side: Direction) = listOf(ThinkableReading("precise", Component.literal("Precise")))
                    override fun receiveThinkable(side: Direction, value: Double?) { throw AssertionError("Client wrote server data") }
                }
            }
        }
        unmockkObject(CreateThinkableProviders)
        val level = mockk<Level>()
        every { level.hasChunkAt(BlockPos.ZERO) } returns true
        every { level.getBlockState(BlockPos.ZERO) } returns Blocks.MOSS_BLOCK.defaultBlockState()
        every { level.getBlockEntity(BlockPos.ZERO) } returns null
        val device = Thinkables.find(level, BlockPos.ZERO)!!
        assertTrue(device.providesThinkable(Direction.UP))
        assertNotNull(device.thinkablePortName(Direction.UP))
        assertNull(device.thinkableValue(Direction.UP))
        assertNull(device.thinkableValue(Direction.UP, "precise"))
        assertEquals("precise", device.thinkableReadings(Direction.UP).single().id())
        device.receiveThinkable(Direction.DOWN, 1.25)
        every { level.getBlockState(BlockPos.ZERO) } returns Blocks.STONE.defaultBlockState()
        assertFalse(device.providesThinkable(Direction.UP))
    }

    @Test fun legacyProvidersKeepDefaultAndRejectUnknownReadings() {
        val device = output(0.125)
        assertEquals(0.125, device.thinkableValue(Direction.UP, ""))
        assertNull(device.thinkableValue(Direction.UP, "other"))
        assertTrue(device.thinkableReadings(Direction.UP).isEmpty())
    }

    @Test fun readingSelectionSurvivesProgramRoundTripAndOldProgramsDefault() {
        val program = CombobulatorProgram.controller().copy(channels = listOf(
            CombobulatorChannel(0, "A", true, face = CombobulatorFace.TOP, device = true, reading = "addon:pressure")))
        val tag = program.write()
        assertEquals("addon:pressure", CombobulatorProgram.read(tag).channels.first().reading)
        tag.getList("Channels", 10).getCompound(0).remove("Reading")
        assertEquals("", CombobulatorProgram.read(tag).channels.first().reading)
    }

    private fun output(value: Double) = object : Thinkable {
        override fun thinkableValue(side: Direction) = value
    }
}
