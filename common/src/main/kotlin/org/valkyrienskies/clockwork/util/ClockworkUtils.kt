package org.valkyrienskies.clockwork.util

import com.fasterxml.jackson.core.JsonProcessingException
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.SelectedAreaToolkit
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.MathFunctions.chunkPos
import org.valkyrienskies.clockwork.util.MathFunctions.toVector3i
import org.valkyrienskies.core.api.ships.properties.ChunkClaim
import org.valkyrienskies.core.api.world.connectivity.DoubleComponentAugmentation
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil.defaultMapper
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.io.IOException
import java.util.*
import java.util.stream.Collectors


object ClockworkUtils {

    @JvmStatic
    fun updateBlockStateWeight(serverLevel: ServerLevel, blockPos: BlockPos, oldWeight: Double, newWeight: Double){
        val state = serverLevel.getBlockState(blockPos)

        val (_, prevBlockType) = BlockStateInfo.get(state) ?: return

        serverLevel.shipObjectWorld.onSetBlock(blockPos.x, blockPos.y, blockPos.z, serverLevel.dimensionId, prevBlockType, prevBlockType, oldWeight, newWeight)
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

    fun retrieveGasInfoFromPocket(pos: Vector3ic, level: ServerLevel): Pair<EnumMap<GasType, Double>, Double> {
        val gasMap = EnumMap<GasType, Double>(GasType::class.java)
        for (type in GasType.entries) {
            val key = ClockworkAugmentations.getComponentAugmentation("gas_" + type.name.lowercase(Locale.getDefault()))
            val gas = level.shipObjectWorld.getAirComponentAugmentation(key, pos.x(), pos.y(), pos.z(), level.dimensionId)
            gasMap[type] = gas
        }

        val temperature = level.shipObjectWorld.getAirComponentAugmentation(
            ClockworkAugmentations.getComponentAugmentation("temperature"),
            pos.x(),
            pos.y(),
            pos.z(),
            level.dimensionId
        )

        return Pair(gasMap, temperature)
    }

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