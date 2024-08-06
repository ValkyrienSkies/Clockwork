package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendTo
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.CWItem
import kotlin.math.max
import kotlin.math.min


class WanderwandItem(properties: Properties) : CWItem(properties) {

    override fun verifyTagAfterLoad(compoundTag: CompoundTag) {
        compoundTag.putBoolean("hasLoaded", false)
        super.verifyTagAfterLoad(compoundTag)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        if (!level.isClientSide && entity is ServerPlayer) {
            if (!stack.orCreateTag.getBoolean("hasLoaded")) {
                stack.tag!!.putBoolean("hasLoaded", true)
                if (stack.tag!!.contains("selectedBlocks")) {
                    sendTo(WanderwandRenderUpdatePacket(BlockPos.ZERO, ToolType.SELECT, blocks = stack.tag!!.get("selectedBlocks") as CompoundTag), entity as ServerPlayer)
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    companion object {

        @JvmStatic
        fun select(sLevel: ServerLevel, sPlayer: ServerPlayer, firstPos: BlockPos, secondPos: BlockPos, isSecond: Boolean, deselect: Boolean, leftClick: Boolean) {
            if (!isSecond) {
                if (leftClick && sPlayer.mainHandItem.item is WanderwandItem && !sPlayer.cooldowns.isOnCooldown(sPlayer.mainHandItem.item)) {
                    val existingSelection = sPlayer.mainHandItem.tag?.get("selectedBlocks") as CompoundTag?
                    if (existingSelection != null) {
                        val existingSelectionDeser = readBlockPosSetFromNBT(existingSelection)
                        if (existingSelectionDeser.contains(firstPos)) {
                            val components = findIsolatedComponents(existingSelectionDeser)
                            val component = components.find { it.contains(firstPos) }
                            if (component != null) {
                                existingSelectionDeser.removeAll(component)
                                sPlayer.mainHandItem.tag?.put("selectedBlocks", writeBlockPosSetToNBT(existingSelectionDeser))
                            }
                        }
                    }
                }
                return
            }

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

            if (sPlayer.mainHandItem.item is WanderwandItem) {
                val wand = sPlayer.mainHandItem

                if (!deselect) {
                    val existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag?
                    if (existingSelection != null) {
                        selection.addAll(readBlockPosSetFromNBT(existingSelection))
                    }

                    wand.tag?.put("selectedBlocks", writeBlockPosSetToNBT(selection))
                } else {
                    val existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag?
                    if (existingSelection != null) {
                        val existingSelectionDeser = readBlockPosSetFromNBT(existingSelection)
                        existingSelectionDeser.removeAll(selection)
                        wand.tag?.put("selectedBlocks", writeBlockPosSetToNBT(existingSelectionDeser))
                    }
                }
                sendTo(WanderwandRenderUpdatePacket(firstPos, ToolType.SELECT, blocks = writeBlockPosSetToNBT(selection)), sPlayer)
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
            TODO()
        }

        @JvmStatic
        fun bind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {
            TODO()
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

        @JvmStatic
        fun findIsolatedComponents(set: HashSet<BlockPos>): HashSet<HashSet<BlockPos>> {
            if (set.isEmpty()) return HashSet()
            if (set.size == 1) return HashSet(hashSetOf(set))
            val isolatedComponents = HashSet<HashSet<BlockPos>>()
            val visited = HashSet<BlockPos>()

            for (pos in set) {
                if (visited.contains(pos)) continue

                val component = HashSet<BlockPos>()
                val queue = ArrayDeque<BlockPos>()
                queue.add(pos)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    if (visited.contains(current)) continue

                    visited.add(current)
                    component.add(current)

                    for (neighbor in getNeighbors(current)) {
                        if (set.contains(neighbor) && !visited.contains(neighbor)) {
                            queue.add(neighbor)
                        }
                    }
                }

                isolatedComponents.add(component)
            }

            return isolatedComponents
        }

        @JvmStatic
        fun findCorners(set: HashSet<BlockPos>): Pair<BlockPos, BlockPos> {
            val minX = set.minByOrNull { it.x }!!.x
            val minY = set.minByOrNull { it.y }!!.y
            val minZ = set.minByOrNull { it.z }!!.z

            val maxX = set.maxByOrNull { it.x }!!.x
            val maxY = set.maxByOrNull { it.y }!!.y
            val maxZ = set.maxByOrNull { it.z }!!.z

            return Pair(BlockPos(minX, minY, minZ), BlockPos(maxX, maxY, maxZ))
        }

        @JvmStatic
        fun getNeighbors(pos: BlockPos): Set<BlockPos> {
            val neighbors = HashSet<BlockPos>()
            for (dir in Direction.values()) {
                neighbors.add(pos.relative(dir))
            }
            return neighbors
        }
    }
}