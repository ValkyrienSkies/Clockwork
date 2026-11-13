package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

internal data class PlottingDocument(val plan: PlottingPlan, val file: Path? = null,
                                     val fingerprint: String? = null, val saved: CompoundTag? = null) {
    val dirty get() = saved == null || saved != plan.write()
}

internal class PlottingPlanFiles(directory: Path) {
    val directory: Path = directory.toAbsolutePath().normalize()
    data class Library(val documents: List<PlottingDocument>, val errors: List<String>)

    fun load(): Library {
        Files.createDirectories(directory)
        val errors = mutableListOf<String>()
        val documents = mutableListOf<PlottingDocument>()
        val paths = Files.list(directory).use { stream -> stream.filter {
            it.fileName.toString().endsWith(".json", ignoreCase = true) && Files.isRegularFile(it, NOFOLLOW_LINKS)
        }.sorted().limit(MAX_DOCUMENTS + 1L).toList() }
        if (paths.size > MAX_DOCUMENTS) errors.add("Only the first $MAX_DOCUMENTS plan files were loaded")
        for (path in paths.take(MAX_DOCUMENTS)) {
            try {
                val bytes = read(path)
                val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
                require(root.get("version")?.asInt == 1) { "Unsupported plan version" }
                val tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, root.getAsJsonObject("plan")) as CompoundTag
                val plan = PlottingPlan.read(tag)
                require(plan.nodes.size == tag.getList("Nodes", 10).size) { "Invalid or oversized circuit" }
                documents.add(PlottingDocument(plan, path, hash(bytes), plan.write()))
            } catch (e: Exception) { errors.add("${path.fileName}: ${e.message ?: e.javaClass.simpleName}") }
        }
        return Library(documents, errors)
    }

    fun save(document: PlottingDocument): PlottingDocument {
        if (document.file != null && !document.dirty) return document
        Files.createDirectories(directory)
        val target = document.file?.let(::checked) ?: directory.resolve(fileName(document.plan.name))
        if (document.file != null) checkUnchanged(document)
        else require(!Files.exists(target, NOFOLLOW_LINKS)) { "Plan file already exists" }
        val root = JsonObject().apply {
            addProperty("version", 1)
            add("plan", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, document.plan.write()))
        }
        val bytes = (GSON.toJson(root) + "\n").toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Plan file is too large" }
        val temporary = Files.createTempFile(directory, ".plotting-", ".tmp")
        try {
            Files.write(temporary, bytes)
            if (document.file != null) checkUnchanged(document)
            try { Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary, target, REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temporary) }
        return PlottingDocument(document.plan, target, hash(bytes), document.plan.write())
    }

    fun trash(document: PlottingDocument) {
        val file = document.file ?: return
        checkUnchanged(document)
        val trash = directory.resolve(".trash")
        require(!Files.isSymbolicLink(trash)) { "Invalid trash folder" }
        Files.createDirectories(trash)
        Files.move(checked(file), trash.resolve("${UUID.randomUUID()}-${file.fileName}"))
    }

    private fun checked(path: Path): Path {
        val normalized = path.toAbsolutePath().normalize()
        require(normalized.parent == directory && !Files.isSymbolicLink(normalized)) { "Invalid plan path" }
        return normalized
    }

    private fun checkUnchanged(document: PlottingDocument) {
        val file = checked(requireNotNull(document.file))
        check(Files.isRegularFile(file, NOFOLLOW_LINKS) && hash(read(file)) == document.fingerprint) {
            "${file.fileName} changed on disk. Reload or duplicate the plan before saving."
        }
    }

    private fun read(path: Path): ByteArray {
        val bytes = Files.newInputStream(checked(path)).use { it.readNBytes(MAX_BYTES + 1) }
        require(bytes.size <= MAX_BYTES) { "Plan file is too large" }
        return bytes
    }

    companion object {
        const val MAX_DOCUMENTS = 128
        private const val MAX_BYTES = 1_000_000
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun fileName(name: String): String {
            val stem = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_-]+"), "-").trim('-').take(40).ifBlank { "plan" }
            return "$stem-${UUID.randomUUID().toString().take(8)}.json"
        }
    }
}
