package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.*
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandGridRotation
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexWrapper
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.vsCore
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class HingeGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableSetOf<Long>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val links = mutableSetOf<UUID>()
        private val files = mutableListOf<Path>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = "hinges")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "hinges")
        fun cleanup(level: ServerLevel) {
            val data = HingeLinks.get(level)
            data.links.values.filter { it.id in links || it.a.shipId in ships || it.b.shipId in ships }.toList()
                .forEach { data.remove(level, it) }
            ships.forEach { level.shipObjectWorld.allShips.getById(it)?.let(level.shipObjectWorld::deleteShip) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            files.forEach { Files.deleteIfExists(it) }
            ships.clear(); chunks.clear(); links.clear(); files.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "hinges", timeoutTicks = 160)
        fun allMountingDirectionsAssembleWithoutSelfCollision(helper: GameTestHelper) {
            val level = helper.level
            val fixtures = ArrayList<HingeLinks.Link>()
            for ((index, facing) in Direction.values().withIndex()) {
                val pos = base(helper).offset(index * 5, 0, -100)
                force(level, ChunkPos(pos)); force(level, ChunkPos(pos.relative(facing)))
                level.setBlockAndUpdate(pos, ClockworkBlocks.HINGE.defaultState.setValue(HingeBlock.FACING, facing)
                    .setValue(HingeBlock.ROLL, index % 4))
                val error = HingeAssembly.assemble(level, pos)
                helper.assertTrue(error == null, "Could not assemble $facing hinge: $error")
                val link = HingeLinks.get(level).at(pos)!!
                remember(level, link); fixtures.add(link)
            }
            helper.startSequence().thenIdle(25).thenExecute {
                for (link in fixtures) {
                    helper.assertTrue(link.connected && link.jointId != null, "Hinge did not create a live joint")
                    val head = level.getBlockState(link.b.pos)
                    helper.assertTrue(head.block == ClockworkBlocks.HINGE_ATTACHMENT.get() &&
                        head.getCollisionShape(level, link.b.pos).isEmpty, "Attached hinge head retained a collision shape")
                    helper.assertTrue(!head.isSolid, "VS would use the attachment's outline as its collision shape")
                    helper.assertTrue(!level.getBlockState(link.a.pos).getCollisionShape(level, link.a.pos).isEmpty,
                        "The base lost its collision shape")
                    helper.assertTrue(HingeLinks.worldPoint(level, link.a)!!.distanceTo(HingeLinks.worldPoint(level, link.b)!!) < 0.1,
                        "The hinge halves do not share their pin center")
                }
                for (roll in 0..3) for (facing in Direction.values()) {
                    val state = ClockworkBlocks.HINGE.defaultState.setValue(HingeBlock.FACING, facing).setValue(HingeBlock.ROLL, roll)
                    for (grid in WandGridRotation.all) {
                        val transformed = grid.steps.fold(state) { s, step -> step.apply(s) }
                        helper.assertTrue(HingeGeometry.axis(transformed) == grid.face(HingeGeometry.axis(state)),
                            "Structure rotation lost the signed hinge axis")
                    }
                    for (attachment in listOf(false, true)) {
                        val half = (if (attachment) ClockworkBlocks.HINGE_ATTACHMENT else ClockworkBlocks.HINGE).defaultState
                            .setValue(HingeBlock.FACING, facing).setValue(HingeBlock.ROLL, roll)
                            .setValue(HingeBlock.ASSEMBLED, !attachment)
                        val shape = half.getCollisionShape(level, BlockPos.ZERO)
                        fun checkPoint(x: Double, y: Double, z: Double, solid: Boolean) {
                            val point = HingeGeometry.orientation(half).transform(Vector3d(x - 8, y - 8, z - 8))
                                .add(8.0, 8.0, 8.0).div(16.0)
                            helper.assertTrue(shape.toAabbs().any { it.contains(point.x, point.y, point.z) } == solid,
                                "Hinge collision does not match its plate/support: $half at $x,$y,$z")
                        }
                        checkPoint(3.5, if (attachment) 15.0 else 1.0, 3.5, true)
                        checkPoint(8.0, 8.0, 8.0, true)
                        checkPoint(3.5, 8.0, 3.5, false)
                    }
                }
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "hinges", timeoutTicks = 160)
        fun limitsAndPayloadNbtSurviveDisassembly(helper: GameTestHelper) {
            val level = helper.level
            val pos = base(helper)
            place(level, pos)
            level.setBlockAndUpdate(pos.above(), Blocks.BARREL.defaultBlockState())
            (level.getBlockEntity(pos.above()) as BarrelBlockEntity).customName = Component.literal("Hinge payload")
            helper.assertTrue(HingeAssembly.assemble(level, pos) == null, "Payload assembly failed")
            val link = HingeLinks.get(level).at(pos)!!
            remember(level, link)
            helper.startSequence().thenIdle(25).thenExecute {
                val be = level.getBlockEntity(pos) as HingeBlockEntity
                helper.assertTrue(HingeLinks.worldPoint(level, link.a)!!.distanceTo(HingeLinks.worldPoint(level, link.b)!!) < 0.1,
                    "Adding the payload shifted the hinge pivot")
                helper.assertTrue(be.lower.type != be.upper.type, "The two angle controls share a behaviour ID")
                be.lower.setValue(35); be.upper.setValue(110)
                helper.assertTrue(link.lower == -35 && link.upper == 110, "Angle limits were not updated independently")
            }.thenIdle(5).thenExecute {
                helper.assertTrue(link.connected && link.jointId != null, "Payload hinge broke before disassembly")
                val joint = level.gtpa.getJointById(link.jointId!!) as VSD6Joint
                helper.assertTrue(abs(joint.twistLimit!!.lowerLimit - Math.toRadians(-35.0)) < 1e-5 &&
                    abs(joint.twistLimit!!.upperLimit - Math.toRadians(110.0)) < 1e-5, "Live joint has stale angle limits")
                val error = HingeAssembly.disassemble(level, link)
                helper.assertTrue(error == null, "Disassembly failed: $error")
                helper.assertTrue((level.getBlockEntity(pos.above()) as? BarrelBlockEntity)?.customName?.string == "Hinge payload",
                    "Disassembly lost the payload block entity data")
                helper.assertTrue(!level.getBlockState(pos).getValue(HingeBlock.ASSEMBLED) && HingeLinks.get(level).at(pos) == null,
                    "Disassembly did not fold the hinge back into one block")
                helper.assertTrue(level.getBlockState(link.b.pos).isAir, "Disassembly left a duplicate head behind")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "hinges", timeoutTicks = 200)
        fun overloadBreaksAndNearbyEndsCanReconnect(helper: GameTestHelper) {
            val level = helper.level
            val pos = base(helper)
            place(level, pos)
            level.setBlockAndUpdate(pos.above(), Blocks.BARREL.defaultBlockState())
            helper.assertTrue(HingeAssembly.assemble(level, pos) == null, "Hinge assembly failed")
            val link = HingeLinks.get(level).at(pos)!!
            remember(level, link)
            var original = Vector3d()
            var rotation = Quaterniond()
            val loadTicks = AtomicInteger()
            val applied = AtomicInteger()
            val load = AtomicReference(2_000_000.0)
            helper.startSequence().thenIdle(25).thenExecute {
                helper.assertTrue(link.jointId != null, "Hinge never acquired a joint")
                val ship = level.shipObjectWorld.loadedShips.getById(link.b.shipId)!!
                helper.assertTrue(!ship.isStatic, "The hinge payload is static")
                original = Vector3d(ship.transform.positionInWorld)
                rotation = Quaterniond(ship.transform.shipToWorldRotation)
                loadTicks.set(120)
                ClockworkMod.physTickOnce(level.dimensionId) { physics, _, again ->
                    if (loadTicks.getAndDecrement() > 0) {
                        physics.getBodyById(ship.id)?.let {
                            it.applyWorldForce(Vector3d(0.0, load.get(), 0.0))
                            applied.incrementAndGet()
                        }
                        again()
                    }
                }
            }.thenIdle(12).thenExecute {
                helper.assertTrue(link.connected, "The strengthened hinge broke under twice its former force limit")
                load.set(HingeGeometry.MAX_FORCE * 20.0)
            }.thenWaitUntil {
                helper.assertTrue(!link.connected, "Overloading did not break the hinge after ${applied.get()} force applications; gap=${HingeLinks.worldPoint(level, link.a)!!.distanceTo(HingeLinks.worldPoint(level, link.b)!!)}")
                loadTicks.set(0)
            }.thenIdle(3).thenExecute {
                helper.assertTrue(!level.getBlockState(link.b.pos).getCollisionShape(level, link.b.pos).isEmpty,
                    "Broken attachment did not regain collision")
                val ship = level.shipObjectWorld.allShips.getById(link.b.shipId)!!
                helper.assertTrue(HingeAssembly.connect(level, link.a.pos, link.b.pos) == "far", "A distant hinge reconnected")
                level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(Vector3d(original).add(0.0, 2.25, 0.0), rotation, Vector3d(), Vector3d()))
            }.thenIdle(3).thenExecute {
                val error = HingeAssembly.connect(level, link.b.pos, link.a.pos)
                helper.assertTrue(error == null, "Nearby compatible ends could not reconnect: $error")
                remember(level, HingeLinks.get(level).at(pos)!!)
            }.thenIdle(15).thenExecute {
                val repaired = HingeLinks.get(level).at(pos)!!
                helper.assertTrue(repaired.connected && repaired.jointId != null, "Repaired hinge did not regain physics")
                helper.assertTrue(HingeLinks.worldPoint(level, repaired.a)!!.distanceTo(HingeLinks.worldPoint(level, repaired.b)!!) < 0.15,
                    "Repaired hinge did not pull its ends back together")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "hinges", timeoutTicks = 240)
        fun rotationStopsAtBothSignedLimits(helper: GameTestHelper) {
            val level = helper.level
            val pos = base(helper)
            place(level, pos)
            level.setBlockAndUpdate(pos.above(), Blocks.BARREL.defaultBlockState())
            val be = level.getBlockEntity(pos) as HingeBlockEntity
            be.lower.setValue(35); be.upper.setValue(55)
            helper.assertTrue(HingeAssembly.assemble(level, pos) == null, "Hinge assembly failed")
            val link = HingeLinks.get(level).at(pos)!!
            remember(level, link)
            val torque = AtomicReference(0.0)
            val ticks = AtomicInteger(600)
            helper.startSequence().thenIdle(25).thenExecute {
                torque.set(5_000.0)
                ClockworkMod.physTickOnce(level.dimensionId) { physics, _, again ->
                    if (ticks.getAndDecrement() > 0) {
                        physics.getBodyById(link.b.shipId)?.applyWorldTorque(Vector3d(torque.get(), 0.0, 0.0))
                        again()
                    }
                }
            }.thenWaitUntil {
                val angle = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
                helper.assertTrue(link.connected && angle in 50.0..60.0, "Hinge has not reached the positive stop: $angle")
            }.thenIdle(10).thenExecute {
                val angle = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
                helper.assertTrue(link.connected && angle in 50.0..60.0, "Positive limit did not stop the hinge: $angle, connected=${link.connected}")
                helper.assertTrue(HingeAssembly.disassemble(level, link) == "neutral", "A bent hinge disassembled")
                be.upper.setValue(45)
            }.thenIdle(20).thenExecute {
                val angle = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
                helper.assertTrue(link.connected && angle in -40.0..50.0,
                    "Changing the limit broke the hinge or left it outside the allowed range: $angle, connected=${link.connected}")
                torque.set(-5_000.0)
            }.thenWaitUntil {
                val angle = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
                helper.assertTrue(link.connected && angle in -40.0..-30.0, "Hinge has not reached the negative stop: $angle")
            }.thenIdle(10).thenExecute {
                ticks.set(0)
                val angle = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
                helper.assertTrue(link.connected && angle in -40.0..-30.0, "Negative limit did not stop the hinge: $angle")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "hinges", timeoutTicks = 240)
        fun assemblyAndVdexKeepHingeConnections(helper: GameTestHelper) {
            val level = helper.level
            val pos = base(helper)
            place(level, pos)
            helper.assertTrue(HingeAssembly.assemble(level, pos) == null, "Hinge assembly failed")
            val link = HingeLinks.get(level).at(pos)!!
            remember(level, link)
            var host: ServerShip? = null
            var pasted = emptySet<Long>()
            val filename = "clockwork-hinge-test-${UUID.randomUUID()}"
            val file = VdexWrapper.getSchematicDirectory(level.server).resolve("$filename.vdex")
            files.add(file)
            helper.startSequence().thenIdle(25).thenExecute {
                host = ShipAssembler.assembleToShip(level, setOf(pos.mutable()), 1.0)
                host!!.isStatic = true
                ships.add(host!!.id); force(level, host!!)
                helper.assertTrue(link.a.shipId == host!!.id, "Assembling the base did not remap its endpoint")
            }.thenIdle(15).thenExecute {
                helper.assertTrue(link.connected && link.jointId != null, "Base assembly broke the hinge")
                VdexWrapper.saveShip(level, host!!, filename)
                val saved = VdexIO.read(file)
                helper.assertTrue(saved.nbtData[WandLinkSchematics.ENTRY]?.getList("hinges", 10)?.size == 1,
                    "VDex did not save the hinge")
                helper.assertTrue(saved.metadata.constraints.isEmpty(), "VDex saved a duplicate native hinge joint")
                val before = level.shipObjectWorld.allShips.mapTo(HashSet()) { it.id }
                val paste = Vector3d(pos.x.toDouble(), 240.0, pos.z + 200.0)
                force(level, ChunkPos(BlockPos.containing(paste.x, paste.y, paste.z)))
                helper.assertTrue(VdexWrapper.loadShip(level, paste, filename, Quaterniond().rotationY(0.7), false) == null,
                    "VDex could not paste the hinge")
                val added = level.shipObjectWorld.allShips.filter { it.id !in before }
                pasted = added.mapTo(HashSet()) { it.id }
                added.forEach { ships.add(it.id); force(level, it) }
            }.thenWaitUntil {
                helper.assertTrue(HingeLinks.get(level).links.values.any { it.a.shipId in pasted && it.b.shipId in pasted && it.jointId != null },
                    "Pasted hinge did not restore its joint")
            }.thenExecute {
                val copy = HingeLinks.get(level).links.values.single { it.a.shipId in pasted && it.b.shipId in pasted }
                links.add(copy.id)
                helper.assertTrue(copy.id != link.id && copy.connected && copy.lower == link.lower && copy.upper == link.upper,
                    "Pasting lost hinge state or reused its identity")
                helper.assertTrue(level.getBlockState(copy.a.pos).block == ClockworkBlocks.HINGE.get() &&
                    level.getBlockState(copy.b.pos).block == ClockworkBlocks.HINGE_ATTACHMENT.get(), "Pasted endpoints do not point at hinge blocks")
            }.thenSucceed()
        }

        private fun base(helper: GameTestHelper) = helper.absolutePos(BlockPos(1, 1, 1)).let { BlockPos(it.x, 220, it.z) }
        private fun place(level: ServerLevel, pos: BlockPos) {
            force(level, ChunkPos(pos))
            level.setBlockAndUpdate(pos, ClockworkBlocks.HINGE.defaultState.setValue(HingeBlock.FACING, Direction.UP))
        }
        private fun remember(level: ServerLevel, link: HingeLinks.Link) {
            links.add(link.id)
            for (id in listOf(link.a.shipId, link.b.shipId).filter { it >= 0 }) {
                ships.add(id); level.shipObjectWorld.allShips.getById(id)?.let { force(level, it) }
            }
        }
        private fun force(level: ServerLevel, pos: ChunkPos) { if (level.setChunkForced(pos.x, pos.z, true)) chunks.add(pos) }
        private fun force(level: ServerLevel, ship: ServerShip) {
            val bounds = ship.shipAABB ?: return
            for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4))
                force(level, ChunkPos(x, z))
        }
    }
}
