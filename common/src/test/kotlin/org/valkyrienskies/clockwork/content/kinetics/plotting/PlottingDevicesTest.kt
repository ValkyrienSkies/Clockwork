package org.valkyrienskies.clockwork.content.kinetics.plotting

import io.mockk.mockk
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.clockwork.compat.thinkable.ThinkableProviderRegistry

class PlottingDevicesTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test fun registeredProvidersExposeNamedReadingsAndInputsWithoutReadingOrWritingTheWorld() {
        val registry = ThinkableProviderRegistry()
        val state = Blocks.TARGET.defaultBlockState()
        registry.registerBlock(Blocks.TARGET) {
            object : Thinkable {
                override fun providesThinkable(side: Direction) = side == Direction.UP
                override fun acceptsThinkable(side: Direction) = side == Direction.DOWN
                override fun thinkableReadings(side: Direction) = listOf(
                    ThinkableReading("addon:distance", Component.literal("Distance (blocks)")),
                    ThinkableReading("addon:velocity", Component.literal("Velocity (m/s)")))
                override fun thinkableValue(side: Direction): Double = error("Preview must not read values")
                override fun thinkableValue(side: Direction, reading: String): Double = error("Preview must not read values")
                override fun receiveThinkable(side: Direction, value: Double?) { error("Preview must not deliver values") }
            }
        }
        val device = registry.find(ThinkableContext(mockk<Level>(), BlockPos.ZERO, state, null))!!
        val preview = PlottingDevices.describe(state, device)!!
        assertEquals("minecraft:target", preview.id)
        assertEquals(listOf("addon:distance", "addon:velocity"), preview.ports(PlottingKind.INPUT).map { it.reading })
        assertTrue(preview.outputs.all { it.side == Direction.UP })
        assertEquals(Direction.DOWN, preview.ports(PlottingKind.OUTPUT).single().side)
        assertNull(PlottingDevices.describe(state, object : Thinkable {}))
    }

    @Test fun blockEntityProvidersAreDiscoverableThroughTheirValidBlockStates() {
        val registry = ThinkableProviderRegistry()
        registry.registerBlockEntity(BlockEntityType.DAYLIGHT_DETECTOR) { _, _ -> object : Thinkable {} }
        registry.freeze()
        assertTrue(registry.supports(Blocks.DAYLIGHT_DETECTOR.defaultBlockState()))
        assertFalse(registry.supports(Blocks.STONE.defaultBlockState()))
    }

    @Test fun addonDeviceAndReadingIdsRoundTripWithoutTheAddonInstalled() {
        val plan = PlottingPlan(nodes = listOf(PlottingNode(1, 0, 0, PlottingKind.INPUT,
            device = "some_addon:pressure_sensor", deviceSide = Direction.WEST, reading = "some_addon:pressure")))
        assertEquals(plan, PlottingPlan.read(plan.write()))
    }

}
