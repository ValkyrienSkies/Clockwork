package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.ceil

internal class GatemakyrPairs : SavedData() {
    data class Mouth(val target: GatemakyrTarget, val startedAt: Long) {
        fun readyAt(): Long {
            if (!target.surface) return startedAt + 16
            val frame = target.localFrame()
            val impact = frame.impact.subtract(frame.position)
            val spread = GatemakyrRiftShape.spreadDuration(target.width, target.height, impact.dot(frame.u), impact.dot(frame.v))
            return startedAt + maxOf(16L, ceil(spread).toLong())
        }

        fun save() = CompoundTag().apply {
            putDouble("X", target.position.x)
            putDouble("Y", target.position.y)
            putDouble("Z", target.position.z)
            putString("Face", target.face.serializedName)
            putLong("Opened", startedAt)
            if (target.shipId >= 0) putLong("Carrier", target.shipId)
            if (target.surface) {
                putBoolean("Surface", true)
                putDouble("ImpactX", target.impact.x)
                putDouble("ImpactY", target.impact.y)
                putDouble("ImpactZ", target.impact.z)
            }
        }
    }
    val pairs = linkedMapOf<UUID, Pair<Mouth, Mouth>>()

    override fun save(tag: CompoundTag): CompoundTag = tag.apply {
        put("Pairs", ListTag().also { list ->
            for ((owner, pair) in pairs) list.add(CompoundTag().apply {
                putUUID("Owner", owner)
                putInt("Width", pair.first.target.width)
                putInt("Height", pair.first.target.height)
                put("A", pair.first.save())
                put("B", pair.second.save())
            })
        })
    }

    companion object {
        fun get(level: ServerLevel): GatemakyrPairs =
            level.dataStorage.computeIfAbsent(::load, ::GatemakyrPairs, "clockwork_gatemakyr_pairs")

        internal fun load(tag: CompoundTag) = GatemakyrPairs().apply {
            for (entry in tag.getList("Pairs", Tag.TAG_COMPOUND.toInt())) {
                val record = entry as CompoundTag
                val width = record.getInt("Width")
                val height = record.getInt("Height")
                if (!record.hasUUID("Owner") || width !in 1..32 || height !in 1..32) continue
                fun read(name: String): Mouth? {
                    val mouth = record.getCompound(name)
                    val face = Direction.byName(mouth.getString("Face")) ?: return null
                    val position = Vec3(mouth.getDouble("X"), mouth.getDouble("Y"), mouth.getDouble("Z"))
                    if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) return null
                    val carrier = if (mouth.contains("Carrier", Tag.TAG_LONG.toInt())) mouth.getLong("Carrier") else -1L
                    val surface = mouth.getBoolean("Surface")
                    val impact = if (surface) Vec3(mouth.getDouble("ImpactX"), mouth.getDouble("ImpactY"), mouth.getDouble("ImpactZ")) else position
                    if (!impact.x.isFinite() || !impact.y.isFinite() || !impact.z.isFinite()) return null
                    return Mouth(GatemakyrTarget(position, face, carrier, surface, width, height, impact), mouth.getLong("Opened"))
                }
                val a = read("A") ?: continue
                val b = read("B") ?: continue
                pairs[record.getUUID("Owner")] = a to b
            }
        }
    }
}
