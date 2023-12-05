package org.valkyrienskies.clockwork.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
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
                    { direction -> BlockPos(direction.getNormal()).asLong() },
                    { direction -> direction },
                    { _, _ -> throw IllegalArgumentException("Duplicate keys") },
                    { Long2ObjectOpenHashMap() }
                )
            )
}