package org.valkyrienskies.clockwork.util

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import org.valkyrienskies.core.util.asLong

/**
 * Use to track the area of a ship when projected into the x/y/z planes, used primarily for drag calculations.
 *
 * Time complexity: O(1) for all operations
 * Space complexity: O(n) where n is the area of the largest side
 */
class SideProfileTracker {

    /**
     * Maps (y, z) -> count of blocks
     */
    private val xMap = Long2IntOpenHashMap()

    /**
     * Maps (x, z) -> count of blocks
     */
    private val yMap = Long2IntOpenHashMap()

    /**
     * Maps (x, y) -> count of blocks
     */
    private val zMap = Long2IntOpenHashMap()

    /**
     * The area of the ship in blocks when projected onto the x-axis
     */
    val xArea: Int get() = xMap.size

    /**
     * The area of the ship in blocks when projected onto the y-axis
     */
    val yArea: Int get() = yMap.size

    /**
     * The area of the ship in blocks when projected onto the z-axis
     */
    val zArea: Int get() = zMap.size

    /**
     * Add a block at the coordinates (x, y, z)
     *
     * WARNING: Do not add a block twice, this will break things!
     */
    fun add(x: Int, y: Int, z: Int) {
        val yz = asLong(y, z)
        val xz = asLong(x, z)
        val xy = asLong(x, y)

        xMap.addTo(yz, 1)
        yMap.addTo(xz, 1)
        zMap.addTo(xy, 1)
    }

    /**
     * Removes a block at the coordinates (x, y, z)
     *
     * WARNING: Do not remove a block twice, this will break things!
     */
    fun remove(x: Int, y: Int, z: Int) {
        val yz = asLong(y, z)
        val xz = asLong(x, z)
        val xy = asLong(x, y)

        if (xMap.addTo(yz, -1) == 1) {
            xMap.remove(yz)
        }
        if (yMap.addTo(xz, -1) == 1) {
            yMap.remove(xz)
        }
        if (zMap.addTo(xy, -1) == 1) {
            zMap.remove(xy)
        }

    }

}