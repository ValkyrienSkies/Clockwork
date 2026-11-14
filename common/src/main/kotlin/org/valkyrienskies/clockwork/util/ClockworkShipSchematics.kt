package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import org.joml.primitives.AABBi
import org.valkyrienskies.clockwork.content.curiosities.meteor.MeteorShipAttachment
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronFrozenShip
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.mod.common.shipObjectWorld

object ClockworkShipSchematics {
    fun capture(level: ServerLevel, origins: Map<Long, BlockPos>): ListTag = ListTag().also { entries ->
        for ((id, origin) in origins) {
            val ship = level.shipObjectWorld.loadedShips.getById(id) ?: continue
            val tag = CompoundTag()
            ship.getAttachment(GravitronFrozenShip::class.java)?.let { frozen ->
                tag.put("Frozen", CompoundTag().apply {
                    putDouble("X", frozen.x - origin.x); putDouble("Y", frozen.y - origin.y); putDouble("Z", frozen.z - origin.z)
                    putLong("Age", (level.gameTime - frozen.frozenAt).coerceIn(0, 40))
                })
            }
            ship.getAttachment(MeteorShipAttachment::class.java)?.let { tag.putBoolean("MeteorTouchedGround", it.touchedGround) }
            ship.getAttachment(BalloonController::class.java)?.let { controller ->
                tag.put("Balloons", ListTag().also { balloons -> controller.balloons.values.forEach { balloon ->
                    balloons.add(CompoundTag().apply {
                        put("Regions", ListTag().also { regions -> balloon.regions.forEach { region ->
                            regions.add(CompoundTag().apply {
                                putLong("Min", BlockPos(region.minX(), region.minY(), region.minZ()).subtract(origin).asLong())
                                putLong("Max", BlockPos(region.maxX(), region.maxY(), region.maxZ()).subtract(origin).asLong())
                            })
                        } })
                        put("Gases", CompoundTag().also { gases -> balloon.gasMasses.forEach(gases::putDouble) })
                        putDouble("Energy", balloon.currentEnergy); putDouble("Volume", balloon.currentVolume)
                        putBoolean("Leaking", balloon.isLeaking)
                    })
                } })
            }
            if (!tag.isEmpty) {
                tag.putLong("Ship", id)
                entries.add(tag)
            }
        }
    }

    fun paste(level: ServerLevel, entries: ListTag, destinations: Map<Long, Pair<Long, BlockPos>>) {
        for (entry in entries) {
            val tag = entry as CompoundTag
            val (id, origin) = destinations[tag.getLong("Ship")] ?: continue
            val ship = level.shipObjectWorld.loadedShips.getById(id) ?: continue
            if (tag.contains("Frozen") && ship.isStatic) {
                val frozen = tag.getCompound("Frozen")
                ship.setAttachment(GravitronFrozenShip().apply {
                    x = origin.x + frozen.getDouble("X"); y = origin.y + frozen.getDouble("Y"); z = origin.z + frozen.getDouble("Z")
                    frozenAt = level.gameTime - frozen.getLong("Age").coerceIn(0, 40)
                })
            }
            if (tag.contains("MeteorTouchedGround")) ship.setAttachment(MeteorShipAttachment(tag.getBoolean("MeteorTouchedGround")))
            for (saved in tag.getList("Balloons", 10)) {
                val balloon = saved as CompoundTag
                val regions = balloon.getList("Regions", 10).mapTo(ArrayList()) {
                    val region = it as CompoundTag
                    val min = origin.offset(BlockPos.of(region.getLong("Min")))
                    val max = origin.offset(BlockPos.of(region.getLong("Max")))
                    AABBi(min.x, min.y, min.z, max.x, max.y, max.z)
                }
                val gases = balloon.getCompound("Gases")
                BalloonController.getOrCreate(ship).addBalloon(BalloonData(ArrayList(regions),
                    HashMap(gases.allKeys.associateWith(gases::getDouble)), balloon.getDouble("Energy"),
                    balloon.getDouble("Volume"), balloon.getBoolean("Leaking")))
            }
        }
    }
}
