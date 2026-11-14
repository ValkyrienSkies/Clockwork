package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.content.schematics.SchematicPrinter
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.piston.PistonHeadBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock

class VdexBuildPlan(val blueprint: VdexBlueprintData, level: ServerLevel) {
    data class Step(val ship: Int, val block: VdexBlueprintData.Block? = null, val entity: CompoundTag? = null,
                    val link: Int = -1, val coating: Boolean = false, val requirement: ItemRequirement)
    val steps: List<Step>
    val blockSteps: List<Step>
    val linkCount: Int

    init {
        val blocks = ArrayList<Step>()
        val entities = ArrayList<Step>()
        blueprint.ships.forEachIndexed { index, ship ->
            for (block in ship.blocks) {
                val safe = block.copy(nbt = safeBlockData(block.state, block.pos, block.nbt))
                val requirement = requirement(safe)
                require(!requirement.isInvalid) { "Cannot build ${block.state.block.name.string} in survival" }
                blocks.add(Step(index, block = safe, requirement = requirement))
            }
            for (saved in ship.entities) {
                val entity = saved.copy()
                val nbt = VdexSafeNbt.entity(entity.getCompound("nbt"), level)
                entity.put("nbt", nbt)
                val cost = if (nbt.contains("Contraption")) {
                    val contraption = nbt.getCompound("Contraption")
                    val blockTag = contraption.getCompound("Blocks")
                    val palette = blockTag.getList("Palette", 10).map { NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), it as CompoundTag) }
                    val parts = blockTag.getList("BlockList", 10)
                    require(parts.size in 1..VdexBlueprintData.MAX_BLOCKS) { "Invalid contraption" }
                    val requirements = ArrayList<ItemRequirement.StackRequirement>()
                    for (part in parts) {
                        part as CompoundTag
                        val state = palette.getOrNull(part.getInt("State")) ?: error("Invalid contraption palette")
                        val pos = BlockPos.of(part.getLong("Pos"))
                        val safe = safeBlockData(state, pos, part.getCompound("Data"))
                        part.remove("UpdateTag")
                        if (safe == null) part.remove("Data") else part.put("Data", safe)
                        val partCost = requirement(VdexBlueprintData.Block(pos, state, safe))
                        require(!partCost.isInvalid) { "Cannot build ${state.block.name.string} in survival" }
                        requirements.addAll(partCost.requiredItems)
                    }
                    ItemRequirement(requirements)
                } else {
                    nbt.remove("Passengers")
                    val actual = EntityType.loadEntityRecursive(nbt, level) { it } ?: error("Unknown blueprint entity")
                    require(!actual.onlyOpCanSetNbt()) { "Cannot build this entity in survival" }
                    ItemRequirement.of(actual)
                }
                require(!cost.isInvalid) { "Cannot build blueprint entity ${nbt.getString("id")} in survival" }
                nbt.remove("UUID")
                entities.add(Step(index, entity = entity, requirement = cost))
            }
        }
        blockSteps = blocks.sortedBy { SchematicPrinter.shouldDeferBlock(it.block!!.state) }
        val links = WandLinks.load(blueprint.data.nbtData[WandLinkSchematics.ENTRY] ?: CompoundTag()).links.values.toList()
        linkCount = links.size
        val connections = links.mapIndexed { index, link ->
            Step(0, link = index, requirement = when {
                link.spring -> ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ClockworkItems.SPRING.get())
                link.thinkable -> ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ClockworkItems.THINKABLE.get())
                link.physical -> ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ClockworkItems.ROPE_KNOT.get())
                else -> ItemRequirement.NONE
            })
        }
        val lubricant = blueprint.data.nbtData[WandLinkSchematics.ENTRY]?.getCompound("lubricant")
        val coatings = if (lubricant == null || lubricant.getList("Blocks", 10).isEmpty()) emptyList() else
            List(maxOf(1, lubricant.getList("Areas", 10).size)) {
                Step(0, coating = true, requirement = ItemRequirement(ItemRequirement.ItemUseType.DAMAGE, ClockworkItems.LUBRICANT.get()))
            }
        steps = blockSteps + entities + connections + coatings
    }

    companion object {
        fun safeBlockData(state: BlockState, pos: BlockPos, original: CompoundTag?): CompoundTag? =
            VdexSafeNbt.block(state, pos, original)

        fun requirement(block: VdexBlueprintData.Block): ItemRequirement {
            val state = block.state
            if ((state.block as? HingeBlock)?.attachment == true || state.block is PistonHeadBlock ||
                state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ||
                state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) return ItemRequirement.NONE
            val be = (state.block as? EntityBlock)?.newBlockEntity(block.pos, state)
            if (be != null && block.nbt != null) be.load(block.nbt)
            return ItemRequirement.of(state, be)
        }
    }
}
