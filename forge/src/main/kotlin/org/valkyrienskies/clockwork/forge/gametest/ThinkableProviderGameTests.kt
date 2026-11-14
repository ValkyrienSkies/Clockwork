package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.api.thinkable.Thinkables
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ThinkableProviderGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "thinkable_providers"

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun metadataEntrypointRegistersBlockAndEntityAdapters(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, Blocks.BONE_BLOCK.defaultBlockState())
            val source = Thinkables.find(level, pos)
            helper.assertTrue(source != null, "Optional metadata entrypoint was not discovered")
            helper.assertTrue(level.getBlockEntity(pos) == null && source!!.thinkableValue(Direction.UP) == -12.375,
                "Block provider required a block entity or lost precision")
            helper.assertTrue(source!!.thinkablePortName(Direction.UP)?.string == "Fixture output", "Provider port label was lost")
            level.setBlockAndUpdate(pos, Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(BlockStateProperties.POWER, 5))
            helper.assertTrue(source.thinkableValue(Direction.UP) == null, "Replaced block left a live adapter")
            helper.assertTrue(Thinkables.find(level, pos)?.thinkableValue(Direction.UP) == 5.125, "Entity provider did not take priority over block provider")
            level.setBlockAndUpdate(pos, Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(BlockStateProperties.POWER, 15))
            helper.assertTrue(Thinkables.find(level, pos)?.thinkableValue(Direction.UP) == null, "Nonfinite provider output escaped")
            level.removeBlock(pos, false)
            helper.assertTrue(Thinkables.find(level, pos) == null, "Removed adapter remained discoverable")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun adaptedInputHonorsCablePriorityDisconnectAndAdjacentRemoval(helper: GameTestHelper) {
            val level = helper.level
            val sinkPos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(sinkPos, Blocks.HAY_BLOCK.defaultBlockState())
            val controller = controller(helper, sinkPos.below(), 0.625)
            val sourcePos = sinkPos.east(2)
            level.setBlockAndUpdate(sourcePos, Blocks.BONE_BLOCK.defaultBlockState())
            val sink = WandAnchor.at(level, sinkPos, Direction.DOWN)
            fun value() = Thinkables.find(level, sinkPos)?.thinkableValue(Direction.UP)
            helper.runAtTickTime(6) {
                helper.assertTrue(value() == 0.625, "Adjacent Combobulator did not deliver to a block adapter")
                helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, sourcePos, Direction.UP), sink), "Adapter cable failed")
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(value() == -12.375, "Adjacent output overwrote the cable")
                helper.assertTrue(Thinkables.input(level, sinkPos, Direction.DOWN).value() == -12.375, "Public input API disagreed with delivery")
                level.setBlockAndUpdate(sourcePos, Blocks.BONE_BLOCK.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X))
            }
            helper.runAtTickTime(18) {
                val input = Thinkables.input(level, sinkPos, Direction.DOWN)
                helper.assertTrue(input.connected() && input.value() == null && value() == null,
                    "Unavailable cable fell through to adjacency: input=$input, delivered=${value()}")
                ThinkableConnections.disconnect(level, sink)
            }
            helper.runAtTickTime(24) {
                helper.assertTrue(value() == 0.625, "Disconnect did not restore adjacent input")
                level.removeBlock(controller.blockPos, false)
                helper.assertTrue(value() == null, "Removed adjacent controller did not clear the adapter")
                helper.assertTrue(!Thinkables.input(level, sinkPos, Direction.DOWN).connected(), "Disconnected input reported a connection")
                helper.succeed()
            }
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun relayReadsProviderAndDeliversToBlockAdapter(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.THINKABLE_RELAY.defaultState.setValue(BlockStateProperties.FACING, Direction.EAST))
            level.setBlockAndUpdate(pos.east(), Blocks.BONE_BLOCK.defaultBlockState())
            level.setBlockAndUpdate(pos.above(), Blocks.HAY_BLOCK.defaultBlockState())
            helper.runAtTickTime(6) {
                helper.assertTrue(Thinkables.find(level, pos.above())?.thinkableValue(Direction.UP) == -12.375, "Relay failed to forward provider output")
                level.removeBlock(pos.east(), false)
            }
            helper.runAtTickTime(9) {
                helper.assertTrue(Thinkables.find(level, pos.above())?.thinkableValue(Direction.UP) == null, "Relay retained removed provider input")
                helper.succeed()
            }
        }

        private fun controller(helper: GameTestHelper, pos: BlockPos, value: Double): CombobulatorBlockEntity {
            helper.level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val entity = helper.level.getBlockEntity(pos) as CombobulatorBlockEntity
            entity.updateProgram(CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, constant = value),
                CombobulatorChannel(1, "B", true, constant = 0.0),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.TOP, device = true))), entity.revision)
            return entity
        }
    }
}
