package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.schematic.VdexWrapper
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class CopyableBlockGameTests {
    companion object {
        private val ships = mutableSetOf<Long>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val files = mutableListOf<Path>()

        @JvmStatic @AfterBatch(batch = "copyable_blocks")
        fun cleanup(level: ServerLevel) {
            ships.forEach { level.shipObjectWorld.allShips.getById(it)?.let { ship -> ShipAssembler.deleteShip(level, ship, true, false) } }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            files.forEach { Files.deleteIfExists(it) }
            ships.clear(); chunks.clear(); files.clear()
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "copyable_blocks", timeoutTicks = 240)
        fun vdexPastesLegacyCopyableBlocks(helper: GameTestHelper) {
            val level = helper.level
            val start = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 220, it.z) }
            val positions = listOf(start, start.east(2), start.east(5), start.east(8), start.east(11))
            positions.forEach { force(level, it) }
            val blocks = listOf(ClockworkBlocks.UNIVERSAL_SHAFT.get(), ClockworkBlocks.UNIVERSAL_SHAFT.get(),
                ClockworkBlocks.SPINOFF_BEARING.get(), ClockworkBlocks.PHYS_BEARING.get(), ClockworkBlocks.GIMBAL_BEARING.get())
            positions.zip(blocks).forEach { (pos, block) -> level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(FACING, Direction.UP)) }
            (level.getBlockEntity(start) as UniversalShaftBlockEntity)
                .connectTo(level.getBlockEntity(start.east(2)) as UniversalShaftBlockEntity)
            val ship = ShipAssembler.assembleToShip(level, positions.toSet(), 1.0)
            ship.isStatic = true
            ships.add(ship.id)
            val bounds = ship.shipAABB!!
            val first = BlockPos(bounds.minX(), bounds.minY(), bounds.minZ())
            positions.forEach { force(level, first.offset(it.subtract(start))) }
            val filename = "clockwork-copyable-test-${UUID.randomUUID()}"
            files.add(VdexWrapper.getSchematicDirectory(level.server).resolve("$filename.vdex"))
            var pastedId = -1L
            var pastedFirst = BlockPos.ZERO
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Source ship did not load")
            }.thenIdle(5).thenExecute {
                val firstEnd = level.getBlockEntity(first) as UniversalShaftBlockEntity
                helper.assertTrue(firstEnd.connectedPos == first.east(2), "Assembly lost the universal joint connection")
                VdexWrapper.saveShip(level, ship, filename)
                val before = level.shipObjectWorld.allShips.mapTo(HashSet()) { it.id }
                val paste = Vector3d(start.x.toDouble(), 240.0, start.z + 300.0)
                force(level, BlockPos.containing(paste.x, paste.y, paste.z))
                helper.assertTrue(VdexWrapper.loadShip(level, paste, filename, Quaterniond().rotationY(.4), false) == null,
                    "VDex rejected the schematic")
                val copy = level.shipObjectWorld.allShips.single { it.id !in before }
                pastedId = copy.id
                ships.add(copy.id)
                copy.isStatic = true
                val box = copy.shipAABB!!
                pastedFirst = BlockPos(box.minX(), box.minY(), box.minZ())
                positions.forEach { force(level, pastedFirst.offset(it.subtract(start))) }
            }.thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(pastedId) != null, "Pasted ship did not load")
                val firstEnd = level.getBlockEntity(pastedFirst) as? UniversalShaftBlockEntity
                val secondEnd = level.getBlockEntity(pastedFirst.east(2)) as? UniversalShaftBlockEntity
                helper.assertTrue(firstEnd?.connectedPos == pastedFirst.east(2) && secondEnd?.connectedPos == pastedFirst,
                    "Pasted universal joint still references the original endpoints")
            }.thenIdle(5).thenExecute {
                positions.zip(blocks).forEach { (pos, block) ->
                    helper.assertTrue(level.getBlockState(pastedFirst.offset(pos.subtract(start))).block == block,
                        "VDex did not restore $block")
                }
                helper.assertTrue((level.getBlockEntity(first) as UniversalShaftBlockEntity).connectedPos == first.east(2),
                    "Pasting changed the original universal joint")
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "copyable_blocks")
        fun legacyCallbacksUseCurrentPasteApi(helper: GameTestHelper) {
            val oldShip = 5_000_000_000L
            val newShip = oldShip + 1
            val ids = mapOf(oldShip to newShip)
            val centers: Map<Long, Pair<Vector3dc, Vector3dc>> = mapOf(oldShip to
                (Vector3d(100.5, 50.5, 200.5) to Vector3d(300.5, 80.5, 400.5)))
            val pos = BlockPos(300, 80, 400)
            val blocks = listOf(ClockworkBlocks.UNIVERSAL_SHAFT.get(), ClockworkBlocks.SPINOFF_BEARING.get(),
                ClockworkBlocks.PHYS_BEARING.get(), ClockworkBlocks.GIMBAL_BEARING.get())
            for (block in blocks) {
                val callback = block as ICopyableBlock
                val state = block.defaultBlockState()
                helper.assertTrue(callback.onPaste(helper.level, pos, state, ids, centers, emptyMap(), null) == null,
                    "$block did not accept an empty paste tag")
                val tag = CompoundTag()
                when (block) {
                    ClockworkBlocks.UNIVERSAL_SHAFT.get() -> {
                        tag.putInt("otherPosXa", 103); tag.putInt("otherPosYa", 52); tag.putInt("otherPosZa", 201)
                        tag.putLong("otherShipId", oldShip)
                    }
                    ClockworkBlocks.SPINOFF_BEARING.get() -> {
                        tag.putInt("partnerX", 103); tag.putInt("partnerY", 52); tag.putInt("partnerZ", 201)
                        tag.putLong("partnerShipId", oldShip); tag.putInt("jointId", 7)
                    }
                    ClockworkBlocks.PHYS_BEARING.get() -> {
                        tag.putVector3d("partnerPos", Vector3d(103.25, 52.75, 201.125))
                        tag.putLong("partnerShipId", oldShip); tag.putInt("jointId", 7)
                    }
                    else -> {
                        tag.putDouble("BearingPosSX", 103.25); tag.putDouble("BearingPosSY", 52.75); tag.putDouble("BearingPosSZ", 201.125)
                        tag.putLong("AttachedBlock", BlockPos(103, 52, 201).asLong())
                        tag.putLong("connectedShip", oldShip); tag.putInt("JointID", 7)
                    }
                }
                val pasted = callback.onPaste(helper.level, pos, state, ids, centers, mapOf(7 to 17), tag)
                helper.assertTrue(pasted != null, "$block discarded its connection data")
                when (block) {
                    ClockworkBlocks.UNIVERSAL_SHAFT.get() -> {
                        helper.assertTrue(tag.getLong("otherShipId") == newShip && tag.getInt("otherPosXr") == 3 &&
                            tag.getInt("otherPosYr") == 2 && tag.getInt("otherPosZr") == 1, "Universal joint did not remap its endpoint")
                    }
                    ClockworkBlocks.SPINOFF_BEARING.get() -> {
                        helper.assertTrue(tag.getLong("partnerShipId") == newShip && tag.getInt("jointId") == 17 &&
                            tag.getInt("partnerX") == 303 && tag.getInt("partnerY") == 82 && tag.getInt("partnerZ") == 401,
                            "Spinoff bearing did not remap its endpoint and joint")
                    }
                    ClockworkBlocks.PHYS_BEARING.get() -> {
                        helper.assertTrue(tag.getLong("partnerShipId") == newShip && tag.getInt("jointId") == 17 &&
                            tag.getVector3d("partnerPos")!!.distance(Vector3d(303.25, 82.75, 401.125)) < 1e-9,
                            "Physical bearing did not remap its precise anchor and joint")
                    }
                    else -> {
                        helper.assertTrue(tag.getLong("connectedShip") == newShip && tag.getInt("JointID") == 17 &&
                            tag.getDouble("BearingPosSX") == 303.25 && tag.getDouble("BearingPosSY") == 82.75 &&
                            tag.getDouble("BearingPosSZ") == 401.125 && BlockPos.of(tag.getLong("AttachedBlock")) == BlockPos(303, 82, 401),
                            "Gimbal did not remap its precise anchor, attachment block, and joint")
                    }
                }
            }
            helper.succeed()
        }
    }
}
