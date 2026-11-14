package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class BearingAttachmentGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "bearing_attachment"
        private val ships = mutableListOf<ServerShip>()
        private val hosts = mutableListOf<BlockPos>()
        private val chunks = mutableSetOf<ChunkPos>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = BATCH)
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            hosts.forEach { level.removeBlock(it, false) }
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            hosts.clear(); ships.clear(); chunks.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 160)
        fun gimbalAttachesFlushToRotatedShip(helper: GameTestHelper) = attach(helper, true, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 160)
        fun physicalBearingUsesTheHitFace(helper: GameTestHelper) = attach(helper, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 160)
        fun physicalBearingKeepsItsAttachmentFrameInFollowAngle(helper: GameTestHelper) = attach(helper, false, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 160)
        fun physicalBearingReloadRemovesDuplicateJoints(helper: GameTestHelper) = attach(helper, false, false, true)

        private fun attach(helper: GameTestHelper, gimbal: Boolean, follow: Boolean, duplicate: Boolean = false) {
            val level = helper.level
            val base = BlockPos(22000 + hosts.size * 32, 220, 22000)
            hosts.add(base)
            force(level, base)
            val block = if (gimbal) ClockworkBlocks.GIMBAL_BEARING.get() else ClockworkBlocks.PHYS_BEARING.get()
            level.setBlockAndUpdate(base, block.defaultBlockState().setValue(FACING, Direction.EAST))
            level.setBlockAndUpdate(base.east(3), Blocks.STONE.defaultBlockState())
            val ship = ShipAssembler.assembleToShip(level, setOf(base.east(3)), 1.0)
            ships.add(ship)
            ship.isStatic = true
            val bounds = ship.shipAABB!!
            val target = BlockPos(bounds.minX(), bounds.minY(), bounds.minZ())
            force(level, target)
            val rotation = Quaterniond().rotationY(Math.PI / 2)
            level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(
                Vector3d(base.x + 1.75, base.y + .7, base.z + .7), rotation, Vector3d(), Vector3d()))
            var duplicateId: Int? = null
            lateinit var saved: CompoundTag
            fun aligned() {
                val face = ship.shipToWorld.transformPosition(Vector3d(target.x + .5, target.y + .5, target.z.toDouble()), Vector3d())
                val expected = Vector3d(base.x + .5 + (if (gimbal) .65 else .5), base.y + .5, base.z + .5)
                helper.assertTrue(face.distance(expected) < .08, "Attachment face floats away from bearing: face=$face, expected=$expected")
                val normal = ship.transform.shipToWorldRotation.transform(Vector3d(0.0, 0.0, -1.0))
                helper.assertTrue(normal.dot(Vector3d(-1.0, 0.0, 0.0)) > .995, "Bearing aligned the wrong local axis")
                helper.assertTrue(ship.velocity.isFinite && ship.velocity.length() < 2.0, "Attachment did not settle")
            }
            helper.startSequence().thenIdle(12).thenExecute {
                if (gimbal) (level.getBlockEntity(base) as GimbalBearingBlockEntity).apply {
                    maxAngleBehaviour!!.setValue(0)
                    assembleNextTick = true
                }
                else (level.getBlockEntity(base) as PhysBearingBlockEntity).assemble()
            }.thenWaitUntil {
                helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId0 == ship.id || it.shipId1 == ship.id }, "Bearing failed to attach the existing ship")
            }.thenExecute { ship.isStatic = false }.thenIdle(35).thenWaitUntil {
                aligned()
            }.thenExecute {
                val be = level.getBlockEntity(base)!!
                saved = be.saveWithoutMetadata()
                if (duplicate) {
                    val joint = level.gtpa.getJointById((be as PhysBearingBlockEntity).jointId)!!
                    level.gtpa.addJoint(joint) { id -> level.server.execute { duplicateId = id } }
                }
            }.thenWaitUntil {
                helper.assertTrue(!duplicate || duplicateId != null, "Duplicate joint was not created")
            }.thenExecute {
                if (duplicate) helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 == ship.id || it.shipId1 == ship.id } == 2,
                    "Duplicate fixture did not contain two joints")
                val be = level.getBlockEntity(base)!!
                be.load(saved)
                if (follow) (be as PhysBearingBlockEntity).movementMode!!.setValue(LockedMode.FOLLOW_ANGLE.ordinal)
            }.thenIdle(20).thenExecute {
                aligned()
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 == ship.id || it.shipId1 == ship.id } == 1,
                    "Reload did not retain exactly one bearing joint")
            }.thenSucceed()
        }
    }
}
