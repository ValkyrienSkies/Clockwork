package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.api.thinkable.Thinkables
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.kinetics.combobulator.*

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class CreateThinkableGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "create_thinkables"

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun gaugesExposePreciseNamedReadings(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, AllBlocks.SPEEDOMETER.defaultState)
            (level.getBlockEntity(pos) as SpeedGaugeBlockEntity).speed = -12.375f
            val speed = Thinkables.find(level, pos)!!
            helper.assertTrue(speed.thinkableValue(Direction.UP, "speed") == -12.375 &&
                speed.thinkableValue(Direction.UP, "absolute_speed") == 12.375, "Speed readings lost precision or direction")
            helper.assertTrue(speed.thinkableValue(Direction.UP, "unknown") == null, "Unknown reading silently fell back")
            level.setBlockAndUpdate(pos, AllBlocks.STRESSOMETER.defaultState)
            (level.getBlockEntity(pos) as StressGaugeBlockEntity).updateFromNetwork(128f, 17.25f, 1)
            val stress = Thinkables.find(level, pos)!!
            helper.assertTrue(stress.thinkableReadings(Direction.UP).size == 4, "Missing stress choices")
            helper.assertTrue(stress.thinkableValue(Direction.UP, "stress") == 17.25 &&
                stress.thinkableValue(Direction.UP, "capacity") == 128.0 &&
                stress.thinkableValue(Direction.UP, "remaining_stress") == 110.75 &&
                stress.thinkableValue(Direction.UP, "stress_percent") == 13.4765625, "Incorrect raw stress readings")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun thresholdAndTankExposeAmountCapacityAndPercent(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos, AllBlocks.THRESHOLD_SWITCH.defaultState)
            val threshold = level.getBlockEntity(pos) as ThresholdSwitchBlockEntity
            threshold.currentMinLevel = 10
            threshold.currentLevel = 11
            threshold.currentMaxLevel = 42
            val reader = Thinkables.find(level, pos)!!
            helper.assertTrue(reader.thinkableValue(Direction.UP, "fill_percent") == 3.125, "Threshold fill lost precision")
            threshold.currentLevel = -1
            helper.assertTrue(reader.thinkableValue(Direction.UP) == null, "Disconnected threshold was treated as a value")
            level.setBlockAndUpdate(pos, AllBlocks.FLUID_TANK.defaultState)
            val tank = level.getBlockEntity(pos) as FluidTankBlockEntity
            tank.tankInventory.fill(FluidStack(Fluids.WATER, 125), IFluidHandler.FluidAction.EXECUTE)
            val fluid = Thinkables.find(level, pos)!!
            helper.assertTrue(fluid.thinkableValue(Direction.UP, "fluid") == 125.0, "Tank amount is not in mB")
            helper.assertTrue(fluid.thinkableValue(Direction.UP, "fluid_capacity") == tank.tankInventory.capacity.toDouble(), "Tank capacity is incorrect")
            helper.assertTrue(tank.tankInventory.fluidAmount == 125, "Reading consumed fluid")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
        fun observerCountsFilteredItemsWithoutExtracting(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState())
            val chest = level.getBlockEntity(pos.east()) as ChestBlockEntity
            chest.setItem(0, ItemStack(Items.IRON_INGOT, 37))
            chest.setItem(1, ItemStack(Items.GOLD_INGOT, 23))
            level.setBlockAndUpdate(pos, AllBlocks.SMART_OBSERVER.defaultState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST))
            helper.runAtTickTime(25) {
                val observer = level.getBlockEntity(pos) as SmartObserverBlockEntity
                observer.getBehaviour(FilteringBehaviour.TYPE).setFilter(ItemStack(Items.IRON_INGOT))
                val reader = Thinkables.find(level, pos)!!
                helper.assertTrue(reader.thinkableValue(Direction.UP, "items") == 37.0, "Observer ignored its item filter")
                helper.assertTrue(reader.thinkableValue(Direction.UP, "fluid") == null, "Missing fluid inventory produced a false reading")
                helper.assertTrue(chest.getItem(0).count == 37 && chest.getItem(1).count == 23, "Reading extracted items")
                helper.succeed()
            }
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
        fun addonReadingSurvivesCableRelaySelectionAndSave(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val source = pos.east(3)
            val relay = pos.east(2)
            level.setBlockAndUpdate(source, Blocks.BONE_BLOCK.defaultBlockState())
            level.setBlockAndUpdate(relay, ClockworkBlocks.THINKABLE_RELAY.defaultState.setValue(BlockStateProperties.FACING, Direction.EAST))
            level.setBlockAndUpdate(pos, ClockworkBlocks.COMBOBULATOR.defaultState)
            val controller = level.getBlockEntity(pos) as CombobulatorBlockEntity
            val program = CombobulatorProgram.controller().copy(channels = listOf(
                CombobulatorChannel(0, "A", true, face = CombobulatorFace.TOP, thinkable = true, reading = "doubled"),
                CombobulatorChannel(1, "B", true, constant = 0.0),
                CombobulatorChannel(3, "Output", false, face = CombobulatorFace.FRONT, device = true)))
            controller.updateProgram(CombobulatorProgram.read(program.write()), controller.revision)
            helper.assertTrue(ThinkableConnections.connect(level, WandAnchor.at(level, relay, Direction.UP),
                WandAnchor.at(level, pos, Direction.UP)), "Relay cable could not connect")
            helper.runAtTickTime(8) {
                helper.assertTrue(controller.value(0) == -24.75, "Cable/relay lost the selected addon reading")
                helper.assertTrue(controller.availableReadings[Direction.UP]?.any { it.id() == "doubled" } == true,
                    "Addon reading was not available to the GUI through the relay")
                helper.assertTrue(Thinkables.input(level, pos, Direction.UP).value() == -24.75, "Input API ignored the selection")
                val saved = controller.saveWithoutMetadata()
                helper.assertTrue(saved.getCompound("Program").getList("Channels", 10).getCompound(0).getString("Reading") == "doubled",
                    "Saved controller lost reading ID")
                val update = controller.updateTag
                val copy = ClockworkBlocks.COMBOBULATOR.get().newBlockEntity(pos, controller.blockState) as CombobulatorBlockEntity
                copy.readClient(update)
                helper.assertTrue(copy.availableReadings[Direction.UP]?.any { it.id() == "doubled" } == true, "Client packet lost selector choices")
                controller.updateProgram(program.copy(channels = program.channels.map {
                    if (it.id == 0) it.copy(reading = "missing") else it
                }), controller.revision)
            }
            helper.runAtTickTime(12) {
                helper.assertTrue(controller.value(0).isNaN(), "Missing addon reading fell back to a different value")
                ThinkableConnections.disconnect(level, WandAnchor.at(level, pos, Direction.UP))
                level.setBlockAndUpdate(pos.above(), Blocks.BONE_BLOCK.defaultBlockState())
                controller.updateProgram(program.copy(channels = program.channels.map {
                    if (it.id == 0) it.copy(thinkable = false, device = true) else it
                }), controller.revision)
            }
            helper.runAtTickTime(16) {
                helper.assertTrue(controller.value(0) == -24.75, "Adjacent device ignored selected reading")
                helper.succeed()
            }
        }
    }
}
