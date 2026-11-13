package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.plotting.PlottingPlan
import org.valkyrienskies.clockwork.content.kinetics.plotting.PlottingTableBlockEntity

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class PlottingTableGameTests {
    companion object {
        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "plotting")
        fun legacyPlansRemainAvailableForFileImport(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val level = helper.level
            level.setBlockAndUpdate(pos, ClockworkBlocks.PLOTTING_TABLE.defaultState)
            val table = level.getBlockEntity(pos) as PlottingTableBlockEntity
            val plans = listOf(PlottingPlan.example(), PlottingPlan("Left leg"))
            helper.assertTrue(table.plans.isEmpty(), "New tables should not contain plans")
            table.load(net.minecraft.nbt.CompoundTag().apply { put("Plans", net.minecraft.nbt.ListTag().apply { plans.forEach { add(it.write()) } }) })
            val saved = table.saveWithoutMetadata()
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(pos, ClockworkBlocks.PLOTTING_TABLE.defaultState)
            val restored = level.getBlockEntity(pos) as PlottingTableBlockEntity
            restored.load(saved)
            helper.assertTrue(restored.plans == plans, "Plan library did not survive NBT reload")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "plotting")
        fun droppedTableRetainsPlans(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val level = helper.level
            val state = ClockworkBlocks.PLOTTING_TABLE.defaultState
            level.setBlockAndUpdate(pos, state)
            val table = level.getBlockEntity(pos) as PlottingTableBlockEntity
            table.load(net.minecraft.nbt.CompoundTag().apply { put("Plans", net.minecraft.nbt.ListTag().apply { add(PlottingPlan.example().copy(name = "Saved circuit").write()) }) })
            val drop = Block.getDrops(state, level, pos, table).singleOrNull()
            helper.assertTrue(drop != null && drop.`is`(ClockworkBlocks.PLOTTING_TABLE.asItem()), "Missing table drop")
            val stored = drop!!.tag?.getCompound("BlockEntityTag")?.getList("Plans", 10)
            helper.assertTrue(stored?.size == 1 && stored.getCompound(0).getString("Name") == "Saved circuit", "Table drop lost its plan library")
            helper.succeed()
        }
    }
}
