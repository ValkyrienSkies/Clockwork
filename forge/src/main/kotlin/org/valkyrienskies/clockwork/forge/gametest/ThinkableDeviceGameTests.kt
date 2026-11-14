package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeWinchBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctConditional
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctBlockEntity
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ThinkableDeviceGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "thinkable_devices"

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun resistorCableOverridesAdjacencyAndClearsUnavailableSource(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.REDSTONE_RESISTOR.defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.Z))
            val resistor = level.getBlockEntity(pos) as RedstoneResistorBlockEntity
            val adjacent = source(helper, pos.above(), 0.8, Direction.DOWN)
            val cable = source(helper, pos.west(2), 0.375, Direction.UP)
            val sink = WandAnchor.at(level, pos, Direction.UP)
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, cable.blockPos, Direction.UP), sink), "Resistor cable failed")
            helper.runAtTickTime(8) {
                helper.assertTrue(resistor.thinkableValue(Direction.DOWN) == 0.375, "Cable lost precision or failed to override adjacency")
                cable.updateProgram(CombobulatorProgram.controller().copy(channels = emptyList()), cable.revision)
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(resistor.thinkableValue(Direction.DOWN) == 0.0, "Unavailable cable fell through to adjacency")
                helper.assertTrue(ThinkableConnections.disconnect(level, sink), "Cable did not disconnect")
            }
            helper.runAtTickTime(16) {
                helper.assertTrue(resistor.thinkableValue(Direction.DOWN) == 0.8, "Adjacent controller did not resume")
                level.removeBlock(adjacent.blockPos, false)
            }
            helper.runAtTickTime(20) {
                helper.assertTrue(ThinkableConnections.input(level, pos, Direction.UP) == null, "Removed adjacent output remained active")
                helper.assertTrue(resistor.thinkableValue(Direction.DOWN) == 1.0, "Resistor did not restore normal unpowered behavior")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun valveUsesFractionalTargetAndRestoresNormalControl(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.VALVE_DUCT.defaultState)
            val valve = level.getBlockEntity(pos) as ValveDuctBlockEntity
            val side = Direction.entries.single(valve::acceptsThinkable)
            val sender = source(helper, pos.relative(side), 0.375, side.opposite)
            helper.runAtTickTime(5) {
                valve.speed = 0f
                valve.tick()
                helper.assertTrue(valve.pointer.value == 0f, "Thinkable moved an unpowered valve")
                valve.speed = 64f
                repeat(8) { valve.tick() }
                helper.assertTrue(valve.thinkableValue(side.opposite) == 0.375, "Valve did not reach the fractional opening")
                level.removeBlock(sender.blockPos, false)
                valve.speed = -64f
                repeat(8) { valve.tick() }
                helper.assertTrue(valve.thinkableValue(side.opposite) == 0.0, "Valve retained a stale precise target")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun bothFlapsAcceptDegreesAndReportActualAngle(helper: GameTestHelper) {
            val level = helper.level
            for ((index, block) in listOf(ClockworkBlocks.ANDESITE_FLAP_BEARING.get(), ClockworkBlocks.SMART_FLAP_BEARING.get()).withIndex()) {
                val pos = helper.absolutePos(BlockPos(2 + index * 3, 3, 3))
                level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH))
                level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState())
                val flap = level.getBlockEntity(pos) as FlapBearingBlockEntity
                val side = Direction.entries.single(flap::acceptsThinkable)
                source(helper, pos.relative(side), -7.125, side.opposite)
                helper.runAtTickTime(5) {
                    flap.assemble()
                    helper.assertTrue(flap.isRunning, "Flap did not assemble")
                    flap.speed = 0f
                    flap.tick()
                    helper.assertTrue(flap.thinkableValue(side.opposite) == 0.0, "Unpowered flap moved")
                    flap.speed = 16f
                    repeat(5) { flap.tick() }
                    helper.assertTrue(abs(flap.thinkableValue(side.opposite)!! + 7.125) < 1e-6, "Flap lost signed angle precision")
                    level.removeBlock(pos.relative(side), false)
                    repeat(5) { flap.tick() }
                    helper.assertTrue(flap.thinkableValue(side.opposite) == 0.0, "Flap did not restore redstone control")
                    flap.disassemble()
                    helper.assertTrue(flap.thinkableValue(side.opposite) == null, "Disassembled flap reported a valid angle")
                }
            }
            helper.runAtTickTime(6) { helper.succeed() }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun gasDuctReportsFilteredMeasurementInsteadOfThreshold(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.REDSTONE_DUCT.defaultState)
            val duct = level.getBlockEntity(pos) as RedstoneDuctBlockEntity
            helper.runAtTickTime(5) {
                helper.assertTrue(duct.ensureNodeExists(), "Gas node not created")
                val info = ClockworkMod.getKelvin(level).nodeInfo.getValue(duct.getDuctNodePosition())
                info.currentGasMasses.clear()
                info.currentGasMasses[ClockworkGasses.HELIUM] = 0.125
                duct.conditional = RedstoneDuctConditional(RedstoneDuctConditional.ConditionalType.MASS, true, 0.05,
                    listOf(ClockworkGasses.HELIUM), false)
                val side = Direction.entries.first(duct::providesThinkable)
                helper.assertTrue(duct.thinkableValue(side) == 0.125 && duct.getPower() == 15, "Gas measurement was quantized or ignored its filter")
                duct.conditional!!.filterBlacklist = true
                helper.assertTrue(duct.thinkableValue(side) == 0.0, "Gas blacklist was ignored")
                info.currentGasMasses.clear()
                duct.conditional!!.type = RedstoneDuctConditional.ConditionalType.PRESSURE
                helper.assertTrue(duct.thinkableValue(side) == 0.0, "Empty gas node produced invalid pressure")
                duct.conditional = null
                helper.assertTrue(duct.thinkableValue(side) == null, "Unconfigured duct exposed a fake measurement")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun winchMovesTowardPreciseLengthUsingKineticPower(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(2, 3, 2))
            level.setBlockAndUpdate(pos, ClockworkBlocks.ROPE_WINCH.defaultState.setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z))
            level.setBlockAndUpdate(pos.east(2), Blocks.STONE.defaultBlockState())
            level.setBlockAndUpdate(pos.north(), AllBlocks.CREATIVE_MOTOR.defaultState.setValue(BlockStateProperties.FACING, Direction.SOUTH))
            val motor = level.getBlockEntity(pos.north()) as CreativeMotorBlockEntity
            val winch = level.getBlockEntity(pos) as RopeWinchBlockEntity
            val rope = WandLinks.get(level).add(level, WandAnchor(pos, Direction.DOWN, winch = true),
                WandAnchor(pos.east(2), Direction.WEST), true, 4.0, physical = true)!!
            val sender = source(helper, pos.above(2), 3.0625, Direction.DOWN)
            val sink = WandAnchor.at(level, pos, Direction.UP)
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, sender.blockPos, Direction.DOWN), sink), "Winch input failed")
            motor.generatedSpeed.setValue(64)
            helper.runAtTickTime(25) {
                helper.assertTrue(rope.length == 3.0625 && winch.thinkableValue(Direction.EAST) == 3.0625,
                    "Winch did not stop at the precise length: ${rope.length}")
                motor.generatedSpeed.setValue(0)
                ThinkableConnections.disconnect(level, sink)
            }
            helper.runAtTickTime(30) {
                helper.assertTrue(rope.length == 3.0625, "Unpowered winch moved")
                motor.generatedSpeed.setValue(64)
            }
            helper.runAtTickTime(38) {
                helper.assertTrue(rope.length > 3.0625, "Normal motor direction did not resume")
                helper.succeed()
            }
        }

        private fun source(helper: GameTestHelper, pos: BlockPos, value: Double, side: Direction): CombobulatorBlockEntity {
            helper.level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val be = helper.level.getBlockEntity(pos) as CombobulatorBlockEntity
            val face = CombobulatorFace.entries.single { it.direction(Direction.NORTH) == side }
            be.updateProgram(CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, constant = value),
                CombobulatorChannel(1, "B", true, constant = 0.0),
                CombobulatorChannel(3, "Output", false, face = face, device = true))), be.revision)
            return be
        }
    }
}
