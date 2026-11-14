package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PlottingPlanFilesTest {
    @TempDir lateinit var directory: Path
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test fun emptyFolderHasNoAutomaticExample() {
        val library = PlottingPlanFiles(directory).load()
        assertTrue(library.documents.isEmpty())
        assertTrue(library.errors.isEmpty())
    }

    @Test fun planRoundTripsThroughPortableJsonAndAnotherTableLibrary() {
        val files = PlottingPlanFiles(directory)
        val plan = PlottingPlan.example().copy(name = "Left leg")
        val saved = files.save(PlottingDocument(plan))
        assertFalse(saved.dirty)
        assertTrue(Files.readString(saved.file).contains("\"version\": 1"))
        val loaded = PlottingPlanFiles(directory).load()
        assertEquals(listOf(plan), loaded.documents.map { it.plan })
        assertTrue(loaded.errors.isEmpty())
    }

    @Test fun namesCannotEscapeDirectoryAndDuplicatesGetSeparateFiles() {
        val files = PlottingPlanFiles(directory)
        val first = files.save(PlottingDocument(PlottingPlan("../../CON")))
        val second = files.save(PlottingDocument(first.plan))
        assertEquals(directory.toAbsolutePath(), first.file!!.parent)
        assertNotEquals(first.file, second.file)
        assertEquals(2, files.load().documents.size)
    }

    @Test fun editingRetainsFileIdentityAndLeavesNoTemporaryFiles() {
        val files = PlottingPlanFiles(directory)
        val first = files.save(PlottingDocument(PlottingPlan("One")))
        val second = files.save(first.copy(plan = first.plan.copy(name = "Two")))
        assertEquals(first.file, second.file)
        assertEquals("Two", files.load().documents.single().plan.name)
        Files.list(directory).use { assertEquals(1, it.count()) }
    }

    @Test fun externalEditsAreNotOverwrittenOrDeleted() {
        val files = PlottingPlanFiles(directory)
        val saved = files.save(PlottingDocument(PlottingPlan("One")))
        Files.writeString(saved.file, "external edit")
        assertThrows(IllegalStateException::class.java) { files.save(saved.copy(plan = PlottingPlan("Two"))) }
        assertThrows(IllegalStateException::class.java) { files.trash(saved) }
        assertEquals("external edit", Files.readString(saved.file))
    }

    @Test fun malformedAndFutureFilesRemainIntactAndDoNotHideValidPlans() {
        val files = PlottingPlanFiles(directory)
        files.save(PlottingDocument(PlottingPlan("Valid")))
        Files.writeString(directory.resolve("broken.json"), "invalid")
        Files.writeString(directory.resolve("future.json"), "{\"version\":2,\"plan\":{}}")
        val loaded = files.load()
        assertEquals(1, loaded.documents.size)
        assertEquals(2, loaded.errors.size)
        assertEquals("invalid", Files.readString(directory.resolve("broken.json")))
    }

    @Test fun deletedFilesAreRecoverableFromTrash() {
        val files = PlottingPlanFiles(directory)
        val saved = files.save(PlottingDocument(PlottingPlan.example()))
        val content = Files.readString(saved.file)
        files.trash(saved)
        assertTrue(files.load().documents.isEmpty())
        Files.list(directory.resolve(".trash")).use { assertEquals(content, Files.readString(it.findFirst().orElseThrow())) }
    }
}
