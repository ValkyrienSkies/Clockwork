package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.levelgen.structure.BoundingBox

/**
 * Helper functions for dealing with bounding boxes
 */
object BoundingBoxHelper {
    /**
     * Orient a bounding box from facing south to facing any other direction.<br></br>
     * Created because the vanilla implementation doesn't support `UP` and `DOWN`.
     *
     * @param origin  coordinate around which rotation happens
     * @param xOffset x offset applied before rotation
     * @param yOffset y offset applied before rotation
     * @param zOffset z offset applied before rotation
     * @param width   width of the bounding box
     * @param height  height of the bounding box
     * @param depth   depth of the bounding box
     * @param facing  direction to rotate the bounding box to face
     *
     * @return rotated bounding box
     */
    fun orientBox(
        origin: BlockPos,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        facing: Direction
    ): BoundingBox {
        return orientBox(
            origin.getX(),
            origin.getY(),
            origin.getZ(),
            xOffset,
            yOffset,
            zOffset,
            width,
            height,
            depth,
            facing
        )
    }

    /**
     * Orient a bounding box from facing south to facing any other direction.<br></br>
     * Created because the vanilla implementation doesn't support `UP` and `DOWN`.
     *
     * @param originX x coordinate around which rotation happens
     * @param originY y coordinate around which rotation happens
     * @param originZ z coordinate around which rotation happens
     * @param xOffset x offset applied before rotation
     * @param yOffset y offset applied before rotation
     * @param zOffset z offset applied before rotation
     * @param width   width of the bounding box
     * @param height  height of the bounding box
     * @param depth   depth of the bounding box
     * @param facing  direction to rotate the bounding box to face
     *
     * @return rotated bounding box
     */
    fun orientBox(
        originX: Int,
        originY: Int,
        originZ: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        facing: Direction
    ): BoundingBox {
        when (facing) {
            Direction.SOUTH -> return BoundingBox(
                originX + xOffset,
                originY + yOffset,
                originZ + zOffset,
                originX + width - 1 + xOffset,
                originY + height - 1 + yOffset,
                originZ + depth - 1 + zOffset
            )

            Direction.NORTH -> return BoundingBox(
                originX + xOffset,
                originY + yOffset,
                originZ - depth + 1 + zOffset,
                originX + width - 1 + xOffset,
                originY + height - 1 + yOffset,
                originZ + zOffset
            )

            Direction.WEST -> return BoundingBox(
                originX - depth + 1 + zOffset,
                originY + yOffset,
                originZ + xOffset,
                originX + zOffset,
                originY + height - 1 + yOffset,
                originZ + width - 1 + xOffset
            )

            Direction.EAST -> return BoundingBox(
                originX + zOffset,
                originY + yOffset,
                originZ + xOffset,
                originX + depth - 1 + zOffset,
                originY + height - 1 + yOffset,
                originZ + width - 1 + xOffset
            )

            Direction.UP -> return BoundingBox(
                originX + xOffset,
                originY + zOffset,
                originZ + height - 1 + yOffset,
                originX + width - 1 + xOffset,
                originY + depth - 1 + zOffset,
                originZ + yOffset
            )

            Direction.DOWN -> return BoundingBox(
                originX + xOffset,
                originY + depth - 1 + zOffset,
                originZ + yOffset,
                originX + width - 1 + xOffset,
                originY + zOffset,
                originZ + height - 1 + yOffset
            )

            else -> return BoundingBox(
                originX + xOffset,
                originY + yOffset,
                originZ + zOffset,
                originX + width - 1 + xOffset,
                originY + height - 1 + yOffset,
                originZ + depth - 1 + zOffset
            )
        }
    }
}
