package org.valkyrienskies.clockwork.content.logistics.gas.particles

import com.mojang.brigadier.StringReader
import com.mojang.serialization.JsonOps
import io.netty.buffer.Unpooled
import net.minecraft.SharedConstants
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.Level
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.client.particle.DefaultGasParticlePicker
import org.valkyrienskies.kelvin.impl.registry.GasParticlePickerRegistry
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.abs

class KelvinAirParticleTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `registerParticleOverrides binds kelvin air to KelvinAirParticleData`() {
        val airId = ResourceLocation(KelvinMod.MOD_ID, "air")
        val air = GasType("Air", airId, 1.293, 1.716e-5, 1.005, 0.026)

        val previousPicker = GasParticlePickerRegistry.PARTICLE_PICKERS[airId]
        val previousMapping = GasParticlePickerRegistry.GAS_PARTICLE_PICKERS[air]
        mockkObject(GasTypeRegistry)
        try {
            every { GasTypeRegistry.getGasType(KelvinMod.MOD_ID, "air") } returns air

            ClockworkGasses.registerParticleOverrides()

            val picker = GasParticlePickerRegistry.getParticlePicker(air)
            assertNotNull(picker)
            assertInstanceOf(DefaultGasParticlePicker::class.java, picker)

            val level = mockk<Level>(relaxed = true)
            val options = picker!!.chooseParticleOptions(level, DuctNodePos(0.0, 0.0, 0.0))
            assertInstanceOf(KelvinAirParticleData::class.java, options)
        } finally {
            unmockkObject(GasTypeRegistry)

            if (previousPicker == null) GasParticlePickerRegistry.PARTICLE_PICKERS.remove(airId)
            else GasParticlePickerRegistry.PARTICLE_PICKERS[airId] = previousPicker

            if (previousMapping == null) GasParticlePickerRegistry.GAS_PARTICLE_PICKERS.remove(air)
            else GasParticlePickerRegistry.GAS_PARTICLE_PICKERS[air] = previousMapping
        }
    }

    @Test
    fun `KelvinAirParticleData serializes and deserializes cleanly`() {
        val data = KelvinAirParticleData()
        val particleType = mockk<ParticleType<KelvinAirParticleData>>(relaxed = true)

        val buf = FriendlyByteBuf(Unpooled.buffer())
        data.writeToNetwork(buf)
        val fromNet = KelvinAirParticleData.DESERIALIZER.fromNetwork(particleType, buf)
        assertInstanceOf(KelvinAirParticleData::class.java, fromNet)

        val fromCmd = KelvinAirParticleData.DESERIALIZER.fromCommand(particleType, StringReader(""))
        assertInstanceOf(KelvinAirParticleData::class.java, fromCmd)

        val encoded = KelvinAirParticleData.CODEC.encodeStart(JsonOps.INSTANCE, data).result()
        assertTrue(encoded.isPresent)
        val decoded = KelvinAirParticleData.CODEC.parse(JsonOps.INSTANCE, encoded.get()).result()
        assertTrue(decoded.isPresent)
    }

    @Test
    fun `twirl offset starts at zero and stays bounded within twirl radius`() {
        val radius = 1f / 6f
        val start = KelvinAirParticle.computeTwirlOffset(0f, radius, 42f, Direction.Axis.X)
        assertEquals(0.0, start.length(), 1e-6)

        for (axis in listOf(Direction.Axis.X, Direction.Axis.Z)) {
            for (step in 1..20) {
                val progress = step / 20f
                val offset = KelvinAirParticle.computeTwirlOffset(progress, radius, 90f, axis)
                assertTrue(offset.length() <= radius + 1e-5, "Offset exceeded radius at progress=$progress")
                if (axis == Direction.Axis.X) {
                    assertEquals(0.0, abs(offset.x), 1e-6)
                } else {
                    assertEquals(0.0, abs(offset.z), 1e-6)
                }
            }
        }
    }

    @Test
    fun `alpha matches Create translucent air and fades out cleanly`() {
        assertEquals(0.25f, KelvinAirParticle.computeAlpha(0f), 1e-6f)
        assertEquals(0.25f, KelvinAirParticle.computeAlpha(0.5f), 1e-6f)
        assertTrue(KelvinAirParticle.computeAlpha(0.9f) in 0f..0.25f)
        assertEquals(0f, KelvinAirParticle.computeAlpha(1f), 1e-6f)
    }

    @Test
    fun `air particle asset defines Create generic frames`() {
        val stream = javaClass.classLoader.getResourceAsStream("assets/vs_clockwork/particles/air.json")
        assertNotNull(stream, "assets/vs_clockwork/particles/air.json should exist on classpath")
        val content = stream!!.bufferedReader().readText()
        for (i in 0..7) {
            assertTrue(content.contains("minecraft:generic_$i"), "Missing generic_$i frame")
        }
    }
}
