package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkPackets
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque

object VdexBlueprintClient {
    private data class Download(val total: Int, val bytes: ByteArrayOutputStream)
    private val downloads = HashMap<String, Download>()
    private val outgoing = ArrayDeque<VdexBlueprintUploadPacket>()
    private val requested = HashSet<String>()
    private val cache = object : LinkedHashMap<String, VdexBlueprintData>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, VdexBlueprintData>?) = size > 2
    }
    private var lastLevel: Any? = null
    var status: Component = Component.empty()
        private set

    fun directory(): Path = Minecraft.getInstance().gameDirectory.toPath().resolve("schematics/vdex").also { Files.createDirectories(it) }
    private fun cachedFile(id: String) = directory().resolve(".cache").also { Files.createDirectories(it) }.resolve("$id.vdex")

    fun open() { Minecraft.getInstance().setScreen(VdexBlueprintScreen()) }

    fun notice(message: Component) {
        status = message
        Minecraft.getInstance().player?.displayClientMessage(message, true)
    }

    fun get(id: String): VdexBlueprintData? {
        if (!VdexBlueprintData.validId(id)) return null
        cache[id]?.let { return it }
        if (!requested.add(id)) return null
        try {
            val file = cachedFile(id)
            if (Files.exists(file) && VdexBlueprintData.hash(Files.readAllBytes(file)) == id) {
                return VdexBlueprintData.read(file).also { cache[id] = it; requested.remove(id) }
            }
            ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.DOWNLOAD, id))
        } catch (e: Exception) { notice(Component.literal(e.message ?: "Could not read blueprint")) }
        return null
    }

    fun import(file: Path) {
        try {
            val blueprint = VdexBlueprintData.read(file)
            val bytes = Files.readAllBytes(file)
            val id = VdexBlueprintData.hash(bytes)
            Files.write(cachedFile(id), bytes)
            cache[id] = blueprint
            requested.remove(id)
            outgoing.clear()
            val name = file.fileName.toString().removeSuffix(".vdex").take(80)
            for (offset in bytes.indices step VdexBlueprintData.CHUNK_BYTES) outgoing.add(VdexBlueprintUploadPacket(id, name, offset, bytes.size,
                bytes.copyOfRange(offset, minOf(bytes.size, offset + VdexBlueprintData.CHUNK_BYTES))))
            status = Component.translatable("gui.vs_clockwork.blueprint.uploading")
        } catch (e: Exception) { notice(Component.literal(e.message ?: "Could not import blueprint")) }
    }

    fun accept(packet: VdexBlueprintDownloadPacket) {
        try {
            require(VdexBlueprintData.validId(packet.id) && packet.total in 1..VdexBlueprintData.MAX_FILE_BYTES)
            require(packet.bytes.size in 1..VdexBlueprintData.CHUNK_BYTES && packet.offset in 0 until packet.total)
            if (packet.offset == 0) {
                require(downloads.size < 4 || downloads.containsKey(packet.id))
                downloads[packet.id] = Download(packet.total, ByteArrayOutputStream())
            }
            val download = downloads[packet.id] ?: return
            require(download.total == packet.total && download.bytes.size() == packet.offset && packet.offset + packet.bytes.size <= packet.total)
            download.bytes.write(packet.bytes)
            if (download.bytes.size() != packet.total) return
            downloads.remove(packet.id)
            val bytes = download.bytes.toByteArray()
            require(VdexBlueprintData.hash(bytes) == packet.id)
            VdexBlueprintData.validateArchive(bytes)
            val file = cachedFile(packet.id)
            Files.write(file, bytes)
            val blueprint = VdexBlueprintData.read(file)
            cache[packet.id] = blueprint
            requested.remove(packet.id)
            val name = packet.name.replace(Regex("[^a-zA-Z0-9 _.-]"), "_").trim(' ', '.').take(64).ifBlank { "Ship" }
            val export = directory().resolve("$name-${packet.id.take(8)}.vdex")
            if (!Files.exists(export)) Files.write(export, bytes)
        } catch (e: Exception) {
            downloads.remove(packet.id)
            notice(Component.literal(e.message ?: "Could not download blueprint"))
        }
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        if (lastLevel !== mc.level) {
            lastLevel = mc.level
            downloads.clear(); outgoing.clear(); requested.clear()
            VdexBlueprintPreview.clear()
        }
        VdexBlueprintPreview.tick()
        val stack = mc.player?.mainHandItem
        if (stack?.item !is VdexBlueprintItem) { outgoing.clear(); return }
        repeat(2) { if (outgoing.isNotEmpty()) ClockworkPackets.sendToServer(outgoing.removeFirst()) }
        get(stack.tag?.getString("VdexFile").orEmpty())
    }
}
