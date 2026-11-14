package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ExtendonGameTests {
    companion object {
        private const val BATCH = "extendon"
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableListOf<ServerShip>()
        private val forcedChunks = mutableSetOf<ChunkPos>()
        private val fixtureBlocks = mutableSetOf<BlockPos>()
        private val observers = mutableListOf<ShipTestObserver>()
        private var previousPhysicsSync = false

        @JvmStatic
        @BeforeBatch(batch = BATCH)
        fun configurePhysics(level: ServerLevel) {
            previousPhysicsSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic
        @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            fixtureBlocks.forEach { level.removeBlock(it, false) }
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            forcedChunks.forEach { level.setChunkForced(it.x, it.z, false) }
            fixtureBlocks.clear()
            ships.clear()
            forcedChunks.clear()
            observers.forEach { it.close() }
            observers.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousPhysicsSync
        }

        private fun fixturePosition(helper: GameTestHelper): BlockPos =
            helper.absolutePos(BlockPos(2, 0, 2)).atY(220)

        private fun forceChunk(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) forcedChunks.add(chunk)
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 360)
        fun extendonConnectsAndChangesLengthWithoutViolentCorrection(helper: GameTestHelper) {
            extendonStability(helper, false)
        }

        @JvmStatic
        @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 360)
        fun extendonPreservesRotatedShipAlignmentAndRestoresOldJoints(helper: GameTestHelper) {
            extendonStability(helper, true)
        }

        private fun extendonStability(helper: GameTestHelper, shipMounted: Boolean) {
            val level = helper.level
            val start = fixturePosition(helper)
            observers.add(ShipTestObserver(level, start))
            val top = start.above(8)
            fixtureBlocks.add(top)
            fixtureBlocks.add(start)
            fixtureBlocks.add(start.below())
            level.setBlockAndUpdate(top, ClockworkBlocks.EXTENDON.defaultState.setValue(FACING, Direction.DOWN))
            val host = if (shipMounted) {
                ShipAssembler.assembleToShip(level, setOf(top), 1.0).also {
                    it.isStatic = true
                    ships.add(it)
                }
            } else null
            val topPos = host?.shipAABB?.let { BlockPos(it.minX(), it.minY(), it.minZ()) } ?: top
            forceChunk(level, topPos)
            level.setBlockAndUpdate(start, ClockworkBlocks.EXTENDON.defaultState.setValue(FACING, Direction.UP))
            level.setBlockAndUpdate(start.below(), Blocks.NETHERITE_BLOCK.defaultBlockState())
            val child = ShipAssembler.assembleToShip(level, setOf(start, start.below()), 1.0)
            ships.add(child)
            child.isStatic = true
            val box = child.shipAABB!!
            val bottomPos = BlockPos(box.minX(), box.maxY() - 1, box.minZ())
            forceChunk(level, bottomPos)
            lateinit var upper: ExtendonBlockEntity
            lateinit var lower: ExtendonBlockEntity
            var initialRotation = Quaterniond()
            var lastLength = 8.0
            var contracted = 0.0
            var restoredLength = 0.0
            fun distance(): Double {
                val a = topPos.center.toJOML()
                host?.transform?.shipToWorld?.transformPosition(a)
                return child.transform.shipToWorld.transformPosition(bottomPos.center.toJOML()).distance(a)
            }
            fun checkMotion() {
                helper.assertTrue(child.velocity.isFinite && child.angularVelocity.isFinite, "Extendon produced non-finite motion")
                helper.assertTrue(child.velocity.length() < 14.0, "Extendon launched its payload: ${child.velocity}")
                helper.assertTrue(child.angularVelocity.length() < 3.0, "Extendon violently rotated its payload: ${child.angularVelocity}")
                val current = upper.distanceJoint!!
                val length = current.minDistance!!.toDouble()
                helper.assertTrue(abs(length - lastLength) <= .201, "Extendon target jumped from $lastLength to $length")
                helper.assertTrue((current.stiffness ?: 0f) > 0 && (current.damping ?: 0f) > 0, "Extendon lost its stiffness or damping")
                lastLength = length
            }
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(child.id) != null, "Extendon payload did not load")
                upper = level.getBlockEntity(topPos) as ExtendonBlockEntity
                lower = level.getBlockEntity(bottomPos) as ExtendonBlockEntity
            }.thenExecute {
                if (shipMounted) level.shipObjectWorld.teleportShip(child, vsCore.newShipTeleportData(
                    newPos = child.transform.positionInWorld, newRot = Quaterniond().rotateY(.8)))
            }.thenIdle(3).thenExecute {
                initialRotation = Quaterniond(child.transform.shipToWorldRotation)
                upper.connectTo(lower)
                val joint = upper.distanceJoint!!
                helper.assertTrue(abs(joint.minDistance!! - distance()) < .01, "Extendon did not start at its actual separation")
                val angular = upper.sphericalJoint!!
                val frame0 = Quaterniond(host?.transform?.shipToWorldRotation ?: Quaterniond()).mul(angular.pose0.rot)
                val frame1 = Quaterniond(child.transform.shipToWorldRotation).mul(angular.pose1.rot)
                helper.assertTrue(abs(frame0.dot(frame1)) > .9999, "Extendon starts with mismatched angular frames")
                lastLength = joint.minDistance!!.toDouble()
            }.thenWaitUntil {
                helper.assertTrue((upper.distanceJointId ?: -1) >= 0 && (upper.sphericalJointId ?: -1) >= 0,
                    "Extendon joints were not created")
            }.thenExecute {
                child.isStatic = false
            }.thenExecuteFor(65) {
                checkMotion()
            }.thenExecute {
                contracted = distance()
                helper.assertTrue(contracted < 3.0, "Extendon did not retract: $contracted")
                helper.assertTrue(abs(initialRotation.dot(child.transform.shipToWorldRotation)) > .98,
                    "Extendon forced the connected ship away from its original orientation")
                val kelvin = ClockworkMod.getKelvin(level)
                helper.assertTrue(kelvin.addGasAtTemperature(upper.getDuctNodePosition(), ClockworkGasses.HELIUM, 8.0, 300.0),
                    "Could not supply Extendon gas")
            }.thenExecuteFor(55) {
                checkMotion()
            }.thenExecute {
                helper.assertTrue(distance() > contracted + 2.0, "Extendon did not extend after receiving gas: ${distance()}")
                if (shipMounted) {
                    child.isStatic = true
                    upper.main = false
                    val legacy = upper.distanceJoint!!.copy(minDistance = .5f, maxDistance = 1000f, stiffness = null, damping = 1000f)
                    level.gtpa.updateJoint(upper.distanceJointId!!, legacy)
                }
            }.thenIdle(2).thenExecute {
                val saved = upper.saveWithoutMetadata()
                if (!shipMounted) {
                    saved.remove("DistanceJointId")
                    saved.remove("SphericalJointId")
                }
                upper.connectedJoint = null
                upper.connectedBe = null
                lower.connectedJoint = null
                lower.connectedBe = null
                upper.load(saved)
            }.thenIdle(3).thenExecute {
                helper.assertTrue(upper.connectedBe === lower && upper.distanceJoint!!.stiffness != null,
                    "Extendon failed to restore a damped connection")
                if (shipMounted) helper.assertTrue(abs(upper.distanceJoint!!.minDistance!! - distance()) < .7,
                    "Legacy Extendon did not restart from its actual separation")
                restoredLength = upper.distanceJoint!!.minDistance!!.toDouble()
                lastLength = restoredLength
            }.thenExecuteFor(12) {
                checkMotion()
            }.thenExecute {
                helper.assertTrue(upper.distanceJoint!!.minDistance!! > restoredLength + .5,
                    "Restored Extendon stopped responding to its gas supply")
                val distanceId = upper.distanceJointId!!
                val angularId = upper.sphericalJointId!!
                upper.disconnect()
                helper.runAfterDelay(3) {
                    helper.assertTrue(level.gtpa.getJointById(distanceId) == null && level.gtpa.getJointById(angularId) == null,
                        "Extendon left constraints behind after disconnecting")
                    helper.succeed()
                }
            }
        }
    }
}
