package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorAction
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorComparison
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorNode
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperand
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorStep
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableSignals
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFunction
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorOperation
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlock
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorGroup

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class CombobulatorGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "combobulator"

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun functionTexturesUpdateWithoutLosingConnectionsAndRestoreOnLoad(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val level = helper.level
            val controller = place(helper, pos, CombobulatorProgram.controller())
            level.setBlockAndUpdate(pos.east(), ClockworkBlocks.THINKABLE_RELAY.defaultState)
            for ((function, group) in listOf(
                CombobulatorFunction.FLOOR to CombobulatorGroup.ARITHMETIC,
                CombobulatorFunction.COMPARE to CombobulatorGroup.LOGIC,
                CombobulatorFunction.LATCH to CombobulatorGroup.MEMORY,
                CombobulatorFunction.DELAY to CombobulatorGroup.TIMING,
                CombobulatorFunction.PULSE to CombobulatorGroup.TIMING,
                CombobulatorFunction.TIMER to CombobulatorGroup.TIMING,
                CombobulatorFunction.SWITCH to CombobulatorGroup.DEFAULT)) {
                controller.updateProgram(CombobulatorProgram.controller(CombobulatorOperation(function)), controller.revision)
                helper.assertTrue(level.getBlockState(pos).getValue(CombobulatorBlock.FUNCTION_GROUP) == group, "Wrong texture group for $function")
                helper.assertTrue(level.getBlockEntity(pos) === controller &&
                    level.getBlockState(pos).getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.EAST)), "Texture change lost entity or connection")
            }
            val saved = controller.saveWithoutMetadata()
            saved.put("Program", CombobulatorProgram.controller(CombobulatorOperation(CombobulatorFunction.COUNTER)).write())
            controller.load(saved)
            helper.runAtTickTime(3) {
                helper.assertTrue(level.getBlockState(pos).getValue(CombobulatorBlock.FUNCTION_GROUP) == CombobulatorGroup.MEMORY,
                    "Loading a program did not restore its texture group")
                controller.updateProgram(CombobulatorProgram(), controller.revision)
                helper.assertTrue(level.getBlockState(pos).getValue(CombobulatorBlock.FUNCTION_GROUP) == CombobulatorGroup.DEFAULT,
                    "Legacy program did not use the default texture")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun connectedModelTracksAllSixNeighborsAndRemoval(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(3, 4, 3))
            val level = helper.level
            level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            for (side in Direction.entries) level.setBlockAndUpdate(pos.relative(side), ClockworkBlocks.COMBOBULATOR.defaultState)
            helper.runAtTickTime(3) {
                for (side in Direction.entries) {
                    helper.assertTrue(level.getBlockState(pos).getValue(CombobulatorBlock.CONNECTIONS.getValue(side)), "Missing center connection $side")
                    helper.assertTrue(level.getBlockState(pos.relative(side)).getValue(CombobulatorBlock.CONNECTIONS.getValue(side.opposite)),
                        "Missing reciprocal connection $side")
                }
                level.removeBlock(pos.east(), false)
            }
            helper.runAtTickTime(6) {
                helper.assertTrue(!level.getBlockState(pos).getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.EAST)), "Removed neighbor retained extension")
                helper.assertTrue(level.getBlockState(pos).getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.UP)), "Unrelated extension disappeared")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun preciseAdjacentDevicesAndThinkableChain(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val sender = place(helper, pos, CombobulatorProgram.controller(CombobulatorOperation(CombobulatorFunction.MULTIPLY)).copy(
                channels = listOf(
                    CombobulatorChannel(0, "A", true, constant = -12.5),
                    CombobulatorChannel(1, "B", true, constant = 0.8),
                    CombobulatorChannel(3, "Output", false, face = CombobulatorFace.RIGHT, device = true))))
            val receiver = place(helper, pos.east(), CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, face = CombobulatorFace.LEFT, device = true),
                CombobulatorChannel(1, "B", true, constant = 0.375),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.TOP, thinkable = true))))
            val final = place(helper, pos.above(3), CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, face = CombobulatorFace.BOTTOM, thinkable = true),
                CombobulatorChannel(1, "B", true, constant = 16.25),
                CombobulatorChannel(3, "Output", false, wireless = true, first = ItemStack(Items.DIAMOND)))))
            helper.assertTrue(final.connectThinkable(Direction.DOWN, WandAnchor.at(helper.level, pos.east(), Direction.UP)), "Precise chain did not connect")
            helper.runAtTickTime(8) {
                helper.assertTrue(sender.value(3) == -10.0, "Constant multiplication lost precision")
                helper.assertTrue(receiver.value(3) == -9.625, "Adjacent device lost precision")
                helper.assertTrue(final.value(0) == -9.625 && final.value(3) == 7.0, "Thinkable or redstone quantization failed")
                helper.assertTrue(helper.level.getSignal(pos, Direction.WEST) == 0, "Precise output emitted redstone")
                helper.level.removeBlock(pos, false)
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(receiver.value(3).isNaN(), "Missing adjacent device did not invalidate the output")
                helper.assertTrue(final.value(3) == 0.0 && final.fault != null, "Unavailable source did not clear wireless output")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun preciseFeedbackDoesNotDependOnBlockTickOrder(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val program = CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, face = CombobulatorFace.TOP, thinkable = true),
                CombobulatorChannel(1, "B", true, constant = 1.0),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.FRONT, thinkable = true)))
            val first = place(helper, pos, program)
            val second = place(helper, pos.above(3), program)
            helper.assertTrue(first.connectThinkable(Direction.UP, WandAnchor.at(helper.level, second.blockPos, Direction.NORTH)), "First cable failed")
            helper.assertTrue(second.connectThinkable(Direction.UP, WandAnchor.at(helper.level, first.blockPos, Direction.NORTH)), "Second cable failed")
            helper.runAtTickTime(10) {
                helper.assertTrue(first.value(3) > 1.0 && first.value(3) == second.value(3), "Feedback depended on evaluation order")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
        fun thinkablePulseArrivesThenStopsWhenInputIsDisabled(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val sensor = pos.above(3)
            val level = helper.level
            level.setBlockAndUpdate(sensor, ClockworkBlocks.ALT_METER.defaultState)
            val receiver = place(helper, pos, CombobulatorProgram(channels = listOf(
                CombobulatorChannel(0, "Altitude", true, face = CombobulatorFace.TOP, thinkable = true))))
            helper.assertTrue(receiver.connectThinkable(Direction.UP, WandAnchor.at(level, sensor, Direction.DOWN)), "Thinkable did not connect")
            val link = WandLinks.get(level).links.values.single { it.thinkable && it.b.pos == pos }
            helper.runAtTickTime(3) { link.nextPulse = level.gameTime + 1 }
            helper.runAtTickTime(6) {
                helper.assertTrue(link.pulseStart >= 0 && !link.pulseArrived, "Thinkable pulse did not start")
                helper.assertTrue(link.pulseDuration == ThinkableSignals.travelTicks(link.length), "Pulse did not use cable length")
            }
            helper.runAtTickTime(20) {
                helper.assertTrue(link.pulseArrived, "Thinkable pulse did not arrive")
                helper.assertTrue(link.nextPulse > level.gameTime, "Thinkable did not leave a gap between pulses")
                receiver.updateProgram(CombobulatorProgram(channels = emptyList()), receiver.revision)
            }
            helper.runAtTickTime(23) {
                helper.assertTrue(link.pulseStart == -1L && link.pulseArrived, "Disabled input retained an active pulse")
                helper.assertTrue(receiver.disconnectThinkable(Direction.UP), "Disabled port could not be disconnected")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun waitsForFeedbackAndHandsOffOverLink(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val followerPos = pos.above(2)
            val level = helper.level
            val program = CombobulatorProgram(
                channels = CombobulatorProgram().channels + listOf(
                    CombobulatorChannel(2, "Foot", true, face = CombobulatorFace.RIGHT),
                    CombobulatorChannel(3, "Handoff", false, true, first = ItemStack(Items.FEATHER)),
                ),
                steps = listOf(
                    CombobulatorStep("Lift", mapOf(1 to CombobulatorAction(6)), delay = 3),
                    CombobulatorStep("Lower", waitForInput = true, input = 2, timeout = 24),
                    CombobulatorStep("Support", mapOf(1 to CombobulatorAction(11), 3 to CombobulatorAction(pulseTicks = 4))),
                ),
            )
            helper.assertTrue(CombobulatorProgram.read(program.write()).write() == program.write(), "Program did not round trip")
            val first = place(helper, pos, program)
            val follower = place(helper, followerPos, CombobulatorProgram(
                channels = listOf(
                    CombobulatorChannel(0, "Start", true, true, first = ItemStack(Items.FEATHER)),
                    CombobulatorChannel(1, "Done", false, face = CombobulatorFace.FRONT),
                ),
                steps = listOf(CombobulatorStep("Start", mapOf(1 to CombobulatorAction(8)))),
            ))

            helper.runAtTickTime(3) { level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState()) }
            helper.runAtTickTime(5) { level.removeBlock(pos.south(), false) }
            helper.runAtTickTime(10) {
                helper.assertTrue(first.activeStep == 1, "Controller did not enter the input wait")
                helper.assertTrue(level.getSignal(pos, Direction.SOUTH) == 6, "Front did not hold the set level")
                helper.assertTrue(level.getSignal(pos, Direction.WEST) == 0, "Signal leaked out another face")
                helper.assertTrue(follower.signal(1) == 0, "Follower started before the handoff")
                level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())
            }
            helper.runAtTickTime(17) {
                helper.assertTrue(first.activeStep == 2 && first.signal(1) == 11, "Foot input did not advance the routine")
                helper.assertTrue(follower.signal(1) == 8, "Link pulse did not start the follower")
                helper.assertTrue(first.signal(3) == 0, "Link pulse did not expire")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun timeoutClearsPulseAndAppliesStopLevel(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val program = CombobulatorProgram(
                channels = listOf(
                    CombobulatorChannel(0, "Start", true, face = CombobulatorFace.BACK),
                    CombobulatorChannel(1, "Joint", false, face = CombobulatorFace.FRONT, stopLevel = 3),
                    CombobulatorChannel(2, "Foot", true, face = CombobulatorFace.RIGHT),
                ),
                steps = listOf(CombobulatorStep(
                    "Lower", mapOf(1 to CombobulatorAction(pulseTicks = 20)), waitForInput = true, input = 2, timeout = 6,
                )),
            )
            val controller = place(helper, pos, program)
            helper.runAtTickTime(3) { helper.level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState()) }
            helper.runAtTickTime(6) {
                helper.assertTrue(controller.signal(1) == 15, "Pulse did not start")
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(controller.timedOut && controller.activeStep == -1, "Step did not time out")
                helper.assertTrue(controller.signal(1) == 3, "Timeout did not cancel the pulse and apply the stop level")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun thinksWithFractionalAltitudesAndFollowsTheBranch(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 3, 1))
            val sensor = pos.above(2)
            val followerPos = pos.above(4)
            val level = helper.level
            level.setBlockAndUpdate(sensor, ClockworkBlocks.ALT_METER.defaultState)
            val register = CombobulatorProgram.registers.first
            val sender = place(helper, pos, CombobulatorProgram(
                channels = CombobulatorProgram().channels + listOf(
                    CombobulatorChannel(2, "Altitude", true, face = CombobulatorFace.TOP, thinkable = true),
                    CombobulatorChannel(3, "Offset", false, face = CombobulatorFace.RIGHT, thinkable = true),
                    CombobulatorChannel(4, "Wireless", false, wireless = true, first = ItemStack(Items.COPPER_INGOT)),
                ),
                steps = listOf(
                    CombobulatorStep("Offset", node = CombobulatorNode.SUBTRACT,
                        a = CombobulatorOperand(2), b = CombobulatorOperand(constant = sensor.y + 4.0), destination = register),
                    CombobulatorStep("Compare", node = CombobulatorNode.COMPARE,
                        a = CombobulatorOperand(register), b = CombobulatorOperand(constant = -3.25),
                        comparison = CombobulatorComparison.LESS, next = 3, otherwise = 2),
                    CombobulatorStep("Wrong branch", mapOf(1 to CombobulatorAction(15)), next = CombobulatorProgram.STOP),
                    CombobulatorStep("Publish", node = CombobulatorNode.ADD,
                        a = CombobulatorOperand(register), b = CombobulatorOperand(constant = 0.125), destination = 3),
                    CombobulatorStep("Broadcast", node = CombobulatorNode.ADD,
                        a = CombobulatorOperand(register), b = CombobulatorOperand(constant = 10.125), destination = 4),
                ),
            ))
            val follower = place(helper, followerPos, CombobulatorProgram(
                channels = listOf(
                    CombobulatorChannel(0, "Offset", true, face = CombobulatorFace.BOTTOM, thinkable = true),
                    CombobulatorChannel(1, "Wireless", true, wireless = true, first = ItemStack(Items.COPPER_INGOT)),
                ),
            ))
            helper.assertTrue(sender.connectThinkable(Direction.UP, WandAnchor.at(level, sensor, Direction.DOWN)), "Altitude cable failed")
            helper.assertTrue(follower.connectThinkable(Direction.DOWN, WandAnchor.at(level, pos, Direction.EAST)), "Output cable failed")
            helper.runAtTickTime(3) { level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState()) }
            helper.runAtTickTime(12) {
                helper.assertTrue(sender.fault == null && sender.signal(1) == 0, "Comparison took the wrong branch")
                helper.assertTrue(sender.value(2) == sensor.y + 0.5, "Alt Meter reading lost precision")
                helper.assertTrue(follower.value(0) == -3.375, "Thinkable output lost its signed fractional value")
                helper.assertTrue(follower.value(1) == 7.0, "Wireless output did not quantize to redstone strength")
                helper.assertTrue(level.getSignal(pos, Direction.WEST) == 0, "Thinkable leaked redstone")
                helper.assertTrue(follower.disconnectThinkable(Direction.DOWN), "Thinkable did not disconnect")
            }
            helper.runAtTickTime(15) {
                helper.assertTrue(follower.value(0).isNaN(), "Disconnected Thinkable still read a value")
                helper.succeed()
            }
        }

        private fun place(helper: GameTestHelper, pos: BlockPos, program: CombobulatorProgram): CombobulatorBlockEntity {
            helper.level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val blockEntity = helper.level.getBlockEntity(pos) as CombobulatorBlockEntity
            blockEntity.updateProgram(program, blockEntity.revision)
            return blockEntity
        }
    }
}
