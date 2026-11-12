package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.content.schematics.SchematicProcessor
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexWrapper
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

object VdexBlueprintLibrary {
    private data class Upload(val id: String, val name: String, val total: Int, val started: Long, val bytes: ByteArrayOutputStream)
    private data class Download(val id: String, val name: String, val bytes: ByteArray, var offset: Int = 0)
    private val downloads = HashMap<UUID, Download>()
    private val uploads = HashMap<UUID, Upload>()
    private val cache = object : LinkedHashMap<Path, VdexBlueprintData>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Path, VdexBlueprintData>?) = size > 4
    }

    fun directory(level: ServerLevel): Path = VdexWrapper.getSchematicDirectory(level.server).resolve("clockwork").also { Files.createDirectories(it) }

    fun path(level: ServerLevel, id: String): Path {
        require(VdexBlueprintData.validId(id)) { "Invalid blueprint identifier" }
        return directory(level).resolve("$id.vdex")
    }

    fun load(level: ServerLevel, id: String): VdexBlueprintData {
        val file = path(level, id)
        return cache.getOrPut(file) { VdexBlueprintData.read(file) }
    }

    fun install(level: ServerLevel, bytes: ByteArray): String {
        VdexBlueprintData.validateArchive(bytes)
        val id = VdexBlueprintData.hash(bytes)
        val destination = path(level, id)
        if (!Files.exists(destination)) {
            Files.write(destination, bytes)
            try { load(level, id) } catch (e: Exception) {
                Files.deleteIfExists(destination)
                throw e
            }
        } else load(level, id)
        return id
    }

    fun capture(player: ServerPlayer, ship: ServerShip, stack: ItemStack) = guarded(player) {
        val level = player.serverLevel()
        require(!VdexConstruction.locked(level.shipObjectWorld.loadedShips.getById(ship.id))) { "Finish construction before saving this ship" }
        val name = ship.slug?.take(80) ?: "Ship"
        val temporaryName = "clockwork-capture-${UUID.randomUUID()}"
        val file = VdexWrapper.getSchematicDirectory(level.server).resolve("$temporaryName.vdex")
        try {
            VdexWrapper.saveShip(level, ship, temporaryName, player.scoreboardName)
            val saved = VdexBlueprintData.read(file).data
            VdexIO.write(file, saved.metadata.copy(social = saved.metadata.social.copy(name = name)), saved.modList, saved.nbtData)
            val id = install(level, Files.readAllBytes(file))
            select(stack, id, name)
            download(player, id)
            player.displayClientMessage(Component.translatable("message.vs_clockwork.blueprint.saved", name), true)
        } finally { Files.deleteIfExists(file) }
    }

    fun select(stack: ItemStack, id: String, name: String) {
        stack.orCreateTag.putString("VdexFile", id)
        stack.orCreateTag.putString("VdexName", name.take(80))
        stack.orCreateTag.remove("Deployed")
        stack.orCreateTag.remove("VdexStopped")
    }

    fun held(player: ServerPlayer): ItemStack? = player.mainHandItem.takeIf { it.item is VdexBlueprintItem && player.mayBuild() && !player.isSpectator }

    fun upload(player: ServerPlayer, packet: VdexBlueprintUploadPacket) = guarded(player) {
        val stack = held(player) ?: return@guarded
        val now = player.serverLevel().gameTime
        uploads.entries.removeIf { now - it.value.started > 1200 }
        require(VdexBlueprintData.validId(packet.id) && packet.total in 1..VdexBlueprintData.MAX_FILE_BYTES &&
            packet.bytes.size in 1..VdexBlueprintData.CHUNK_BYTES && packet.offset in 0 until packet.total) { "Invalid blueprint upload" }
        if (packet.offset == 0) {
            require(uploads.size < 16 || uploads.containsKey(player.uuid)) { "Blueprint transfers are busy" }
            uploads[player.uuid] = Upload(packet.id, packet.name.take(80), packet.total, now, ByteArrayOutputStream())
        }
        val pending = uploads[player.uuid] ?: return@guarded
        require(pending.id == packet.id && pending.total == packet.total && pending.bytes.size() == packet.offset &&
            packet.offset + packet.bytes.size <= packet.total) { "Invalid blueprint upload sequence" }
        pending.bytes.write(packet.bytes)
        if (pending.bytes.size() == pending.total) {
            uploads.remove(player.uuid)
            val bytes = pending.bytes.toByteArray()
            require(VdexBlueprintData.hash(bytes) == pending.id) { "Blueprint checksum mismatch" }
            val id = install(player.serverLevel(), bytes)
            val blueprint = load(player.serverLevel(), id)
            select(stack, id, pending.name.ifBlank { blueprint.data.metadata.social.name })
            player.inventory.setChanged()
            ClockworkPackets.sendTo(VdexBlueprintNoticePacket(Component.translatable("message.vs_clockwork.blueprint.loaded", stack.tag!!.getString("VdexName"))), player)
        }
    }

    fun download(player: ServerPlayer, id: String) = guarded(player) {
        val stack = held(player) ?: return@guarded
        require(stack.tag?.getString("VdexFile") == id) { "Blueprint is no longer held" }
        val bytes = Files.readAllBytes(path(player.serverLevel(), id))
        require(bytes.size <= VdexBlueprintData.MAX_FILE_BYTES)
        downloads[player.uuid] = Download(id, stack.tag!!.getString("VdexName"), bytes)
    }

    fun tick(level: ServerLevel) {
        downloads.keys.removeIf { level.server.playerList.getPlayer(it) == null }
        for (player in level.players()) {
            val transfer = downloads[player.uuid] ?: continue
            repeat(2) {
                if (transfer.offset >= transfer.bytes.size) return@repeat
                val end = minOf(transfer.bytes.size, transfer.offset + VdexBlueprintData.CHUNK_BYTES)
                ClockworkPackets.sendTo(VdexBlueprintDownloadPacket(transfer.id, transfer.name, transfer.offset, transfer.bytes.size,
                    transfer.bytes.copyOfRange(transfer.offset, end)), player)
                transfer.offset = end
            }
            if (transfer.offset == transfer.bytes.size) downloads.remove(player.uuid)
        }
    }

    fun deploy(player: ServerPlayer, stack: ItemStack, position: Vec3, yaw: Float) {
        require(position.x.isFinite() && position.y.isFinite() && position.z.isFinite() && yaw.isFinite() &&
            player.position().distanceToSqr(position) <= 256.0 * 256.0 && player.serverLevel().worldBorder.isWithinBounds(position.x, position.z)) { "Blueprint placement is outside range" }
        load(player.serverLevel(), stack.tag?.getString("VdexFile") ?: "")
        stack.orCreateTag.apply {
            putDouble("VdexX", position.x); putDouble("VdexY", position.y); putDouble("VdexZ", position.z)
            putFloat("VdexYaw", yaw % 360f); putBoolean("Deployed", true)
        }
        player.inventory.setChanged()
    }

    fun paste(player: ServerPlayer, stack: ItemStack) {
        require(player.isCreative && stack.tag?.getBoolean("Deployed") == true) { "Use a schematicannon to build this blueprint in survival" }
        val tag = stack.tag!!
        deploy(player, stack, Vec3(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ")), tag.getFloat("VdexYaw"))
        val level = player.serverLevel()
        val blueprint = load(level, tag.getString("VdexFile")).data
        val structures = blueprint.nbtData.mapValues { (_, structure) ->
            structure.copy().also { copy -> copy.getList("blocks", 10).forEach {
                val block = it as CompoundTag
                block.getCompound("nbt").remove("ClockworkVdex")
            } }
        }
        val name = "clockwork-paste-${UUID.randomUUID()}"
        val temporary = VdexWrapper.getSchematicDirectory(level.server).resolve("$name.vdex")
        try {
            VdexIO.write(temporary, blueprint.metadata, blueprint.modList, structures)
            val error = VdexWrapper.loadShip(level, Vector3d(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ")),
                name, Quaterniond().rotationY(Math.toRadians(tag.getFloat("VdexYaw").toDouble())),
                additionalProcessors = if (player.hasPermissions(2)) emptyList() else listOf(SchematicProcessor.INSTANCE))
            player.displayClientMessage(error ?: Component.translatable("message.vs_clockwork.blueprint.pasted"), true)
        } finally { Files.deleteIfExists(temporary) }
    }

    fun guarded(player: ServerPlayer, action: () -> Unit) {
        try { action() } catch (e: Exception) {
            uploads.remove(player.uuid)
            ClockworkMod.LOGGER.warn("Blueprint operation failed for {}: {}", player.scoreboardName, e.message)
            ClockworkPackets.sendTo(VdexBlueprintNoticePacket(Component.literal(e.message ?: "Could not process blueprint")), player)
        }
    }
}
