package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

/** Block coordinates remain in their body's local space, including while the body moves. */
data class WandAnchor(val pos: BlockPos, val face: Direction, val shipId: Long = -1) {
    fun local(): Vec3 = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.normal).scale(0.505))

    fun world(level: Level, render: Boolean = false): Vec3? {
        val p = local()
        if (shipId < 0) return p
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return null
        val matrix = if (render && ship is ClientShip) ship.renderTransform.shipToWorld else ship.shipToWorld
        val v = matrix.transformPosition(Vector3d(p.x, p.y, p.z))
        return Vec3(v.x, v.y, v.z)
    }

    fun save() = CompoundTag().also {
        it.putLong("pos", pos.asLong()); it.putInt("face", face.ordinal); it.putLong("ship", shipId)
    }

    companion object {
        fun at(level: Level, pos: BlockPos, face: Direction) = WandAnchor(pos.immutable(), face, level.getShipManagingPos(pos)?.id ?: -1)
        fun load(tag: CompoundTag) = WandAnchor(BlockPos.of(tag.getLong("pos")),
            Direction.values()[tag.getInt("face").coerceIn(0, 5)], tag.getLong("ship"))
    }
}
