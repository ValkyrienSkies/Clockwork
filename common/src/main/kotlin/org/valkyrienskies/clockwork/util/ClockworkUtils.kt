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

    fun oldQuaternionf(pX: Float, pY: Float, pZ: Float): Quaternionf {
        return oldQuaternionf(pX, pY, pZ, false)
    }

    fun oldQuaternionf(pX: Float, pY: Float, pZ: Float, degrees: Boolean): Quaternionf {
        var pX = pX
        var pY = pY
        var pZ = pZ
        if (degrees) {
            pX *= 0.017453292f
            pY *= 0.017453292f
            pZ *= 0.017453292f
        }

        val f = Mth.sin(0.5f * pX)
        val f1 = Mth.cos(0.5f * pX)
        val f2 = Mth.sin(0.5f * pY)
        val f3 = Mth.cos(0.5f * pY)
        val f4 = Mth.sin(0.5f * pZ)
        val f5 = Mth.cos(0.5f * pZ)
        val x = f * f3 * f5 + f1 * f2 * f4
        val y = f1 * f2 * f5 - f * f3 * f4
        val z = f * f2 * f5 + f1 * f3 * f4
        val w = f1 * f3 * f5 - f * f2 * f4
        return Quaternionf(x, y, z, w)
    }

    fun load(nbt: CompoundTag?): SelectedAreaToolkit {
        val toolKit = SelectedAreaToolkit()
        if (nbt != null) {
            toolKit.overwriteFrom(getMapper().readValue<SelectedAreaToolkit>(nbt.getByteArray("SelectedData")))
        }
        return toolKit
    }

    private fun getMapper(): ObjectMapper {
        return VSJacksonUtil.defaultMapper
    }

    fun save(compoundTag: CompoundTag, area: SelectedAreaToolkit): CompoundTag {
        compoundTag.putByteArray("SelectedData", getMapper().writeValueAsBytes(area))
        return compoundTag
    }
}