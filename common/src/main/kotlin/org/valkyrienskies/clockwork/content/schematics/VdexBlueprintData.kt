package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtUtils
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Quaterniondc
import net.minecraft.world.phys.Vec3
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.schematic.VdexMetadata
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.schematic.VdexData
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexShipEntry
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class VdexBlueprintData(val data: VdexData, val ships: List<Ship>) {
    data class Block(val pos: BlockPos, val state: BlockState, val nbt: CompoundTag?)
    data class Ship(val entry: VdexShipEntry, val size: BlockPos, val blocks: List<Block>, val entities: List<CompoundTag>,
                    val pivot: Vector3d = massCenter(blocks, size)) {
        fun relativePoint(point: Vector3d): Vector3d = point.sub(pivot).mul(entry.scale)
            .rotate(entry.relativeRot).add(entry.relativePos)

        fun worldPoint(point: Vector3d, position: Vec3, rotation: Quaterniondc): Vector3d =
            relativePoint(point).rotate(rotation).add(position.x, position.y, position.z)
    }

    val blockCount = ships.sumOf { it.blocks.size }
    val bounds: AABB = run {
        val points = ships.flatMap { ship ->
            buildList {
                for (x in listOf(0, ship.size.x)) for (y in listOf(0, ship.size.y)) for (z in listOf(0, ship.size.z))
                    add(ship.relativePoint(Vector3d(x.toDouble(), y.toDouble(), z.toDouble())))
            }
        }
        AABB(points.minOf { it.x }, points.minOf { it.y }, points.minOf { it.z },
            points.maxOf { it.x }, points.maxOf { it.y }, points.maxOf { it.z })
    }

    companion object {
        private const val PIVOT = "ClockworkPivot"

        @JvmStatic
        fun capturePivots(level: ServerLevel, metadata: VdexMetadata, blocks: Map<String, CompoundTag>): Map<String, CompoundTag> {
            val captured = blocks.toMutableMap()
            for (entry in metadata.ships) {
                val ship = level.shipObjectWorld.allShips.getById(entry.previousId) ?: continue
                val bounds = ship.shipAABB ?: continue
                val tag = blocks[entry.nbtFile]?.copy() ?: continue
                val pivot = Vector3d(ship.transform.positionInModel).sub(bounds.minX().toDouble(), bounds.minY().toDouble(), bounds.minZ().toDouble())
                tag.put(PIVOT, CompoundTag().apply { putDouble("X", pivot.x); putDouble("Y", pivot.y); putDouble("Z", pivot.z) })
                captured[entry.nbtFile] = tag
            }
            return captured
        }

        private fun massCenter(blocks: List<Block>, size: BlockPos): Vector3d {
            val center = Vector3d()
            var total = 0.0
            for (block in blocks) {
                val mass = BlockStateInfo.get(block.state)?.first ?: continue
                if (!mass.isFinite() || mass <= 0.0) continue
                center.add((block.pos.x + .5) * mass, (block.pos.y + .5) * mass, (block.pos.z + .5) * mass)
                total += mass
            }
            return if (total > 0.0) center.div(total) else Vector3d(size.x / 2.0, size.y / 2.0, size.z / 2.0)
        }

        const val MAX_FILE_BYTES = 8 * 1024 * 1024
        const val MAX_EXPANDED_BYTES = 64 * 1024 * 1024
        const val MAX_BLOCKS = 131072
        const val MAX_SHIPS = 64
        const val CHUNK_BYTES = 24000

        fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

        fun validId(id: String) = id.length == 64 && id.all { it in '0'..'9' || it in 'a'..'f' }

        fun validateArchive(bytes: ByteArray) {
            require(bytes.size in 1..MAX_FILE_BYTES) { "Blueprint file is too large" }
            var total = 0
            var entries = 0
            val names = HashSet<String>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.name.startsWith("/") && !entry.name.contains("\\") && entry.name.split("/").none { it == ".." }) { "Invalid archive path" }
                    require(++entries <= 2048 && names.add(entry.name)) { "Invalid blueprint archive" }
                    val content = zip.readNBytes(MAX_EXPANDED_BYTES - total + 1)
                    total += content.size
                    require(total <= MAX_EXPANDED_BYTES) { "Expanded blueprint is too large" }
                    if (!entry.isDirectory && entry.name.startsWith("structure/"))
                        NbtIo.read(DataInputStream(ByteArrayInputStream(content)), NbtAccounter(MAX_EXPANDED_BYTES.toLong()))
                }
            }
            require("metadata.json" in names) { "Not a VDex schematic" }
        }

        fun read(path: Path): VdexBlueprintData {
            require(Files.size(path) <= MAX_FILE_BYTES) { "Blueprint file is too large" }
            validateArchive(Files.readAllBytes(path))
            return parse(VdexIO.read(path))
        }

        fun parse(data: VdexData): VdexBlueprintData {
            require(data.metadata.version == 1 && data.metadata.ships.size in 1..MAX_SHIPS) { "Unsupported VDex schematic" }
            require(data.metadata.ships.map { it.previousId }.distinct().size == data.metadata.ships.size) { "Duplicate ship IDs" }
            require(data.metadata.constraints.size <= 4096) { "Too many joints" }
            var count = 0
            val ships = data.metadata.ships.map { entry ->
                require(entry.relativePos.isFinite && entry.relativePos.length() <= 2048 && entry.previousCenter.isFinite &&
                    entry.relativeRot.isFinite && entry.relativeRot.lengthSquared() in .99..1.01 &&
                    entry.scale.isFinite() && entry.scale in .01..64.0) { "Invalid ship transform" }
                val tag = data.nbtData[entry.nbtFile] ?: error("Missing ship structure")
                val sizeTag = tag.getList("size", 3)
                require(sizeTag.size == 3) { "Invalid ship size" }
                val size = BlockPos(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2))
                require(size.x in 1..256 && size.y in 1..256 && size.z in 1..256) { "Ship exceeds blueprint dimensions" }
                val paletteTag = if (tag.contains("palette")) tag.getList("palette", 10)
                    else tag.getList("palettes", 9).getList(0)
                require(paletteTag.size in 1..65536) { "Missing block palette" }
                val palette = paletteTag.map { value ->
                    val state = value as CompoundTag
                    val name = ResourceLocation.tryParse(state.getString("Name"))
                    require(name != null && BuiltInRegistries.BLOCK.containsKey(name)) { "Missing block: ${state.getString("Name")}" }
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), state)
                }
                val occupied = HashSet<BlockPos>()
                val blocks = tag.getList("blocks", 10).mapNotNull { value ->
                    val block = value as CompoundTag
                    val coords = block.getList("pos", 3)
                    require(coords.size == 3 && block.getInt("state") in palette.indices) { "Invalid blueprint block" }
                    val pos = BlockPos(coords.getInt(0), coords.getInt(1), coords.getInt(2))
                    require(pos.x in 0 until size.x && pos.y in 0 until size.y && pos.z in 0 until size.z && occupied.add(pos)) { "Invalid block position" }
                    val state = palette[block.getInt("state")]
                    if (state.isAir) null else {
                        require(++count <= MAX_BLOCKS) { "Blueprint has too many blocks" }
                        Block(pos, state, if (block.contains("nbt")) block.getCompound("nbt").copy() else null)
                    }
                }
                require(blocks.isNotEmpty()) { "Empty blueprint ship" }
                val entities = tag.getList("entities", 10)
                require(entities.size <= 512) { "Too many schematic entities" }
                val pivot = if (tag.contains(PIVOT, 10)) tag.getCompound(PIVOT).let {
                    Vector3d(it.getDouble("X"), it.getDouble("Y"), it.getDouble("Z"))
                } else massCenter(blocks, size)
                require(pivot.isFinite && pivot.lengthSquared() <= 4096.0 * 4096.0) { "Invalid ship pivot" }
                Ship(entry, size, blocks.sortedWith(compareBy({ it.pos.y }, { it.pos.z }, { it.pos.x })), entities.map { (it as CompoundTag).copy() }, pivot)
            }
            for (constraint in data.metadata.constraints) {
                require(constraint.metadata.shipIndex0 in ships.indices && constraint.metadata.shipIndex1 in ships.indices &&
                    constraint.metadata.position0offset.isFinite && constraint.metadata.position1offset.isFinite &&
                    constraint.joint.pose0.pos.isFinite && constraint.joint.pose1.pos.isFinite &&
                    constraint.joint.pose0.rot.isFinite && constraint.joint.pose1.rot.isFinite) { "Invalid joint endpoints" }
            }
            return VdexBlueprintData(data, ships)
        }
    }
}
