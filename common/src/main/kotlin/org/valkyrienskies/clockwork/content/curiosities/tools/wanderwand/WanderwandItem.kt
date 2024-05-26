package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.platform.CWItem
import kotlin.math.max
import kotlin.math.min


class WanderwandItem(properties: Properties) : CWItem(properties) {

    var idleProgress = 0.0f

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)

        val listtag = ListTag()
    }

    companion object {

        @JvmStatic
        fun select(sLevel: ServerLevel, sPlayer: ServerPlayer, firstPos: BlockPos, secondPos: BlockPos, isSecond: Boolean, deselect: Boolean, leftClick: Boolean) {
            if (isSecond) return

            val minX = min(firstPos.x, secondPos.x)
            val minY = min(firstPos.y, secondPos.y)
            val minZ = min(firstPos.z, secondPos.z)

            val maxX = max(firstPos.x, secondPos.x)
            val maxY = max(firstPos.y, secondPos.y)
            val maxZ = max(firstPos.z, secondPos.z)

            val selection = HashSet<BlockPos>()
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        if (sLevel.getBlockState(BlockPos(x, y, z)).isAir) continue
                        selection.add(BlockPos(x, y, z))
                    }
                }
            }

            if (sPlayer.inventory.getSelected().item is WanderwandItem) {
                val wand = sPlayer.inventory.getSelected()

                if (!deselect) {
                    val existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag
                    if (existingSelection != null) {
                        selection.addAll(readBlockPosSetFromNBT(existingSelection))
                    }

                    wand.tag?.put("selectedBlocks", writeBlockPosSetToNBT(selection))
                } else {
                    var existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag
                    if (existingSelection != null) {
                        val existingSelectionDeser = readBlockPosSetFromNBT(existingSelection)
                        existingSelectionDeser.removeAll(selection)
                        wand.tag?.put("selectedBlocks", writeBlockPosSetToNBT(existingSelectionDeser))
                    }
                }
            }
        }

        @JvmStatic
        fun startWeld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }
        @JvmStatic
        fun weld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        @JvmStatic
        fun attach(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        @JvmStatic
        fun startBind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }
        @JvmStatic
        fun bind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        // Method to write a set of BlockPos to NBT
        @JvmStatic
        fun writeBlockPosSetToNBT(blockPosSet: Set<BlockPos>): CompoundTag {
            // Create the main compound tag
            val mainTag = CompoundTag()

            // Create a list tag to hold the BlockPos entries
            val listTag = ListTag()

            // Iterate over the BlockPos set and convert each BlockPos to an NBT tag
            for (blockPos in blockPosSet) {
                // Create a new compound tag for each BlockPos
                val posTag = CompoundTag()

                // Save the BlockPos coordinates
                posTag.putInt("x", blockPos.x)
                posTag.putInt("y", blockPos.y)
                posTag.putInt("z", blockPos.z)

                // Add the BlockPos tag to the list
                listTag.add(posTag)
            }

            // Add the list tag to the main tag
            mainTag.put("BlockSet", listTag)
            return mainTag
        }

        // Method to read a set of BlockPos from NBT
        @JvmStatic
        fun readBlockPosSetFromNBT(mainTag: CompoundTag): HashSet<BlockPos> {
            // Create a set to hold the BlockPos objects
            val blockPosSet: HashSet<BlockPos> = HashSet()

            // Get the list tag from the main tag
            val listTag = mainTag.getList("BlockSet", 10) // 10 stands for CompoundTag type

            // Iterate over the list tag and convert each entry back to a BlockPos
            for (i in listTag.indices) {
                val posTag = listTag.getCompound(i)
                val x = posTag.getInt("x")
                val y = posTag.getInt("y")
                val z = posTag.getInt("z")
                val blockPos = BlockPos(x, y, z)
                blockPosSet.add(blockPos)
            }
            return blockPosSet
        }
    }
}