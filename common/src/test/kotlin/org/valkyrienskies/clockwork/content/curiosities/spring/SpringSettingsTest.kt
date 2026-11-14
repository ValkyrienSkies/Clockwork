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
        assertEquals(SpringSettings(1.0, 0.0, 16.0), SpringSettings.load(invalid))
        assertEquals(SpringSettings(), settings.reset(0).reset(1).reset(2))
    }
    @Test fun expandedRangeSurvivesSavingAndToolAdjustment() {
        val maximum = SpringSettings(16.0, 4.0, 16.0)
        assertEquals(maximum, SpringSettings.load(maximum.save()))
        assertEquals(SpringSettings(4.0, 2.0, 4.0), SpringSettings.load(SpringSettings(4.0, 2.0, 4.0).save()))
        for (field in 0..2) {
            assertEquals(maximum, maximum.adjust(field, 1, false))
            assertEquals(maximum, maximum.adjust(field, -1, true).adjust(field, 1, true))
        }
        assertEquals(4.1, SpringSettings(stiffness = 4.0).adjust(0, 1, false).stiffness, 1e-9)
        assertEquals(2.1, SpringSettings(damping = 2.0).adjust(1, 1, false).damping, 1e-9)
        assertEquals(4.1, SpringSettings(twist = 4.0).adjust(2, 1, false).twist, 1e-9)
    }

    @Test fun rescaledSettingsReproducePreviousJointStrengthAndStress() {
        val pose = VSJointPose(Vector3d(), Quaterniond())
        val mass = 500.0
        for (old in listOf(SpringSettings(), SpringSettings(4.0, 2.0, 4.0), SpringSettings(0.25, 0.0, 0.25))) {
            val settings = SpringSettings(old.stiffness * 4, old.damping * 2, old.twist * 4)
            val (spring, alignment) = SpringJoints.create(1, 2, pose, pose, 3.0, mass, settings)
            assertEquals((mass * 350.0 * old.stiffness).toFloat(), spring.stiffness)
            assertEquals((mass * 28.0 * old.damping).toFloat(), spring.damping)
            val twist = alignment.drives!![VSD6Joint.D6Drive.TWIST]!!
            assertEquals((mass * 28.0 * old.twist).toFloat(), twist.driveStiffness)
            assertEquals((mass * 5.25 * old.damping).toFloat(), twist.driveDamping)
            assertEquals((2 * 350.0 * old.stiffness + 3 * 28.0 * old.damping) / 7200.0,
                SpringJoints.stressRatio(2.0, 3.0, settings), 1e-9)
        }
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
