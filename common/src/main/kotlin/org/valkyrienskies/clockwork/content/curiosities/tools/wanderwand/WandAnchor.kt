package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

/** Block coordinates remain in their body's local space, including while the body moves. */
data class WandAnchor(val pos: BlockPos, val face: Direction, val shipId: Long = -1, val winch: Boolean = false,
                      val offset: Vec3? = null, val entityId: UUID? = null, val dragonPart: String? = null) {
    private var trackingId = -1

    fun entity(level: Level): Entity? {
        val uuid = entityId ?: return null
        if (level is ServerLevel) return level.getEntity(uuid)?.also { trackingId = it.id }
        return level.getEntity(trackingId)?.takeIf { it.uuid == uuid }
    }

    fun local(): Vec3 = Vec3.atCenterOf(pos).add(offset ?: Vec3.atLowerCornerOf(face.normal).scale(0.505))

    fun world(level: Level, render: Boolean = false, partialTick: Float = 1f): Vec3? {
        if (entityId != null) {
            val target = entity(level) ?: return null
            val part = if (target is EnderDragon && dragonPart != null)
                target.subEntities.firstOrNull { it.name == dragonPart } ?: target else target
            val box = part.boundingBox
            val inset = minOf(3.0 / 16, box.xsize / 4, box.ysize / 4, box.zsize / 4)
            val impact = part.position().add(offset ?: Vec3.ZERO)
            val tip = Vec3(impact.x.coerceIn(box.minX + inset, box.maxX - inset),
                impact.y.coerceIn(box.minY + inset, box.maxY - inset),
                impact.z.coerceIn(box.minZ + inset, box.maxZ - inset))
            return tip.add(part.getPosition(partialTick).subtract(part.position()))
        }
        val p = local()
        if (shipId < 0) return p
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return null
        val matrix = if (render && ship is ClientShip) ship.renderTransform.shipToWorld else ship.shipToWorld
        val v = matrix.transformPosition(Vector3d(p.x, p.y, p.z))
        return Vec3(v.x, v.y, v.z)
    }

    fun save() = CompoundTag().also {
        if (entityId != null) {
            it.putUUID("entity", entityId)
            it.putInt("trackingId", trackingId)
            dragonPart?.let { name -> it.putString("dragonPart", name) }
        }
        offset?.let { point ->
            it.put("offset", CompoundTag().also { tag ->
                tag.putDouble("x", point.x); tag.putDouble("y", point.y); tag.putDouble("z", point.z)
            })
        }
        it.putBoolean("winch", winch); it.putLong("pos", pos.asLong()); it.putInt("face", face.ordinal); it.putLong("ship", shipId)
    }

    companion object {
        fun onEntity(hit: Entity, point: Vec3, face: Direction): WandAnchor {
            val part = hit as? EnderDragonPart
            val target = part?.parentMob ?: hit
            return WandAnchor(BlockPos.containing(point), face, offset = point.subtract(hit.position()),
                entityId = target.uuid, dragonPart = part?.name).also { it.trackingId = target.id }
        }

        fun at(level: Level, pos: BlockPos, face: Direction) = WandAnchor(pos.immutable(), face, level.getShipManagingPos(pos)?.id ?: -1)
        fun load(tag: CompoundTag) = WandAnchor(BlockPos.of(tag.getLong("pos")),
            Direction.values()[tag.getInt("face").coerceIn(0, 5)], tag.getLong("ship"), tag.getBoolean("winch"),
            if (tag.contains("offset")) tag.getCompound("offset").let { Vec3(it.getDouble("x"), it.getDouble("y"), it.getDouble("z")) } else null,
            if (tag.hasUUID("entity")) tag.getUUID("entity") else null,
            tag.getString("dragonPart").ifEmpty { null }).also { it.trackingId = tag.getInt("trackingId") }
    }
}
