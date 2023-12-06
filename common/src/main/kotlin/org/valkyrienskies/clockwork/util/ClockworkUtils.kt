package org.valkyrienskies.clockwork.util

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.stream.Collectors


object ClockworkUtils {

    fun writeVec3(vec: Vec3): ListTag {
        val tag = ListTag()
        tag.add(DoubleTag.valueOf(vec.x))
        tag.add(DoubleTag.valueOf(vec.y))
        tag.add(DoubleTag.valueOf(vec.z))
        return tag
    }

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
}