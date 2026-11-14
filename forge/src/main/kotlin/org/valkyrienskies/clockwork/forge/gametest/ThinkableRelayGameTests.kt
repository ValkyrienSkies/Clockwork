package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlock
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorChannel
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFace
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorProgram
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableRelayBlockEntity

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ThinkableRelayGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "thinkable_relay"

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun fansOutPreciseCableAndAdjacentSignalsWithoutRetainingRemovedInput(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val relay = relay(helper, pos, Direction.DOWN)
            val next = relay(helper, pos.east(), Direction.WEST)
            val source = source(helper, pos.west(2), -12.375)
            level.setBlockAndUpdate(pos.below(), ClockworkBlocks.ALT_METER.defaultState)
            val sink = WandAnchor.at(level, pos, Direction.DOWN)
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, source.blockPos, Direction.UP), sink), "Relay input cable failed")
            val receiverPos = pos.above(2)
            level.setBlockAndUpdate(receiverPos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val receiver = level.getBlockEntity(receiverPos) as CombobulatorBlockEntity
            receiver.updateProgram(CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, face = CombobulatorFace.BOTTOM, thinkable = true),
                CombobulatorChannel(1, "B", true, constant = 0.0),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.TOP, device = true))), receiver.revision)
            helper.assertTrue(receiver.connectThinkable(Direction.DOWN, WandAnchor.at(level, pos, Direction.UP)), "Relay output cable failed")
            helper.runAtTickTime(8) {
                for (side in Direction.entries) helper.assertTrue(relay.thinkableValue(side) == if (side == Direction.DOWN) null else -12.375,
                    "Relay did not forward precisely on $side")
                helper.assertTrue(next.thinkableValue(Direction.UP) == -12.375 && receiver.value(0) == -12.375, "Chained relay or cable fan-out failed")
                helper.assertTrue(ThinkableConnections.disconnect(level, sink), "Relay cable did not disconnect")
                helper.assertTrue(relay.thinkableValue(Direction.UP) == pos.y - 0.5, "Relay did not fall back to the adjacent Alt Meter")
                level.removeBlock(pos.below(), false)
                helper.assertTrue(next.thinkableValue(Direction.UP) == null,
                    "Relay retained the removed source: first=${relay.thinkableValue(Direction.UP)}, next=${next.thinkableValue(Direction.UP)}, source=${level.getBlockEntity(pos.below())}, state=${level.getBlockState(pos.below())}")
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(receiver.value(0).isNaN(), "Relay did not propagate unavailable input")
                helper.succeed()
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun wrenchSelectsEachInputFaceAndCasingConnectsToControllers(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val relay = relay(helper, pos, Direction.DOWN)
            level.setBlockAndUpdate(pos.east(), ClockworkBlocks.COMBOBULATOR.defaultState)
            val player = helper.makeMockPlayer()
            player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack())
            val block = ClockworkBlocks.THINKABLE_RELAY.get()
            for (side in Direction.entries) {
                val context = UseOnContext(player, InteractionHand.MAIN_HAND, BlockHitResult(Vec3.atCenterOf(pos), side, pos, false))
                block.onWrenched(level.getBlockState(pos), context)
                helper.assertTrue(relay.inputSide == side, "Wrench did not select $side")
                helper.assertTrue(Direction.entries.count(relay::acceptsThinkable) == 1 && Direction.entries.count(relay::providesThinkable) == 5,
                    "Relay did not keep exactly one input and five outputs")
            }
            val state = level.getBlockState(pos)
            helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.FACING) == Direction.SOUTH, "Rotated input was lost")
            helper.assertTrue(state.mirror(Mirror.FRONT_BACK).getValue(BlockStateProperties.FACING) == Direction.WEST, "Mirrored input was lost")
            helper.assertTrue(state.getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.EAST)), "Relay did not connect its casing")
            helper.assertTrue(level.getBlockState(pos.east()).getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.WEST)), "Controller casing did not connect back")
            level.removeBlock(pos.east(), false)
            helper.assertTrue(!level.getBlockState(pos).getValue(CombobulatorBlock.CONNECTIONS.getValue(Direction.EAST)), "Removed casing connection remained")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = BATCH)
        fun relayLoopIsUnavailableAndRecoversWhenDriven(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val first = relay(helper, pos, Direction.EAST)
            relay(helper, pos.east(), Direction.SOUTH)
            relay(helper, pos.east().south(), Direction.WEST)
            relay(helper, pos.south(), Direction.NORTH)
            helper.assertTrue(first.thinkableValue(Direction.UP) == null, "Relay loop generated a value")
            val source = source(helper, pos.west(2), 0.125)
            val sink = WandAnchor.at(level, pos, Direction.EAST)
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, source.blockPos, Direction.UP), sink), "Loop override cable failed")
            helper.runAtTickTime(6) {
                helper.assertTrue(first.thinkableValue(Direction.UP) == 0.125, "Driven relay loop did not recover")
                ThinkableConnections.disconnect(level, sink)
                helper.assertTrue(first.thinkableValue(Direction.UP) == null, "Relay loop retained the disconnected value")
                helper.succeed()
            }
        }

        private fun relay(helper: GameTestHelper, pos: BlockPos, input: Direction): ThinkableRelayBlockEntity {
            helper.level.setBlockAndUpdate(pos, ClockworkBlocks.THINKABLE_RELAY.defaultState.setValue(BlockStateProperties.FACING, input))
            return helper.level.getBlockEntity(pos) as ThinkableRelayBlockEntity
        }

        private fun source(helper: GameTestHelper, pos: BlockPos, value: Double): CombobulatorBlockEntity {
            helper.level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val source = helper.level.getBlockEntity(pos) as CombobulatorBlockEntity
            source.updateProgram(CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, constant = value),
                CombobulatorChannel(1, "B", true, constant = 0.0),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.TOP, device = true))), source.revision)
            return source
        }
    }
}
