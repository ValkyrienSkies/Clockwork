package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.math.abs
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.primitives.AABBi
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableItem
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableRelayBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalLinearBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronFrozenShip
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.SugarRocketController
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeType
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing.SpinoffBearingBlockEntity
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.kelvin.api.edges.SmartEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexWrapper
import org.valkyrienskies.mod.common.shipObjectWorld

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class VdexFeatureGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "vdex_features"
        private val ships = mutableSetOf<Long>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val files = mutableListOf<Path>()
        private val starts = HashMap<GameTestHelper, BlockPos>()

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            level.allEntities.filterIsInstance<ControlledContraptionEntity>()
                .filter { level.getShipManagingPos(it.blockPosition())?.id in ships }.forEach { it.discard() }
            ships.forEach { level.shipObjectWorld.allShips.getById(it)?.let { ship -> ShipAssembler.deleteShip(level, ship, true, false) } }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            files.forEach { Files.deleteIfExists(it) }
            ships.clear(); chunks.clear(); files.clear(); starts.clear()
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
        }

        private fun remember(level: ServerLevel, ship: ServerShip) {
            ships.add(ship.id)
            val box = ship.shipAABB ?: return
            for (x in (box.minX() shr 4)..(box.maxX() shr 4)) for (z in (box.minZ() shr 4)..(box.maxZ() shr 4))
                force(level, BlockPos(x * 16, 0, z * 16))
        }

        private fun origin(ship: ServerShip): BlockPos = ship.shipAABB!!.let { BlockPos(it.minX(), it.minY(), it.minZ()) }
        private fun start(helper: GameTestHelper) = starts.getOrPut(helper) { BlockPos(10000 + starts.size * 256, 220, 10000) }
        private fun name(level: ServerLevel): String = "clockwork-vdex-test-${UUID.randomUUID()}".also {
            files.add(VdexWrapper.getSchematicDirectory(level.server).resolve("$it.vdex"))
        }

        private fun paste(helper: GameTestHelper, filename: String): List<ServerShip> {
            val level = helper.level
            val before = level.shipObjectWorld.allShips.mapTo(HashSet()) { it.id }
            val point = start(helper).south(300)
            force(level, point)
            helper.assertTrue(VdexWrapper.loadShip(level, Vector3d(point.x.toDouble(), 240.0, point.z.toDouble()), filename,
                Quaterniond().rotationY(.7), true) == null, "VDex paste failed")
            return level.shipObjectWorld.allShips.filter { it.id !in before }.onEach { remember(level, it) }
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledPhysicalBearingRoundTrip(helper: GameTestHelper) = bearingRoundTrip(helper, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun thinkableAssemblyAndVdexRestoreBothShipsAndOneRope(helper: GameTestHelper) = thinkableRoundTrip(helper, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun relayAssemblyAndVdexRestoreInputFaceAndCable(helper: GameTestHelper) = thinkableRoundTrip(helper, true)

        private fun thinkableRoundTrip(helper: GameTestHelper, relay: Boolean) {
            val level = helper.level
            val base = start(helper)
            val sensor = base.east(4)
            force(level, base)
            force(level, sensor)
            level.setBlockAndUpdate(base, if (relay) ClockworkBlocks.THINKABLE_RELAY.defaultState.setValue(FACING, Direction.UP)
                else ClockworkBlocks.COMBOBULATOR.defaultState)
            level.setBlockAndUpdate(sensor, ClockworkBlocks.ALT_METER.defaultState)
            if (!relay) {
                val controller = level.getBlockEntity(base) as CombobulatorBlockEntity
                controller.updateProgram(CombobulatorProgram.controller().copy(channels = listOf(
                    CombobulatorChannel(0, "Altitude", true, face = CombobulatorFace.TOP, thinkable = true),
                    CombobulatorChannel(1, "Offset", true, constant = 0.125),
                    CombobulatorChannel(3, "Output", false, face = CombobulatorFace.FRONT, device = true))), controller.revision)
            }
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, sensor, Direction.UP), WandAnchor.at(level, base, Direction.UP)), "Thinkable did not connect")
            val original = WandLinks.get(level).links.values.single { it.thinkable && it.b.pos == base }
            val host = ShipAssembler.assembleToShip(level, setOf(base), 1.0)
            val source = ShipAssembler.assembleToShip(level, setOf(sensor), 1.0)
            host.isStatic = true
            source.isStatic = true
            remember(level, host)
            remember(level, source)
            val filename = name(level)
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(original.a.shipId == source.id && original.b.shipId == host.id, "Assembly did not remap Thinkable endpoints")
                helper.assertTrue(original.jointId?.let { level.gtpa.getJointById(it) } != null, "Thinkable did not create its rope joint")
                val moved = level.getBlockEntity(original.b.pos) as? IThinkable
                helper.assertTrue(moved?.thinkableValue(Direction.NORTH)?.isFinite() == true, "Assembled Thinkable lost its reading")
            }.thenExecute {
                VdexWrapper.saveShip(level, host, filename)
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex did not include the Thinkable source ship")
            }.thenWaitUntil {
                val ids = copies.map { it.id }.toSet()
                val copied = WandLinks.get(level).links.values.filter { it.thinkable && it.a.shipId in ids && it.b.shipId in ids }
                helper.assertTrue(copied.size == 1, "VDex did not restore exactly one Thinkable")
                val link = copied.single()
                helper.assertTrue(link.id != original.id && link.length == original.length, "VDex lost Thinkable identity or length")
                helper.assertTrue(link.a.pos != original.a.pos && link.b.pos != original.b.pos, "VDex kept original endpoint positions")
                if (relay) {
                    val copiedRelay = level.getBlockEntity(link.b.pos) as? ThinkableRelayBlockEntity
                    val sourceValue = ThinkableItem.source(level, link.a)
                    helper.assertTrue(copiedRelay?.inputSide == Direction.UP, "VDex lost the relay input face")
                    helper.assertTrue(sourceValue != null && copiedRelay?.thinkableValue(Direction.NORTH) == sourceValue,
                        "Copied relay cannot read its copied source")
                } else {
                    val copiedController = level.getBlockEntity(link.b.pos) as CombobulatorBlockEntity
                    helper.assertTrue(copiedController.value(0).isFinite(), "Copied Combobulator cannot read its copied source")
                    helper.assertTrue(copiedController.value(3) == copiedController.value(0) + 0.125,
                        "VDex did not preserve the controller function and constant")
                }
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == 1,
                    "VDex did not restore exactly one Thinkable rope joint")
            }.thenIdle(10).thenExecute {
                val ids = copies.map { it.id }.toSet()
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == 1,
                    "Copied Thinkable duplicated its rope joint")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledGimbalRoundTrip(helper: GameTestHelper) = bearingRoundTrip(helper, true)

        private fun bearingRoundTrip(helper: GameTestHelper, gimbal: Boolean) {
            val level = helper.level
            val start = start(helper)
            force(level, start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val host = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            host.isStatic = true
            remember(level, host)
            val base = origin(host).east(2)
            val block = if (gimbal) ClockworkBlocks.GIMBAL_BEARING.get() else ClockworkBlocks.PHYS_BEARING.get()
            force(level, base)
            level.setBlockAndUpdate(base.west(), Blocks.STONE.defaultBlockState())
            level.setBlockAndUpdate(base, block.defaultBlockState().setValue(FACING, Direction.EAST))
            level.setBlockAndUpdate(base.east(), Blocks.STONE.defaultBlockState())
            val filename = name(level)
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(host.id) != null, "Bearing host did not load")
            }.thenIdle(5).thenExecute {
                if (gimbal) (level.getBlockEntity(base) as GimbalBearingBlockEntity).assembleNextTick = true
                else (level.getBlockEntity(base) as PhysBearingBlockEntity).assemble()
            }.thenWaitUntil {
                val connected = level.gtpa.getAllConnectedShips(host.id).filter { it != host.id }
                helper.assertTrue(connected.size == 1, "Bearing did not assemble its payload")
                level.shipObjectWorld.allShips.getById(connected.single())?.let { it.isStatic = true; remember(level, it) }
            }.thenExecute {
                VdexWrapper.saveShip(level, host, filename)
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex omitted bearing payload")
            }.thenWaitUntil {
                helper.assertTrue(copies.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Copied bearing ships did not load")
                val ids = copies.map { it.id }
                val copyHost = copies.firstOrNull { level.getBlockState(origin(it).east(2)).block == block }
                helper.assertTrue(copyHost != null, "Copied bearing block was lost")
                val tag = level.getBlockEntity(origin(copyHost!!).east(2))!!.saveWithoutMetadata()
                helper.assertTrue(tag.getLong(if (gimbal) "connectedShip" else "partnerShipId") in ids,
                    "Bearing kept an original ship reference")
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == 1,
                    "Copied bearing did not restore exactly one joint")
            }.thenIdle(10).thenExecute {
                val ids = copies.map { it.id }
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == 1,
                    "Copied bearing created duplicate joints after restoration")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledPropellerRoundTrip(helper: GameTestHelper) = contraptionRoundTrip(helper, ClockworkBlocks.BRASS_PROPELLER_BEARING.get())

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledCopterRoundTrip(helper: GameTestHelper) = contraptionRoundTrip(helper, ClockworkBlocks.COPTER_BEARING.get())

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledFlapRoundTrip(helper: GameTestHelper) = contraptionRoundTrip(helper, ClockworkBlocks.ANDESITE_FLAP_BEARING.get())

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledSmartFlapRoundTrip(helper: GameTestHelper) = contraptionRoundTrip(helper, ClockworkBlocks.SMART_FLAP_BEARING.get())

        private fun contraptionRoundTrip(helper: GameTestHelper, block: Block) {
            val level = helper.level
            val start = start(helper)
            force(level, start)
            level.setBlockAndUpdate(start, block.defaultBlockState().setValue(FACING, Direction.UP))
            val host = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            host.isStatic = true
            remember(level, host)
            val base = origin(host)
            level.setBlockAndUpdate(base.above(), AllBlocks.SAIL_FRAME.defaultState)
            val filename = name(level)
            var copy: ServerShip? = null
            fun contraption(pos: BlockPos): ControlledContraptionEntity? = when (val be = level.getBlockEntity(pos)) {
                is PropellerBearingBlockEntity -> be.propellerContraption
                is FlapBearingBlockEntity -> be.movedContraption
                else -> null
            }
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(host.id) != null, "Contraption host did not load")
            }.thenIdle(5).thenExecute {
                when (val bearing = level.getBlockEntity(base)) {
                    is PropellerBearingBlockEntity -> bearing.assemble()
                    is FlapBearingBlockEntity -> bearing.assemble()
                }
                helper.assertTrue(contraption(base) != null, "Source contraption did not assemble")
                VdexWrapper.saveShip(level, host, filename)
                copy = paste(helper, filename).single()
            }.thenWaitUntil {
                helper.assertTrue(copy != null, "Contraption paste was not reached")
                val target = origin(copy!!)
                val entity = contraption(target)
                helper.assertTrue(entity != null, "Copied bearing did not restore its contraption entity")
                helper.assertTrue(entity !== contraption(base), "Copy retained the original contraption")
                helper.assertTrue(entity!!.contraption.anchor == target.above(), "Contraption kept its original assembly anchor")
                helper.assertTrue(entity.contraption.blocks.isNotEmpty(), "Contraption payload was lost")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledPistonRoundTrip(helper: GameTestHelper) = linearRoundTrip(helper, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun spinoffConnectionRoundTrip(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            val pair = listOf(start, start.east()).mapIndexed { index, pos ->
                force(level, pos)
                level.setBlockAndUpdate(pos, ClockworkBlocks.SPINOFF_BEARING.defaultState.setValue(FACING,
                    if (index == 0) Direction.EAST else Direction.WEST))
                ShipAssembler.assembleToShip(level, setOf(pos), 1.0).also { it.isStatic = true; remember(level, it) }
            }
            val filename = name(level)
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.gtpa.getAllConnectedShips(pair[0].id).contains(pair[1].id), "Spinoff bearings did not connect")
            }.thenExecute {
                VdexWrapper.saveShip(level, pair[0], filename)
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex omitted a spinoff bearing endpoint")
            }.thenWaitUntil {
                helper.assertTrue(copies.size == 2 && copies.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Copied spinoff ships did not load")
                val ends = copies.map { level.getBlockEntity(origin(it)) as SpinoffBearingBlockEntity }
                helper.assertTrue(ends[0].partnerPos == ends[1].blockPos && ends[1].partnerPos == ends[0].blockPos,
                    "Spinoff bearings point outside the copy")
                val ids = copies.map { it.id }
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == 1,
                    "Copied spinoff bearings did not restore exactly one joint")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun gasTankContentsAndControllerRoundTrip(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            force(level, start)
            level.setBlockAndUpdate(start, ClockworkBlocks.DUCT_TANK.defaultState)
            val ship = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            ship.isStatic = true
            remember(level, ship)
            val base = origin(ship)
            level.setBlockAndUpdate(base.above(), ClockworkBlocks.DUCT_TANK.defaultState)
            val filename = name(level)
            var copy: ServerShip? = null
            helper.startSequence().thenWaitUntil {
                val lower = level.getBlockEntity(base) as DuctTankBlockEntity
                val upper = level.getBlockEntity(base.above()) as DuctTankBlockEntity
                helper.assertTrue(upper.controller == lower.controller && lower.height == 2 && lower.ensureNodeExists(),
                    "Source tanks did not form a multiblock")
            }.thenExecute {
                val tank = level.getBlockEntity(base) as DuctTankBlockEntity
                val info = ClockworkMod.getKelvin(level).nodeInfo.getValue(tank.getDuctNodePosition())
                info.currentGasMasses.clear()
                info.currentGasMasses[ClockworkGasses.HELIUM] = .1
                VdexWrapper.saveShip(level, ship, filename)
                copy = paste(helper, filename).single()
            }.thenWaitUntil {
                helper.assertTrue(copy != null, "Tank paste was not reached")
                val destination = origin(copy!!)
                val lower = level.getBlockEntity(destination) as DuctTankBlockEntity
                val upper = level.getBlockEntity(destination.above()) as DuctTankBlockEntity
                helper.assertTrue(lower.controller == destination && upper.controller == destination && lower.height == 2,
                    "Tank controller still points outside the copied multiblock")
                val mass = ClockworkMod.getKelvin(level).getGasMassAt(lower.getDuctNodePosition())[ClockworkGasses.HELIUM] ?: 0.0
                helper.assertTrue(abs(mass - .1) < 1e-6, "Copied gas contents changed: $mass")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun assembledGantryRoundTrip(helper: GameTestHelper) = linearRoundTrip(helper, false)

        private fun linearRoundTrip(helper: GameTestHelper, piston: Boolean) {
            val level = helper.level
            val base = start(helper)
            val states = if (piston) mapOf(
                base to ClockworkBlocks.PHYS_PISTON.defaultState.setValue(FACING, Direction.EAST),
                base.west() to AllBlocks.PISTON_EXTENSION_POLE.defaultState.setValue(FACING, Direction.EAST),
                base.east() to Blocks.STONE.defaultBlockState()) else mapOf(
                base to ClockworkBlocks.PHYS_GANTRY.defaultState.setValue(FACING, Direction.EAST).setValue(AXIS_ALONG_FIRST_COORDINATE, false),
                base.west() to AllBlocks.GANTRY_SHAFT.defaultState.setValue(FACING, Direction.UP))
            states.forEach { (pos, state) -> force(level, pos); level.setBlockAndUpdate(pos, state) }
            val host = ShipAssembler.assembleToShip(level, states.keys, 1.0)
            host.isStatic = true
            remember(level, host)
            val mechanism = origin(host).east()
            val filename = name(level)
            var originals = emptyList<ServerShip>()
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(host.id) != null, "Actuator host did not load")
            }.thenIdle(5).thenExecute {
                val before = level.shipObjectWorld.allShips.mapTo(HashSet()) { it.id }
                val state = level.getBlockState(mechanism)
                state.use(level, helper.makeMockPlayer(), InteractionHand.MAIN_HAND,
                    BlockHitResult(mechanism.center, Direction.NORTH, mechanism, false))
                val added = level.shipObjectWorld.allShips.filter { it.id !in before }
                helper.assertTrue(added.size == 1, "Actuator did not assemble")
                added.forEach { remember(level, it) }
                originals = listOf(host) + added
            }.thenWaitUntil {
                helper.assertTrue(originals.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Assembled actuator did not load")
                helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId0 in originals.map { s -> s.id } && it.shipId1 in originals.map { s -> s.id } },
                    "Assembled actuator did not create a joint")
            }.thenExecute {
                VdexWrapper.saveShip(level, host, filename)
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex omitted half the actuator")
            }.thenWaitUntil {
                helper.assertTrue(copies.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Copied actuator did not load")
                val ids = copies.map { it.id }
                val blocks = copies.flatMap { ship ->
                    val box = ship.shipAABB!!
                    BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX() - 1, box.maxY() - 1, box.maxZ() - 1)
                        .mapNotNull { level.getBlockEntity(it) as? PhysicalLinearBlockEntity }
                }
                helper.assertTrue(blocks.size == 1, "Copied actuator block entity was lost")
                val tag = blocks[0].saveWithoutMetadata()
                helper.assertTrue(tag.contains(if (piston) "ShaftOrigin" else "RailOrigin"), "Copied actuator lost assembly state")
                val anchor = BlockPos.of(tag.getLong(if (piston) "ShaftOrigin" else "RailOrigin"))
                helper.assertTrue(level.getShipManagingPos(anchor)?.id in ids &&
                    tag.getLong(if (piston) "ShaftShip" else "RailShip") in ids, "Actuator retained an original endpoint")
                helper.assertTrue(level.gtpa.getAllJoints().values.any { it.shipId0 in ids && it.shipId1 in ids }, "Copied actuator did not rebuild its joint")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun hosePortsRoundTrip(helper: GameTestHelper) = gasConnectionRoundTrip(helper, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun extendonsRoundTrip(helper: GameTestHelper) = gasConnectionRoundTrip(helper, true)

        private fun gasConnectionRoundTrip(helper: GameTestHelper, extendon: Boolean) {
            val level = helper.level
            val start = start(helper)
            val block = if (extendon) ClockworkBlocks.EXTENDON.get() else ClockworkBlocks.HOSE_PORT.get()
            val pair = listOf(start, start.east(3)).mapIndexed { index, pos ->
                force(level, pos)
                level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(FACING, if (index == 0) Direction.EAST else Direction.WEST))
                ShipAssembler.assembleToShip(level, setOf(pos), 1.0).also { it.isStatic = true; remember(level, it) }
            }
            val filename = name(level)
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(pair.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Gas connection ships did not load")
            }.thenIdle(5).thenExecute {
                (level.getBlockEntity(origin(pair[0])) as IUniversalJoint).connectTo(level.getBlockEntity(origin(pair[1])) as IUniversalJoint)
            }.thenIdle(5).thenExecute {
                VdexWrapper.saveShip(level, pair[0], filename)
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex omitted a gas connection endpoint")
            }.thenWaitUntil {
                helper.assertTrue(copies.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Copied gas connection did not load")
                val ends = copies.map { level.getBlockEntity(origin(it)) as IUniversalJoint }
                helper.assertTrue(ends[0].connectedJoint?.pos == ends[1].pos && ends[1].connectedJoint?.pos == ends[0].pos,
                    "Gas connection endpoints were not remapped")
                val ids = copies.map { it.id }
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == if (extendon) 2 else 1,
                    "Gas connection did not recreate its physical joints")
            }.thenIdle(10).thenExecute {
                val ids = copies.map { it.id }
                helper.assertTrue(level.gtpa.getAllJoints().values.count { it.shipId0 in ids && it.shipId1 in ids } == if (extendon) 2 else 1,
                    "Gas connection lost or duplicated its restored joints")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
        fun registeredBlocksAndSettingsSurviveVdex(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            val states = BuiltInRegistries.BLOCK.filter {
                val id = BuiltInRegistries.BLOCK.getKey(it)
                id.namespace == ClockworkMod.MOD_ID && id.path != "asteroid_block"
            }.sortedBy { BuiltInRegistries.BLOCK.getKey(it).path }.map { it.defaultBlockState() }
            helper.assertTrue(states.size > 50, "Registry audit did not include Clockwork's blocks")
            force(level, start)
            level.setBlockAndUpdate(start, Blocks.STONE.defaultBlockState())
            val ship = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            ship.isStatic = true
            remember(level, ship)
            val corner = origin(ship)
            val offsets = states.indices.map { BlockPos(2 + (it % 12) * 3, 1, (it / 12) * 3) }
            for (pos in BlockPos.betweenClosed(corner, corner.offset(35, 0, offsets.maxOf { it.z }))) {
                force(level, pos)
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState())
            }
            offsets.zip(states).forEach { (offset, state) ->
                val pos = corner.offset(offset)
                force(level, pos)
                if (state.block == ClockworkBlocks.DELIVERY_CANNON.get() || state.block == ClockworkBlocks.DELIVERY_CHUTE.get())
                    level.setBlockAndUpdate(pos.below(), AllBlocks.DEPOT.defaultState)
                if (state.block == ClockworkBlocks.STEAM_GENERATOR.get())
                    level.setBlockAndUpdate(pos.below(), AllBlocks.FLUID_TANK.defaultState)
                level.setBlockAndUpdate(pos, state)
            }
            fun pos(block: Block) = corner.offset(offsets[states.indexOfFirst { it.block == block }])
            level.setBlockAndUpdate(pos(ClockworkBlocks.DUCT.get()).east(), ClockworkBlocks.DUCT.defaultState)
            level.setBlockAndUpdate(pos(ClockworkBlocks.DUCT.get()).west(), ClockworkBlocks.DUCT.defaultState)
            val filename = name(level)
            var copy: ServerShip? = null
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Source did not load")
            }.thenIdle(5).thenExecute {
                helper.assertTrue(level.getBlockEntity(pos(ClockworkBlocks.ROPE_WINCH.get())) is RopeWinchBlockEntity,
                    "Source winch missing at ${pos(ClockworkBlocks.ROPE_WINCH.get())}: ${level.getBlockState(pos(ClockworkBlocks.ROPE_WINCH.get()))}, bounds=${ship.shipAABB}")
                (level.getBlockEntity(pos(ClockworkBlocks.ROPE_WINCH.get())) as RopeWinchBlockEntity).maximum.setValue(73)
                (level.getBlockEntity(pos(ClockworkBlocks.HINGE.get())) as HingeBlockEntity).let { it.lower.setValue(37); it.upper.setValue(112) }
                (level.getBlockEntity(pos(ClockworkBlocks.BLADE_CONTROLLER.get())) as BladeControllerBlockEntity)
                    .setItem(2, ItemStack(ClockworkItems.PROPELLER_BLADE.get()))
                (level.getBlockEntity(pos(ClockworkBlocks.DELIVERY_CANNON.get())) as DeliveryCannonBlockEntity).let {
                    it.currentStack = ItemStack(Items.IRON_INGOT, 7)
                    it.midAirStack = ItemStack(Items.DIAMOND, 3)
                }
                val duct = level.getBlockEntity(pos(ClockworkBlocks.DUCT.get())) as DuctBlockEntity
                for ((direction, type) in listOf(Direction.EAST to DuctEdgeType.SMART, Direction.WEST to DuctEdgeType.FILTERED)) {
                    val other = level.getBlockEntity(duct.blockPos.relative(direction)) as DuctBlockEntity
                    val settings = CompoundTag().apply {
                        if (type == DuctEdgeType.SMART) {
                            putInt("filter", SmartEdge.FilterType.PRESSURE.ordinal)
                            putDouble("comparisonValue", 234567.0); putBoolean("moreThan", true)
                        } else {
                            putString("filtered", ClockworkGasses.HELIUM.resourceLocation.toString()); putBoolean("blacklist", true)
                        }
                    }
                    for ((a, b, side) in listOf(Triple(duct, other, direction), Triple(other, duct, direction.opposite))) {
                        a.DIR_TO_CONNECTION_TYPE[side] = type
                        a.edgeData[DuctBlockEntity.EdgePos(a.getDuctNodePosition(), b.getDuctNodePosition())] = settings.copy()
                        a.shouldUpdateEdges = true
                    }
                }
                val wand = ItemStack(ClockworkItems.WANDERWAND.get())
                wand.orCreateTag.put("selectedBlocks", WanderwandItem.writeAABBSetToNBT(arrayListOf(
                    AABBi(corner.x, corner.y, corner.z, corner.x + 1, corner.y + 1, corner.z + 1))))
                wand.orCreateTag.putString("selectionDimension", level.dimension().location().toString())
                (level.getBlockEntity(pos(ClockworkBlocks.PHYSICS_INFUSER.get())) as PhysicsInfuserBlockEntity).setItem(0, wand)
                VdexWrapper.saveShip(level, ship, filename)
                copy = paste(helper, filename).single()
            }.thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(copy?.id ?: -1L) != null, "Copy did not load")
            }.thenIdle(25).thenExecute {
                val destination = origin(copy ?: return@thenExecute)
                for ((index, state) in states.withIndex()) {
                    val target = destination.offset(offsets[index])
                    helper.assertTrue(level.getBlockState(target).block == state.block, "VDex lost ${BuiltInRegistries.BLOCK.getKey(state.block)}")
                    if (state.hasBlockEntity()) helper.assertTrue(level.getBlockEntity(target)?.type == level.getBlockEntity(corner.offset(offsets[index]))?.type,
                        "VDex lost the block entity for ${BuiltInRegistries.BLOCK.getKey(state.block)}")
                }
                fun copied(block: Block) = destination.offset(pos(block).subtract(corner))
                helper.assertTrue((level.getBlockEntity(copied(ClockworkBlocks.ROPE_WINCH.get())) as RopeWinchBlockEntity).maximum.value == 73, "Winch maximum reset")
                (level.getBlockEntity(copied(ClockworkBlocks.HINGE.get())) as HingeBlockEntity).let {
                    helper.assertTrue(it.lower.value == 37 && it.upper.value == 112, "Hinge limits reset")
                }
                helper.assertTrue(!(level.getBlockEntity(copied(ClockworkBlocks.BLADE_CONTROLLER.get())) as BladeControllerBlockEntity).getItem(2).isEmpty,
                    "Blade inventory disappeared")
                (level.getBlockEntity(copied(ClockworkBlocks.DELIVERY_CANNON.get())) as DeliveryCannonBlockEntity).let {
                    helper.assertTrue(it.currentStack.`is`(Items.IRON_INGOT) && it.currentStack.count == 7 &&
                        it.midAirStack.`is`(Items.DIAMOND) && it.midAirStack.count == 3, "Delivery cannon inventories were overwritten")
                }
                val copiedDuct = level.getBlockEntity(copied(ClockworkBlocks.DUCT.get())) as DuctBlockEntity
                val a = copiedDuct.getDuctNodePosition()
                val smart = copiedDuct.edgeData[DuctBlockEntity.EdgePos(a, copiedDuct.blockPos.east().toDuctNodePos(level.dimension().location()))]
                val filtered = copiedDuct.edgeData[DuctBlockEntity.EdgePos(a, copiedDuct.blockPos.west().toDuctNodePos(level.dimension().location()))]
                helper.assertTrue(smart?.getDouble("comparisonValue") == 234567.0 && smart.getBoolean("moreThan"), "Smart duct settings reset")
                helper.assertTrue(filtered?.getString("filtered") == ClockworkGasses.HELIUM.resourceLocation.toString() && filtered.getBoolean("blacklist"), "Duct filter settings reset")
                val edges = ClockworkMod.getKelvin(level).edges
                val east = copiedDuct.blockPos.east().toDuctNodePos(level.dimension().location())
                val west = copiedDuct.blockPos.west().toDuctNodePos(level.dimension().location())
                val smartEdge = (edges[a to east] ?: edges[east to a]) as? SmartEdge
                val filterEdge = (edges[a to west] ?: edges[west to a]) as? FilteredEdge
                helper.assertTrue(smartEdge?.comparisonValue == 234567.0 && smartEdge.moreThan, "Smart duct did not apply its restored settings")
                helper.assertTrue(filterEdge?.blacklist == true && filterEdge.filter.any { it.resourceLocation == ClockworkGasses.HELIUM.resourceLocation },
                    "Filtered duct did not apply its restored settings")
                val infuser = level.getBlockEntity(copied(ClockworkBlocks.PHYSICS_INFUSER.get())) as PhysicsInfuserBlockEntity
                val selected = WanderwandItem.readAABBSetFromNBT(infuser.getItem(0).tag!!.getCompound("selectedBlocks"))
                helper.assertTrue(selected.size == 1 && selected[0].minX() == destination.x && selected[0].minZ() == destination.z,
                    "Infuser wand still selects the original ship")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun burningRocketRoundTrip(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            force(level, start)
            level.setBlockAndUpdate(start, ClockworkBlocks.SUGAR_ROCKET.defaultState)
            val ship = ShipAssembler.assembleToShip(level, setOf(start), 1.0)
            ship.isStatic = true
            remember(level, ship)
            val filename = name(level)
            var copy: ServerShip? = null
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Rocket ship did not load")
            }.thenExecute {
                (level.getBlockEntity(origin(ship)) as SugarRocketBlockEntity).ignite(1000, 3)
                VdexWrapper.saveShip(level, ship, filename)
                copy = paste(helper, filename).single()
            }.thenWaitUntil {
                val loaded = level.shipObjectWorld.loadedShips.getById(copy?.id ?: -1L)
                val target = copy?.let(::origin)
                val rocket = target?.let { level.getBlockEntity(it) as? SugarRocketBlockEntity }
                helper.assertTrue(rocket?.isBurning == true && rocket.burnTime == 1000 && rocket.burnPower == 3,
                    "Rocket burn state did not survive VDex")
                val controller = loaded?.getAttachment(SugarRocketController::class.java)
                helper.assertTrue(controller?.burningRockets?.any {
                    it.position?.let { position -> position.x() == target!!.x && position.y() == target.y && position.z() == target.z } == true
                } == true, "Pasted burning rocket did not restore thrust")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun universalShaftDiscoversBothShips(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            val pair = listOf(start, start.east(3)).map { pos ->
                force(level, pos)
                level.setBlockAndUpdate(pos, ClockworkBlocks.UNIVERSAL_SHAFT.defaultState)
                ShipAssembler.assembleToShip(level, setOf(pos), 1.0).also { it.isStatic = true; remember(level, it) }
            }
            val filename = name(level)
            var copies = emptyList<ServerShip>()
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(pair.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Shaft ships did not load")
            }.thenExecute {
                (level.getBlockEntity(origin(pair[0])) as UniversalShaftBlockEntity).connectTo(
                    level.getBlockEntity(origin(pair[1])) as UniversalShaftBlockEntity)
                VdexWrapper.saveShip(level, pair[0], filename)
                helper.assertTrue(VdexIO.read(VdexWrapper.getSchematicDirectory(level.server).resolve("$filename.vdex")).metadata.ships.size == 2, "VDex omitted the ship connected only by a universal shaft")
                copies = paste(helper, filename)
                helper.assertTrue(copies.size == 2, "VDex did not paste both ships")
            }.thenWaitUntil {
                helper.assertTrue(copies.all { level.shipObjectWorld.loadedShips.getById(it.id) != null }, "Copied shafts did not load")
                val ends = copies.map { level.getBlockEntity(origin(it)) as UniversalShaftBlockEntity }
                helper.assertTrue(ends[0].connectedPos == ends[1].blockPos && ends[1].connectedPos == ends[0].blockPos, "Copied shafts point outside the copy")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
        fun lubricantAndShipAttachmentsRoundTrip(helper: GameTestHelper) {
            val level = helper.level
            val start = start(helper)
            val shell = BlockPos.betweenClosed(start, start.offset(2, 2, 2)).map { it.immutable() }.filter { it != start.offset(1, 1, 1) }
            shell.forEach { force(level, it); level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState()) }
            val ship = ShipAssembler.assembleToShip(level, shell.toSet(), 1.0)
            ship.isStatic = true
            remember(level, ship)
            val corner = origin(ship)
            val filename = name(level)
            var copy: ServerShip? = null
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Source did not load")
            }.thenExecute {
                val loaded = level.shipObjectWorld.loadedShips.getById(ship.id)!!
                val lubricant = LubricantData.get(level)
                lubricant.set(corner, Blocks.STONE, true); lubricant.set(corner.east(), Blocks.STONE, true)
                lubricant.addArea(listOf(corner, corner.east()))
                GravitronFrozenShip.freeze(level, loaded, Vector3d(corner.x + .25, corner.y + .5, corner.z + .75))
                val gas = ClockworkGasses.HELIUM.resourceLocation.toString()
                BalloonController.getOrCreate(loaded).addBalloon(BalloonData(arrayListOf(AABBi(
                    corner.x + 1, corner.y + 1, corner.z + 1, corner.x + 2, corner.y + 2, corner.z + 2)),
                    hashMapOf(gas to .1), 1000.0, 1.0, false))
                VdexWrapper.saveShip(level, ship, filename)
                copy = paste(helper, filename).single()
            }.thenWaitUntil {
                val loaded = level.shipObjectWorld.loadedShips.getById(copy?.id ?: -1L)
                helper.assertTrue(loaded?.getAttachment(GravitronFrozenShip::class.java) != null, "Frozen overlay was not restored")
            }.thenExecute {
                val destination = origin(copy ?: return@thenExecute)
                val lubricant = LubricantData.get(level)
                helper.assertTrue(lubricant.contains(destination.asLong(), Blocks.STONE), "Lubricant was not copied")
                helper.assertTrue(lubricant.areaAt(destination) == setOf(destination.asLong(), destination.east().asLong()), "Lubricant area was not remapped")
                helper.assertTrue(lubricant.areaAt(corner) == setOf(corner.asLong(), corner.east().asLong()), "Paste changed the original coating")
                val loaded = level.shipObjectWorld.loadedShips.getById(copy?.id ?: -1L)!!
                val frozen = loaded.getAttachment(GravitronFrozenShip::class.java)!!
                helper.assertTrue(copy?.isStatic == true && abs(frozen.x - destination.x - .25) < 1e-9, "Frozen anchor did not move with the ship")
                val balloon = loaded.getAttachment(BalloonController::class.java)!!.balloons.values.single()
                helper.assertTrue(balloon.containsPosition(destination.offset(1, 1, 1)), "Balloon region was not translated: ${balloon.regions}")
                helper.assertTrue((balloon.gasMasses[ClockworkGasses.HELIUM.resourceLocation.toString()] ?: 0.0) > .09,
                    "Balloon helium contents were lost: ${balloon.gasMasses}")
            }.thenSucceed()
        }
    }
}
