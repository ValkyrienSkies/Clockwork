package org.valkyrienskies.clockwork.util

import com.fasterxml.jackson.core.JsonProcessingException
import g_mungus.vlib.VLib
import g_mungus.vlib.v2.api.extension.fillFromVoxelSet
import g_mungus.vlib.v2.api.extension.placeAsShip
import g_mungus.vlib.v2.api.extension.scheduleCallback
import g_mungus.vlib.v2.internal.assembly.BoundedVoxelSet
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.SelectedAreaToolkit
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.clockwork.util.MathFunctions.chunkPos
import org.valkyrienskies.clockwork.util.MathFunctions.toVector3i
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ChunkClaim
import org.valkyrienskies.core.api.world.connectivity.DoubleComponentAugmentation
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil.defaultMapper
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.toBlockPos
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap


object ClockworkUtils {

    val wanderliteNodesToAdd: HashMap<BlockPos, Double> = HashMap()

    @JvmStatic
    fun tick(level: ServerLevel) {
        val successfullyAdded = HashSet<BlockPos>()
        wanderliteNodesToAdd.forEach { (pos, force) ->
            val ship = level.getShipObjectManagingPos(BlockPos(pos.x, pos.y, pos.z))
            if (ship != null) {
                ship.getAttachment<WanderShipControl>()?.addBlock(pos, force) ?: return@forEach
                successfullyAdded.add(pos)
            }
        }
        successfullyAdded.forEach { wanderliteNodesToAdd.remove(it) }
    }

    @JvmStatic
    fun DenseBlockPosSet.toBlockPosSet(): Set<BlockPos> {
        val set = mutableSetOf<BlockPos>()
        this.forEach { x, y, z ->
            set.add(BlockPos(x, y, z))
        }
        return set
    }

    @JvmStatic
    fun Set<BlockPos>.toBoundedVoxelSet(): BoundedVoxelSet {
        var minBound: Vector3ic? = null
        var maxBound: Vector3ic? = null
        this.forEach { bp ->
            val pos = bp.toJOML()
            if (minBound == null) {
                minBound = pos
                maxBound = pos
            } else {
                minBound = Vector3i(
                    Math.min(minBound!!.x(), pos.x),
                    Math.min(minBound!!.y(), pos.y),
                    Math.min(minBound!!.z(), pos.z)
                )
                maxBound = Vector3i(
                    Math.max(maxBound!!.x(), pos.x),
                    Math.max(maxBound!!.y(), pos.y),
                    Math.max(maxBound!!.z(), pos.z)
                )
            }
        }
        if (minBound == null || maxBound == null) {
            ClockworkMod.LOGGER.warn("Tried to convert empty DenseBlockPosSet to BoundedVoxelSet!")
            return BoundedVoxelSet(HashSet(), Vector3i(0, 0, 0), Vector3i(0, 0, 0))
        }
        return BoundedVoxelSet(this, minBound, maxBound)
    }

    @JvmStatic
    fun DenseBlockPosSet.toBoundedVoxelSet(): BoundedVoxelSet {
        var minBound: Vector3ic? = null
        var maxBound: Vector3ic? = null
        this.forEach { x, y, z ->
            val pos = Vector3i(x, y, z)
            if (minBound == null) {
                minBound = pos
                maxBound = pos
            } else {
                minBound = Vector3i(
                    Math.min(minBound!!.x(), pos.x),
                    Math.min(minBound!!.y(), pos.y),
                    Math.min(minBound!!.z(), pos.z)
                )
                maxBound = Vector3i(
                    Math.max(maxBound!!.x(), pos.x),
                    Math.max(maxBound!!.y(), pos.y),
                    Math.max(maxBound!!.z(), pos.z)
                )
            }
        }
        if (minBound == null || maxBound == null) {
            ClockworkMod.LOGGER.warn("Tried to convert empty DenseBlockPosSet to BoundedVoxelSet!")
            return BoundedVoxelSet(HashSet(), Vector3i(0, 0, 0), Vector3i(0, 0, 0))
        }
        return BoundedVoxelSet(this.toBlockPosSet(), minBound, maxBound)
    }

    @JvmStatic
    fun StructureTemplate.fillFromDenseBlockPosSet(level: ServerLevel, set: DenseBlockPosSet) {
        this.fillFromVoxelSet(level, set.toBoundedVoxelSet())
    }

    @JvmStatic
    fun assembleFromDenseBlockSet(level: ServerLevel, set: DenseBlockPosSet, static: Boolean): ServerShip? {
        val voxelSet = set.toBoundedVoxelSet()
        val ship = StructureTemplate().let {
            it.fillFromVoxelSet(level, voxelSet)
            it.placeAsShip(level, BlockPos.containing(level.toWorldCoordinates(voxelSet.min.toBlockPos())), true)
        } ?: return null

        //sorry mungus i had to copy this
        cleanupOriginalBlocks(level, voxelSet) {
            ship.isStatic = static
        }

        return ship
    }

    @JvmStatic
    fun assembleFromBlockSet(level: ServerLevel, set: Set<BlockPos>, static: Boolean): ServerShip? {
        val voxelSet = set.toBoundedVoxelSet()
        val ship = StructureTemplate().let {
            it.fillFromVoxelSet(level, voxelSet)
            it.placeAsShip(level, BlockPos.containing(level.toWorldCoordinates(voxelSet.min.toBlockPos())), true)
        } ?: return null

        //sorry mungus i had to copy this
        cleanupOriginalBlocks(level, voxelSet) {
            ship.isStatic = static
        }

        return ship
    }


    private fun cleanupOriginalBlocks(level: ServerLevel, voxelSet: BoundedVoxelSet, whenComplete: () -> Unit) {
        voxelSet.voxels.forEach { pos ->
            val be = level.getBlockEntity(pos)
            if (be != null) {
                level.removeBlockEntity(pos)
            }
            level.setBlock(pos, VLib.GHOST_BLOCK.defaultBlockState(), 0)
        }

        level.scheduleCallback(4) {
            voxelSet.voxels.forEach { pos ->
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
            }

            whenComplete.invoke()
        }
    }

    @JvmStatic
    fun writeVec3(vec: Vec3): ListTag {
        val tag = ListTag()
        tag.add(DoubleTag.valueOf(vec.x))
        tag.add(DoubleTag.valueOf(vec.y))
        tag.add(DoubleTag.valueOf(vec.z))
        return tag
    }

    @JvmStatic
    fun readVec3(tag: ListTag): Vec3 {
        return Vec3(tag.getDouble(0), tag.getDouble(1), tag.getDouble(2))
    }

    fun fromNormal(x: Int, y: Int, z: Int): Direction {
        return BY_NORMAL[BlockPos.asLong(x, y, z)] as Direction
    }

    private val BY_NORMAL: Long2ObjectMap<Direction> =
        Arrays.stream(Direction.values())
            .collect(
                Collectors.toMap(
                    { direction -> BlockPos(direction.normal).asLong() },
                    { direction -> direction },
                    { _, _ -> throw IllegalArgumentException("Duplicate keys") },
                    { Long2ObjectOpenHashMap() }
                )
            )

    fun writeAABBi(bb: AABBic): ListTag {
        val bbtag = ListTag()
        bbtag.add(FloatTag.valueOf(bb.minX().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.minY().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.minZ().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxX().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxY().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxZ().toFloat()))
        return bbtag
    }

    fun readAABBi(bbtag: ListTag?): AABBic? {
        if (bbtag == null || bbtag.isEmpty()) return null
        return AABBi(
            bbtag.getFloat(0).toInt(),
            bbtag.getFloat(1).toInt(),
            bbtag.getFloat(2).toInt(),
            bbtag.getFloat(3).toInt(),
            bbtag.getFloat(4).toInt(),
            bbtag.getFloat(5).toInt()
        )
    }

    fun writeVector3i(vec: Vector3ic): ListTag {
        val tag = ListTag()
        tag.add(IntTag.valueOf(vec.x()))
        tag.add(IntTag.valueOf(vec.y()))
        tag.add(IntTag.valueOf(vec.z()))
        return tag
    }

    fun readVector3i(tag: ListTag): Vector3ic {
        return Vector3i(tag.getInt(0), tag.getInt(1), tag.getInt(2))
    }

    fun readVector3i(buf: FriendlyByteBuf): Vector3ic {
        return Vector3i(buf.readInt(), buf.readInt(), buf.readInt())
    }

    fun CompoundTag.getVector3d(prefix: String): Vector3d? {
        return if (
            !this.contains(prefix + "x") ||
            !this.contains(prefix + "y") ||
            !this.contains(prefix + "z")
        ) {
            null
        } else {
            Vector3d(
                this.getDouble(prefix + "x"),
                this.getDouble(prefix + "y"),
                this.getDouble(prefix + "z")
            )
        }
    }

    fun writeVector3i(buf: FriendlyByteBuf, vector3f: Vector3ic) {
        buf.writeInt(vector3f.x())
        buf.writeInt(vector3f.y())
        buf.writeInt(vector3f.z())
    }

    fun loadArea(nbt: CompoundTag?): SelectedAreaToolkit {
        val toolKit = SelectedAreaToolkit()
        if (nbt != null) {
            val nb = nbt.getByteArray(ClockworkConstants.Nbt.SELECTED_DATA)
            try {
                toolKit.overwriteFrom(
                    defaultMapper.readValue(
                        nb,
                        SelectedAreaToolkit::class.java
                    )
                )
            } catch (ignored: IOException) {
            }
        }
        return toolKit
    }

    fun saveArea(nbt: CompoundTag, area: SelectedAreaToolkit?): CompoundTag {
        try {
            nbt.putByteArray(ClockworkConstants.Nbt.SELECTED_DATA, defaultMapper.writeValueAsBytes(area))
        } catch (ignored: JsonProcessingException) {
        }
        return nbt
    }

    fun retrieveGasInfoFromPocket(pos: Vector3ic, level: ServerLevel): Pair<HashMap<GasType, Double>, Double> {
        val gasMap = HashMap<GasType, Double>()
        for (type in GasTypeRegistry.GAS_TYPES.values) {
            val key = ClockworkAugmentations.getComponentAugmentation("gas_" + type.name.lowercase(Locale.getDefault()))
            val gas = level.shipObjectWorld.getAirComponentAugmentation(key, pos.x(), pos.y(), pos.z(), level.dimensionId)
            gasMap[type] = gas
        }

        val heatEnergy = level.shipObjectWorld.getAirComponentAugmentation(
            ClockworkAugmentations.getComponentAugmentation("heatEnergy"),
            pos.x(),
            pos.y(),
            pos.z(),
            level.dimensionId
        )

        return Pair(gasMap, heatEnergy)
    }

    fun getRealPos(level: Level?, blockPos: BlockPos): Vector3d
    { return vsApi.getShipManagingBlock(level, blockPos)?.positionToWorld(blockPos.toJOMLD().add(0.5,0.5,0.5)) ?: blockPos.toJOMLD().add(0.5,0.5,0.5) }

    /**
     * Retrieves all components within a given chunk claim, using a key as reference.
     *
     * Deprecated implementation
     */
    @Deprecated("Deprecated. Replaced with proper implementation in VS Core.")
    fun getAirComponentsInChunkClaim(claim: ChunkClaim, level: ServerLevel, referenceKey: DoubleComponentAugmentation): HashMap<Vector3i, Long> {
        val map = HashMap<Vector3i, Long>()
        level.shipObjectWorld.getFromEachAirComponentRoot(referenceKey, level.dimensionId).keys.forEach { pos ->
            if (claim.contains(pos.chunkPos().x, pos.chunkPos().z)) {
                map[pos.toVector3i()] = try {
                    level.shipObjectWorld.getAirComponentSize(pos.first, pos.second, pos.third, level.dimensionId)
                } catch (e: IllegalArgumentException) {
                    -1
                }
                if (map[pos.toVector3i()] == -1L) {
                    ClockworkMod.LOGGER.warn("Failed to get air component size at $pos")
                }
            }
        }
        return HashMap(map.filterNot { it.value == -1L })
    }

    /**
     * Retrieves all components within a given chunk claim, using a key as reference.
     *
     * Deprecated implementation
     */
    @Deprecated("Deprecated. Replaced with proper implementation in VS Core.")
    fun getSolidComponentsInChunkClaim(claim: ChunkClaim, level: ServerLevel, referenceKey: DoubleComponentAugmentation): HashMap<Vector3i, Long> {
        val map = HashMap<Vector3i, Long>()
        level.shipObjectWorld.getFromEachSolidComponentRoot(referenceKey, level.dimensionId).keys.forEach { pos ->
            if (claim.contains(pos.chunkPos().x, pos.chunkPos().z)) {
                map[pos.toVector3i()] = try {
                    level.shipObjectWorld.getSolidComponentSize(pos.first, pos.second, pos.third, level.dimensionId)
                } catch (e: IllegalArgumentException) {
                    -1
                }
            }
        }
        return HashMap(map.filterNot { it.value == -1L })
    }
}
