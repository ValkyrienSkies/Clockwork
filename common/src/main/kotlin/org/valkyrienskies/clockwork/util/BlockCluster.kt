package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.mod.common.util.toJOML

data class BlockCluster(val blocks: HashSet<Vector3ic>) {
    fun merge(other: BlockCluster): BlockCluster {
        if (other == this) {
            return this
        }
        if (other.blocks.size > blocks.size) {
            return other.merge(this)
        }
        blocks.addAll(other.blocks)
        other.clear()
        return this
    }

    fun overlaps(other: BlockCluster): Boolean {
        return blocks.any { other.contains(it) }
    }

    fun add(block: Vector3ic) {
        blocks.add(block)
    }

    fun add(block: BlockPos) {
        blocks.add(block.toJOML())
    }

    fun add(x: Int, y: Int, z: Int) {
        blocks.add(Vector3i(x, y, z))
    }

    fun addAll(other: BlockCluster) {
        blocks.addAll(other.blocks)
    }

    fun remove(block: Vector3ic) {
        blocks.remove(block)
    }

    fun remove(block: BlockPos) {
        blocks.remove(block.toJOML())
    }

    fun remove(x: Int, y: Int, z: Int) {
        blocks.remove(Vector3i(x, y, z))
    }

    fun removeAll(other: BlockCluster) {
        blocks.removeAll(other.blocks)
    }

    fun contains(block: Vector3ic): Boolean {
        return blocks.contains(block)
    }

    fun clear() {
        blocks.clear()
    }

    fun isEmpty(): Boolean {
        return blocks.isEmpty()
    }

    fun size(): Int {
        return blocks.size
    }
}
