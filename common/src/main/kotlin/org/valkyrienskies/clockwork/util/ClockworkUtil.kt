package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld


object ClockworkUtil {

    @JvmStatic
    fun updateBlockStateWeight(serverLevel: ServerLevel, blockPos: BlockPos, oldWeight: Double, newWeight: Double) {
        val state = serverLevel.getBlockState(blockPos)

        val (_, prevBlockType) = BlockStateInfo.get(state) ?: return

        serverLevel.shipObjectWorld.onSetBlock(blockPos.x,
            blockPos.y,
            blockPos.z,
            serverLevel.dimensionId,
            prevBlockType,
            prevBlockType,
            oldWeight,
            newWeight)
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
}