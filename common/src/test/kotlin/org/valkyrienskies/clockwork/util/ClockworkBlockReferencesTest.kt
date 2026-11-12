package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import org.joml.Vector3d
import org.joml.Vector3dc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClockworkBlockReferencesTest {
    private val old = 5_000_000_000L
    private val fresh = old + 1
    private val source = BlockPos(-20000, 90, 30000)
    private val target = source.offset(4000, 20, -6000)
    private val centers: Map<Long, Pair<Vector3dc, Vector3dc>> = mapOf(old to
        (Vector3d(-20000.5, 80.5, 30000.5) to Vector3d(-16000.5, 100.5, 24000.5)))

    @Test fun remapsEveryStoredPositionAndJointWithoutTruncatingShipIds() {
        val tag = CompoundTag().apply {
            putLong("ShaftOrigin", source.asLong()); putLong("ShaftShip", old)
            putLong("RailOrigin", source.asLong()); putLong("RailShip", old)
            put("Controller", NbtUtils.writeBlockPos(source))
            put("shootingAtChute", NbtUtils.writeBlockPos(source))
            putInt("ConnectedPosX", source.x); putInt("ConnectedPosY", source.y); putInt("ConnectedPosZ", source.z)
            putInt("DistanceJointId", 41); putInt("SphericalJointId", 42)
        }
        tag.put("ClockworkReferences", CompoundTag().also { refs -> ClockworkBlockReferences.positions(tag).keys.forEach { refs.putLong(it, old) } })
        ClockworkBlockReferences.paste(tag, mapOf(old to fresh), centers, mapOf(41 to 61, 42 to 62))
        assertTrue(ClockworkBlockReferences.positions(tag).values.all { it == target })
        assertEquals(fresh, tag.getLong("ShaftShip")); assertEquals(fresh, tag.getLong("RailShip"))
        assertEquals(61, tag.getInt("DistanceJointId")); assertEquals(62, tag.getInt("SphericalJointId"))
        assertFalse(tag.contains("ClockworkReferences"))
    }

    @Test fun omittedBodiesDetachInsteadOfKeepingSourceReferences() {
        val tag = CompoundTag().apply {
            putLong("ShaftOrigin", source.asLong()); putLong("ShaftShip", old)
            put("Controller", NbtUtils.writeBlockPos(source))
            putInt("ConnectedPosX", source.x); putInt("ConnectedPosY", source.y); putInt("ConnectedPosZ", source.z)
            putBoolean("IsMain", true); putInt("DistanceJointId", 41)
        }
        ClockworkBlockReferences.paste(tag, emptyMap(), emptyMap(), emptyMap())
        assertTrue(ClockworkBlockReferences.positions(tag).isEmpty())
        assertFalse(tag.contains("ShaftShip")); assertFalse(tag.contains("DistanceJointId")); assertFalse(tag.contains("IsMain"))
    }

    @Test fun runtimeRegistrationsAndInFlightInfusionDoNotCarryIntoCopy() {
        val tag = CompoundTag().apply {
            putInt("PhysID", 19); put("edgeData", CompoundTag()); put("CondensedData", CompoundTag())
            for (key in listOf("Sequence", "Initiator", "Selection", "Regions", "Planned")) put(key, CompoundTag())
            putBoolean("Powered", true); putBoolean("Impacted", true)
            putInt("UserSetting", 73)
        }
        ClockworkBlockReferences.paste(tag, emptyMap(), emptyMap(), emptyMap())
        assertEquals(-1, tag.getInt("PhysID"))
        for (key in listOf("Sequence", "Initiator", "Selection", "Regions", "Planned", "edgeData", "CondensedData")) assertFalse(tag.contains(key), key)
        assertTrue(tag.getBoolean("Completed")); assertFalse(tag.getBoolean("Impacted"))
        assertTrue(tag.getBoolean("Powered")); assertEquals(73, tag.getInt("UserSetting"))
    }
}
