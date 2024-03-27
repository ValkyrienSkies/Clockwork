package org.valkyrienskies.clockwork.util

import org.joml.Vector3ic
data class BlockCluster(val blocks: HashSet<Vector3ic>) {
    fun merge(other: BlockCluster): BlockCluster {
        if (other == this) {
            return this
        }
        if (other.blocks.size > blocks.size) {
            return other.merge(this)
        }
        blocks.addAll(other.blocks)
        other.blocks.clear()
        return this
    }
}
