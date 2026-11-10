package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID

class SpringSettingsTest {
    @Test fun toolStepsSnapToCleanLengthsAndRespectBounds() {
        assertEquals(3.5, SpringSettings.adjustLength(3.234567, 1, false))
        assertEquals(3.3125, SpringSettings.adjustLength(3.234567, 1, true))
        assertEquals(0.5, SpringSettings.adjustLength(0.5, -1, true))
        assertEquals(128.0, SpringSettings.adjustLength(128.0, 1, false))
    }
    @Test fun settingsRoundTripAndLegacyDataGetsDefaults() {
        val settings = SpringSettings(2.0, 0.5, 0.25)
        assertEquals(settings, SpringSettings.load(settings.save()))
        assertEquals(SpringSettings(), SpringSettings.load(CompoundTag()))
        val invalid = CompoundTag().also { it.putDouble("stiffness", Double.NaN); it.putDouble("damping", -20.0); it.putDouble("twist", 500.0) }
        assertEquals(SpringSettings(1.0, 0.0, 4.0), SpringSettings.load(invalid))
        assertEquals(SpringSettings(), settings.reset(0).reset(1).reset(2))
    }
    @Test fun settingsReachBothJointsWithoutLockingAxialTravel() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val normal = SpringJoints.create(1, 2, pose, pose, 3.0, 500.0)
        val adjusted = SpringJoints.create(1, 2, pose, pose, 3.0, 500.0, SpringSettings(2.0, 0.5, 0.0))
        assertEquals(normal.first.stiffness * 2f, adjusted.first.stiffness)
        assertEquals(normal.first.damping!! * 0.5f, adjusted.first.damping)
        assertFalse(adjusted.second.drives!!.containsKey(VSD6Joint.D6Drive.TWIST))
        assertFalse(adjusted.second.drives!!.containsKey(VSD6Joint.D6Drive.X))
        for (axis in listOf(VSD6Joint.D6Drive.Y, VSD6Joint.D6Drive.Z)) {
            assertFalse(adjusted.second.drives!!.containsKey(axis))
        }
    }

    @Test fun clipboardPastesTuningByDefaultAndEasesToTheCopiedTargetLengthWithPasteAll() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val source = WandLinks.Link(UUID.randomUUID(), WandAnchor(BlockPos.ZERO, Direction.UP),
            WandAnchor(BlockPos(0, 5, 0), Direction.DOWN, 42), true, 5.0, pose, pose, true, true)
        source.settings = SpringSettings(2.0, 0.5, 0.25)
        source.appliedLength = 4.0
        val data = SpringClipboard.snapshot(source)
        val destination = source.copy(id = UUID.randomUUID(), length = 3.0)

        SpringClipboard.apply(data, destination, includeLength = false)
        assertEquals(source.settings, destination.settings)
        assertEquals(3.0, destination.length)

        SpringClipboard.apply(data, destination, includeLength = true)
        assertEquals(5.0, destination.length)
        assertEquals(3.0, destination.appliedLength)
        assertEquals(3.0625, SpringSettings.easeLength(destination.appliedLength, destination.length))
        assertEquals(5.0, SpringSettings.easeLength(destination.appliedLength, destination.length, 40.0))
    }
}
