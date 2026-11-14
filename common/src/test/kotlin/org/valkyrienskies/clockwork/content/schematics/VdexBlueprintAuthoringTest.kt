package org.valkyrienskies.clockwork.content.schematics

import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.mod.common.schematic.VdexData
import org.valkyrienskies.mod.common.schematic.VdexMetadata
import org.valkyrienskies.mod.common.schematic.VdexModEntry
import org.valkyrienskies.mod.common.schematic.VdexShipEntry
import org.valkyrienskies.mod.common.schematic.VdexSocialMetadata

class VdexBlueprintAuthoringTest {
    @Test
    fun `editing authoring fields preserves ship structures and connection metadata`() {
        val structure = CompoundTag().also { it.putString("ClockworkWandLinks", "saved connections") }
        val metadata = VdexMetadata(social = VdexSocialMetadata("Original", "Old description", "Builder"),
            ships = listOf(VdexShipEntry()), shipIdToIndex = mapOf(12L to 0))
        val source = VdexData(metadata, listOf(VdexModEntry("vs_clockwork", "test", true)), mapOf("ship_0.nbt" to structure))
        val edited = VdexBlueprintAuthoring("  Crane  ", "  Builder & Friend  ", "First line\nSecond line").apply(source)
        assertEquals(VdexSocialMetadata("Crane", "First line\nSecond line", "Builder & Friend"), edited.metadata.social)
        assertSame(source.nbtData, edited.nbtData)
        assertSame(source.modList, edited.modList)
        assertSame(source.metadata.ships, edited.metadata.ships)
        assertSame(source.metadata.constraints, edited.metadata.constraints)
        assertSame(source.metadata.shipIdToIndex, edited.metadata.shipIdToIndex)
        assertEquals("Original", source.metadata.social.name)
    }

    @Test
    fun `blank excessive and control text cannot be saved`() {
        val source = VdexData(VdexMetadata(), emptyList(), emptyMap())
        for (fields in listOf(
            VdexBlueprintAuthoring(" ", "Author", ""),
            VdexBlueprintAuthoring("Ship", " ", ""),
            VdexBlueprintAuthoring("x".repeat(81), "Author", ""),
            VdexBlueprintAuthoring("Ship", "x".repeat(81), ""),
            VdexBlueprintAuthoring("Ship", "Author", "x".repeat(2049)),
            VdexBlueprintAuthoring("Ship\nName", "Author", ""),
            VdexBlueprintAuthoring("Ship", "Author", "\u0000"),
            VdexBlueprintAuthoring("Ship", "\u00a7aAuthor", "")
        )) assertThrows(IllegalArgumentException::class.java) { fields.apply(source) }
    }

    @Test
    fun `save packet preserves Unicode names and multiline descriptions`() {
        val fields = VdexBlueprintAuthoring("飛行船", "Élodie", "Line one\nLine two")
        val buffer = FriendlyByteBuf(Unpooled.buffer())
        try {
            VdexBlueprintSavePacket("a".repeat(64), true, fields).write(buffer)
            val decoded = VdexBlueprintSavePacket(buffer)
            assertTrue(decoded.save)
            assertEquals("a".repeat(64), decoded.id)
            assertEquals(fields, decoded.authoring)
            assertFalse(buffer.isReadable)
        } finally { buffer.release() }
    }

    @Test
    fun `preview downloads remain distinguishable from saved exports`() {
        for (preview in listOf(true, false)) {
            val buffer = FriendlyByteBuf(Unpooled.buffer())
            try {
                VdexBlueprintDownloadPacket("b".repeat(64), "Ship", 0, 3, byteArrayOf(1, 2, 3), preview).write(buffer)
                val decoded = VdexBlueprintDownloadPacket(buffer)
                assertEquals(preview, decoded.preview)
                assertArrayEquals(byteArrayOf(1, 2, 3), decoded.bytes)
                assertFalse(buffer.isReadable)
            } finally { buffer.release() }
        }
    }
}
