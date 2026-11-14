package org.valkyrienskies.clockwork.content.schematics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VdexArchiveTest {
    private fun zip(vararg entries: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> entries.forEach { zip.putNextEntry(ZipEntry(it)); zip.write("{}".toByteArray()); zip.closeEntry() } }
    }.toByteArray()

    @Test fun identifiersAreContentAddressedAndCannotBePaths() {
        val bytes = zip("metadata.json")
        val id = VdexBlueprintData.hash(bytes)
        assertTrue(VdexBlueprintData.validId(id))
        assertEquals(id, VdexBlueprintData.hash(bytes.copyOf()))
        assertFalse(VdexBlueprintData.validId("../$id"))
        assertFalse(VdexBlueprintData.validId(id.uppercase()))
    }

    @Test fun rejectsOversizeMissingMetadataAndUnsafeEntryNames() {
        assertThrows(IllegalArgumentException::class.java) { VdexBlueprintData.validateArchive(ByteArray(VdexBlueprintData.MAX_FILE_BYTES + 1)) }
        assertThrows(IllegalArgumentException::class.java) { VdexBlueprintData.validateArchive(zip("ship.json")) }
        assertThrows(IllegalArgumentException::class.java) { VdexBlueprintData.validateArchive(zip("metadata.json", "../outside.nbt")) }
        assertThrows(IllegalArgumentException::class.java) { VdexBlueprintData.validateArchive(zip("metadata.json", "/outside.nbt")) }
        VdexBlueprintData.validateArchive(zip("metadata.json"))
    }

    @Test fun rejectsExcessiveEntryCounts() {
        val names = listOf("metadata.json") + (0..2048).map { "extra/$it" }
        assertThrows(IllegalArgumentException::class.java) { VdexBlueprintData.validateArchive(zip(*names.toTypedArray())) }
    }
}
