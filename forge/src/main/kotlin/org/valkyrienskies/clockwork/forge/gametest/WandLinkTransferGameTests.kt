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
import net.minecraft.world.level.block.Rotation
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.*
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexWrapper
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.util.relocateBlock
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class WandLinkTransferGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableListOf<ServerShip>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val links = mutableSetOf<UUID>()
        private val files = mutableListOf<Path>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = "wand_link_transfer")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "wand_link_transfer")
        fun cleanup(level: ServerLevel) {
            val data = WandLinks.get(level)
            val ids = ships.mapTo(HashSet()) { it.id }
            data.links.values.filter { it.id in links || it.a.shipId in ids || it.b.shipId in ids }.toList()
                .forEach { data.remove(level, it) }
            ships.forEach { level.shipObjectWorld.allShips.getById(it.id)?.let(level.shipObjectWorld::deleteShip) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            files.forEach { Files.deleteIfExists(it) }
            ships.clear(); chunks.clear(); links.clear(); files.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun springSurvivesAssemblyWeldingAndSeparation(helper: GameTestHelper) {
            val level = helper.level
            val base = base(helper)
            level.setBlockAndUpdate(base, Blocks.OAK_LOG.defaultBlockState())
            level.setBlockAndUpdate(base.east(4), Blocks.OAK_LOG.defaultBlockState())
            val data = WandLinks.get(level)
            val link = data.add(level, WandAnchor(base, Direction.NORTH), WandAnchor(base.east(4), Direction.NORTH),
                true, 4.0, physical = true, spring = true)!!
            links.add(link.id)
            var first: ServerShip? = null
            var second: ServerShip? = null
            var separated: ServerShip? = null
            var oldJoint: Int? = null
            var oldAlignment: Int? = null
            helper.startSequence().thenIdle(3).thenExecute {
                helper.assertTrue(data.links.containsKey(link.id) && link.jointId == null && link.alignmentId == null,
                    "World-to-world spring did not remain visual-only")
                first = assemble(level, setOf(base))
                first!!.isStatic = false
                helper.assertTrue(link.a.shipId == first!!.id && link.b.shipId == -1L, "Assembling one end did not remap its anchor")
            }.thenIdle(15).thenExecute {
                helper.assertTrue(link.jointId != null && link.alignmentId != null, "Assembled spring did not gain physics")
                second = assemble(level, setOf(base.east(4)))
                helper.assertTrue(link.b.shipId == second!!.id, "Assembling the other end lost its anchor")
            }.thenIdle(15).thenExecute {
                oldJoint = link.jointId; oldAlignment = link.alignmentId
                val failure = WanderwandWeld.weld(level, link.a.copy(face = Direction.UP), link.b.copy(face = Direction.WEST))
                helper.assertTrue(failure == null, "Weld failed: $failure")
                helper.assertTrue(data.links[link.id] === link && link.a.shipId == second!!.id && link.b.shipId == second!!.id,
                    "Welding broke the spring instead of preserving both endpoints")
                helper.assertTrue(!level.getBlockState(link.a.pos).isAir && !level.getBlockState(link.b.pos).isAir,
                    "Welded anchor points do not identify their blocks")
            }.thenIdle(10).thenExecute {
                helper.assertTrue(link.jointId == null && link.alignmentId == null && link.stress == 0f,
                    "Same-body spring retained a physics constraint")
                helper.assertTrue(oldJoint != null && level.gtpa.getJointById(oldJoint!!) == null &&
                    oldAlignment != null && level.gtpa.getJointById(oldAlignment!!) == null, "Welding left orphaned joints")
                separated = assemble(level, setOf(link.a.pos))
                separated!!.isStatic = false
            }.thenIdle(15).thenExecute {
                helper.assertTrue(link.a.shipId == separated!!.id && link.b.shipId == second!!.id,
                    "Separating the blocks did not redirect the visual spring")
                helper.assertTrue(link.jointId != null && link.alignmentId != null, "Separating the endpoints did not restore physics")
                val native = level.gtpa.getJointById(link.jointId!!)
                helper.assertTrue(native != null && setOf(native.shipId0, native.shipId1) == setOf(separated!!.id, second!!.id),
                    "Restored joint targets stale ship IDs")
                helper.assertTrue(link.length == 4.0 && data.links[link.id] === link, "Transfer changed spring identity or rest length")
                val face = link.a.face
                level.relocateBlock(link.a.pos, base, true, null, Rotation.CLOCKWISE_90)
                level.relocateBlock(link.b.pos, base.east(4), true, null, Rotation.NONE)
                helper.assertTrue(link.a.pos == base && link.b.pos == base.east(4) &&
                    link.a.shipId == -1L && link.b.shipId == -1L && link.a.face == Rotation.CLOCKWISE_90.rotate(face),
                    "Disassembly did not carry the anchor positions and rotated faces back into the world")
            }.thenIdle(10).thenExecute {
                helper.assertTrue(data.links[link.id] === link && link.jointId == null && link.alignmentId == null,
                    "Disassembling both endpoints did not preserve a visual-only spring")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesPhysicalRopes(helper: GameTestHelper) = schematic(helper, true, true, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesWinchRopes(helper: GameTestHelper) = schematic(helper, true, true, false, winch = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesSprings(helper: GameTestHelper) = schematic(helper, true, true, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesMagicalBinds(helper: GameTestHelper) = schematic(helper, true, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesMagicalAttachments(helper: GameTestHelper) = schematic(helper, false, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "wand_link_transfer", timeoutTicks = 240)
        fun vdexCopiesSameBodyVisualSprings(helper: GameTestHelper) = schematic(helper, true, true, true, sameBody = true)

        private fun schematic(helper: GameTestHelper, rope: Boolean, physical: Boolean, spring: Boolean, sameBody: Boolean = false, winch: Boolean = false) {
            val level = helper.level
            val base = base(helper)
            if (winch) {
                level.setBlockAndUpdate(base, ClockworkBlocks.ROPE_WINCH.defaultState)
                (level.getBlockEntity(base) as RopeWinchBlockEntity).maximum.setValue(48)
            }
            val root = assemble(level, if (sameBody) setOf(base, base.east()) else setOf(base))
            val other = if (sameBody) root else assemble(level, setOf(base.east(4)))
            val data = WandLinks.get(level)
            var source: WandLinks.Link? = null
            var pasted = emptyList<ServerShip>()
            var restored: WandLinks.Link? = null
            val filename = "clockwork-link-test-${UUID.randomUUID()}"
            val file = VdexWrapper.getSchematicDirectory(level.server).resolve("$filename.vdex")
            files.add(file)
            val pastePos = Vector3d(base.x.toDouble(), 240.0, base.z + 200.0)
            force(level, ChunkPos(BlockPos.containing(pastePos.x, pastePos.y, pastePos.z)))
            helper.startSequence().thenIdle(15).thenExecute {
                if (!sameBody) other.isStatic = false
                val a = WandAnchor(anchor(root), if (winch) Direction.DOWN else Direction.NORTH, root.id, winch)
                val b = WandAnchor(if (sameBody) a.pos.east() else anchor(other), Direction.NORTH, other.id)
                source = data.add(level, a, b, rope, if (sameBody) 1.0 else 4.0, physical, spring)
                helper.assertTrue(source != null, "Could not create schematic fixture link")
                links.add(source!!.id)
            }.thenWaitUntil {
                helper.assertTrue(sameBody || (source!!.jointId != null && (!spring || source!!.alignmentId != null)),
                    "Source joints have not been created")
            }.thenIdle(10).thenExecute {
                VdexWrapper.saveShip(level, root, filename)
                val saved = VdexIO.read(file)
                helper.assertTrue(saved.nbtData.containsKey(WandLinkSchematics.ENTRY), "VDex omitted Clockwork link data")
                helper.assertTrue(saved.metadata.constraints.isEmpty(), "VDex saved duplicate native joints for the link")
                helper.assertTrue(saved.metadata.ships.size == if (sameBody) 1 else 2, "VDex missed a connected ship")
                val before = level.shipObjectWorld.allShips.mapTo(HashSet()) { it.id }
                val failure = VdexWrapper.loadShip(level, pastePos, filename, Quaterniond().rotationY(Math.PI / 2), false)
                helper.assertTrue(failure == null, "VDex paste failed: $failure")
                pasted = level.shipObjectWorld.allShips.filter { it.id !in before }
                pasted.forEach { ships.add(it); force(level, it) }
            }.thenWaitUntil {
                val ids = pasted.mapTo(HashSet()) { it.id }
                restored = data.links.values.singleOrNull { it.a.shipId in ids && it.b.shipId in ids }
                helper.assertTrue(restored != null, "Pasted schematic did not restore its link")
            }.thenIdle(10).thenExecute {
                val link = restored!!
                links.add(link.id)
                helper.assertTrue(link.id != source!!.id && data.links[source!!.id] === source, "Pasting changed the source link")
                helper.assertTrue(link.rope == rope && link.physical == physical && link.spring == spring,
                    "Pasting changed the connection type")
                helper.assertTrue(link.length == source!!.length && link.save().getCompound("settings") == source!!.save().getCompound("settings"),
                    "Pasting lost the connection settings")
                helper.assertTrue(!level.getBlockState(link.a.pos).isAir && !level.getBlockState(link.b.pos).isAir,
                    "Pasted connection points are not on the copied blocks")
                if (physical) helper.assertTrue(level.getEntity(link.knotA!!) is RopeKnotEntity && level.getEntity(link.knotB!!) is RopeKnotEntity,
                    "Pasting without entities did not recreate endpoint hitboxes")
                if (winch) helper.assertTrue(link.a.winch && (level.getEntity(link.knotA!!) as RopeKnotEntity).winch &&
                    (level.getBlockEntity(link.a.pos) as? RopeWinchBlockEntity)?.maximum?.value == 48,
                    "Pasting lost the winch endpoint or its maximum length")
                val ids = pasted.mapTo(HashSet()) { it.id }
                val count = level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids }
                helper.assertTrue(count == if (sameBody) 0 else if (spring) 2 else 1, "Pasting produced $count joints instead of the expected count")
            }.thenSucceed()
        }

        private fun base(helper: GameTestHelper): BlockPos = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 220, it.z) }
        private fun anchor(ship: ServerShip): BlockPos = ship.shipAABB!!.let { BlockPos(it.minX(), it.minY(), it.minZ()) }
        private fun force(level: ServerLevel, pos: ChunkPos) { if (level.setChunkForced(pos.x, pos.z, true)) chunks.add(pos) }
        private fun force(level: ServerLevel, ship: ServerShip) {
            val bounds = ship.shipAABB!!
            for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4))
                force(level, ChunkPos(x, z))
        }
        private fun assemble(level: ServerLevel, positions: Set<BlockPos>): ServerShip {
            for (pos in positions) if (level.getBlockState(pos).isAir) level.setBlockAndUpdate(pos, Blocks.OAK_LOG.defaultBlockState())
            return ShipAssembler.assembleToShip(level, positions.map { it.mutable() }.toSet(), 1.0).also {
                it.isStatic = true; ships.add(it); force(level, it)
            }
        }
    }
}
