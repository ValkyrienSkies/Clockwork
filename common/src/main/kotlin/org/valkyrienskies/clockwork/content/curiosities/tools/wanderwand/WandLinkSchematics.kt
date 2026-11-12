package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3i
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData
import org.valkyrienskies.clockwork.util.ClockworkShipSchematics
import org.valkyrienskies.clockwork.util.ClockworkContraptionSchematics
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.schematic.VdexData
import org.valkyrienskies.mod.common.schematic.VdexMetadata
import org.valkyrienskies.mod.common.schematic.VdexModEntry
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.yRange
import java.util.UUID

object WandLinkSchematics {
    const val ENTRY = "clockwork_links.nbt"

    @JvmStatic
    fun capture(level: ServerLevel, metadata: VdexMetadata, mods: List<VdexModEntry>, blocks: Map<String, CompoundTag>): VdexData {
        val data = WandLinks.get(level)
        val ids = metadata.ships.mapTo(HashSet()) { it.previousId }
        val links = data.links.values.filter { it.a.shipId in ids && it.b.shipId in ids }
        val hinges = HingeLinks.get(level).links.values.filter { it.a.shipId in ids && it.b.shipId in ids }
        val owned = data.links.values.flatMap { listOfNotNull(it.jointId, it.alignmentId) }.toSet() + hinges.mapNotNull { it.jointId }
        val filtered = metadata.copy(constraints = metadata.constraints.filter { it.previousId !in owned })
        val origins = metadata.ships.mapNotNull { entry ->
            level.shipObjectWorld.allShips.getById(entry.previousId)?.shipAABB?.let {
                entry.previousId to BlockPos(it.minX(), it.minY(), it.minZ())
            }
        }.toMap()
        val lubricant = LubricantData.get(level).captureSchematic(level, origins)
        val attachments = ClockworkShipSchematics.capture(level, origins)
        val capturedBlocks = ClockworkContraptionSchematics.capture(level, metadata, origins, blocks)
        if (links.isEmpty() && hinges.isEmpty() && attachments.isEmpty() && lubricant.getList("Blocks", 10).isEmpty()) return VdexData(filtered, mods, capturedBlocks)
        val payload = encode(links, origins)
        payload.put("lubricant", lubricant)
        payload.put("attachments", attachments)
        payload.put("hinges", ListTag().also { list -> hinges.forEach { list.add(it.save()) } })
        val required = if (mods.any { it.id == ClockworkMod.MOD_ID }) mods else mods + VdexModEntry(
            ClockworkMod.MOD_ID, ValkyrienSkiesMod.platformUtils.getModVersion(ClockworkMod.MOD_ID), true)
        return VdexData(filtered, required, capturedBlocks + (ENTRY to payload))
    }

    internal fun encode(links: Collection<WandLinks.Link>, origins: Map<Long, BlockPos>) = CompoundTag().also { tag ->
        tag.putInt("version", 1)
        tag.put("links", ListTag().also { list -> links.filter { it.a.shipId in origins && it.b.shipId in origins }
            .forEach { list.add(it.save()) } })
        tag.put("origins", ListTag().also { list -> origins.forEach { (id, origin) -> list.add(CompoundTag().also {
            it.putLong("ship", id); it.putLong("pos", origin.asLong())
        }) } })
    }

    internal fun decode(payload: CompoundTag, destinations: Map<Long, Pair<Long, BlockPos>>): List<WandLinks.Link> {
        if (payload.getInt("version") != 1) return emptyList()
        val origins = payload.getList("origins", Tag.TAG_COMPOUND.toInt()).filterIsInstance<CompoundTag>()
            .associate { it.getLong("ship") to BlockPos.of(it.getLong("pos")) }
        fun destination(anchor: WandAnchor): WandLinkTransfer.Destination? {
            val (id, corner) = destinations[anchor.shipId] ?: return null
            val origin = origins[anchor.shipId] ?: return null
            return WandLinkTransfer.Destination(anchor.copy(pos = corner.offset(anchor.pos.subtract(origin)), shipId = id))
        }
        return WandLinks.load(payload).links.values.mapNotNull { old ->
            val a = destination(old.a) ?: return@mapNotNull null
            val b = destination(old.b) ?: return@mapNotNull null
            if (old.spring && (!old.rope || !old.physical)) return@mapNotNull null
            old.copy(id = UUID.randomUUID(), a = a.anchor, b = b.anchor,
                pose0 = WandLinkTransfer.pose(old.pose0, old.a, a), pose1 = WandLinkTransfer.pose(old.pose1, old.b, b)).also {
                it.settings = old.settings; it.appliedLength = old.appliedLength; it.needsReframe = old.needsReframe
            }
        }
    }

    @JvmStatic
    fun queuePaste(level: ServerLevel, schematic: VdexData, ships: Map<Long, ServerShip>) {
        val payload = schematic.nbtData[ENTRY] ?: return
        if (payload.getInt("version") != 1) {
            ClockworkMod.LOGGER.warn("Unsupported Clockwork link schematic version {}", payload.getInt("version"))
            return
        }
        val destinations = schematic.metadata.ships.mapNotNull { entry ->
            val ship = ships[entry.previousId] ?: return@mapNotNull null
            val size = schematic.nbtData[entry.nbtFile]?.getList("size", Tag.TAG_INT.toInt()) ?: return@mapNotNull null
            if (size.size != 3) return@mapNotNull null
            val center = ship.chunkClaim.getCenterBlockCoordinates(level.yRange, Vector3i())
            entry.previousId to (ship.id to BlockPos(center.x - size.getInt(0) / 2,
                center.y - size.getInt(1) / 2, center.z - size.getInt(2) / 2))
        }.toMap()
        val links = decode(payload, destinations)
        val hinges = decodeHinges(payload, destinations)
        if (links.isEmpty() && hinges.isEmpty() && !payload.contains("lubricant")) return
        val deadline = level.gameTime + 200
        ValkyrienSkiesMod.api.tickEndEvent.on { _, listener ->
            if (level.server.isStopped || level.gameTime > deadline) {
                listener.unregister()
                ClockworkMod.LOGGER.warn("Timed out restoring {} schematic links", links.size)
                return@on
            }
            if (destinations.values.any { level.shipObjectWorld.loadedShips.getById(it.first) == null }) return@on
            listener.unregister()
            ClockworkShipSchematics.paste(level, payload.getList("attachments", 10), destinations)
            LubricantData.get(level).pasteSchematic(level, payload.getCompound("lubricant"), destinations)
            val data = WandLinks.get(level)
            links.forEach { data.restore(level, it) }
            hinges.forEach { HingeLinks.get(level).restore(level, it) }
            WanderwandServer.syncLinks(level)
        }
    }

    internal fun decodeHinges(payload: CompoundTag, destinations: Map<Long, Pair<Long, BlockPos>>): List<HingeLinks.Link> {
        if (payload.getInt("version") != 1) return emptyList()
        val origins = payload.getList("origins", Tag.TAG_COMPOUND.toInt()).filterIsInstance<CompoundTag>()
            .associate { it.getLong("ship") to BlockPos.of(it.getLong("pos")) }
        fun move(anchor: WandAnchor): WandAnchor? {
            val (id, corner) = destinations[anchor.shipId] ?: return null
            val origin = origins[anchor.shipId] ?: return null
            return anchor.copy(pos = corner.offset(anchor.pos.subtract(origin)), shipId = id)
        }
        val saved = CompoundTag().also { it.put("links", payload.getList("hinges", Tag.TAG_COMPOUND.toInt())) }
        return HingeLinks.load(saved).links.values.mapNotNull { old ->
            val a = move(old.a) ?: return@mapNotNull null
            val b = move(old.b) ?: return@mapNotNull null
            old.copy(id = UUID.randomUUID(), a = a, b = b)
        }
    }
}
