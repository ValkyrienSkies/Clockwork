package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendTo
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.clockwork.util.AABBHelper.subtractWithAABB
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import kotlin.math.max
import kotlin.math.min


class WanderwandItem(properties: Properties) : CWItem(properties) {

    override fun verifyTagAfterLoad(compoundTag: CompoundTag) {
        compoundTag.putBoolean("hasLoaded", false)
        super.verifyTagAfterLoad(compoundTag)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltipComponents: MutableList<Component>, isAdvanced: TooltipFlag) {
        tooltipComponents.add(Component.translatable("vs_clockwork.wanderwand.warning").withStyle(ChatFormatting.RED))
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced)
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
                if (leftClick && sPlayer.mainHandItem.item is WanderwandItem) {
                    val existingSelection = sPlayer.mainHandItem.tag?.get("selectedBlocks") as CompoundTag?
                    if (existingSelection != null) {
                        val existingSelectionDeser = readAABBSetFromNBT(existingSelection)
                        val existingAABB = existingSelectionDeser.find { it.containsPoint(firstPos.toJOML())}
                        if (existingAABB != null) {
                            existingSelectionDeser.remove(existingAABB)
                            sPlayer.mainHandItem.tag?.remove("selectedBlocks")
                            sPlayer.mainHandItem.tag?.put("selectedBlocks", writeAABBSetToNBT(existingSelectionDeser))
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

            val selection = AABBi(minX, minY, minZ, (maxX + 1), (maxY + 1), (maxZ + 1))

            if (sPlayer.mainHandItem.item is WanderwandItem) {
                val wand = sPlayer.mainHandItem

                if (!deselect) {
                    val existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag?
                    var toWrite: List<AABBic> = arrayListOf(selection)
                    if (existingSelection != null) {
                        val existingGroups = readAABBSetFromNBT(existingSelection)
                        existingGroups.add(selection)
                        toWrite = mergeAdjacentFast(existingGroups)
                    }
                    wand.tag?.remove("selectedBlocks")
                    wand.tag?.put("selectedBlocks", writeAABBSetToNBT(toWrite))
                } else {
                    val existingSelection = wand.tag?.get("selectedBlocks") as CompoundTag?
                    if (existingSelection != null) {
                        val existingSelectionDeser = readAABBSetFromNBT(existingSelection)
                        val out = existingSelectionDeser.subtractWithAABB(selection)
                        wand.tag?.remove("selectedBlocks")
                        wand.tag?.put("selectedBlocks", writeAABBSetToNBT(out))
                    }
                }
                sendTo(WanderwandRenderUpdatePacket(firstPos, if (deselect) ToolType.DESELECT else ToolType.SELECT, blocks = wand.tag?.get("selectedBlocks") as CompoundTag?), sPlayer)
            }
        }

        @JvmStatic
        fun startWeld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos, clickedFace: Int) {
            val ship = sLevel.getLoadedShipManagingPos(clickedPos) ?: return
            val wand = sPlayer.mainHandItem
            wand.tag?.putBoolean("isWelding", true)
            wand.tag?.putLong("weldingShipId", ship.id)
            val blocks = HashSet<BlockPos>()
            val clickedDir = Direction.values().get(clickedFace)
            blocks.add(clickedPos)
            for (dir in Direction.values()) {
                if (dir == clickedDir) continue
                val neighbor = clickedPos.relative(dir)
                if (sLevel.getBlockState(neighbor).isAir) continue
                blocks.add(neighbor)
            }
            sLevel.playSound(null, sPlayer.blockPosition(), ClockworkSounds.WAND_START.mainEvent!!, sPlayer.soundSource, 1.0f, 1.0f)
            sendTo(WanderwandRenderUpdatePacket(clickedPos, ToolType.WELD, blocks = wand.tag?.get("selectedBlocks") as CompoundTag?, selDir = clickedDir, shipId = ship.id, onOff = true), sPlayer)
        }

        @JvmStatic
        fun weld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos, clickedFace: Int) {
            val ship = sLevel.getLoadedShipManagingPos(clickedPos) ?: return
            val wand = sPlayer.mainHandItem
            wand.tag?.putBoolean("isWelding", true)
            wand.tag?.putLong("weldingShipId", ship.id)
            val blocks = HashSet<BlockPos>()
            val clickedDir = Direction.values().get(clickedFace)
            sLevel.playSound(null, sPlayer.blockPosition(), ClockworkSounds.WAND_WELD.mainEvent!!, sPlayer.soundSource, 1.0f, 1.0f)
            sendTo(WanderwandRenderUpdatePacket(clickedPos, ToolType.WELD, blocks = wand.tag?.get("selectedBlocks") as CompoundTag?, selDir = clickedDir, shipId = ship.id, onOff = false), sPlayer)
        }

        @JvmStatic
        fun attach(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        @JvmStatic
        fun startBind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {
            //TODO
        }

        @JvmStatic
        fun bind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {
            //TODO
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
        fun writeAABBSetToNBT(aabbSet: List<AABBic>): CompoundTag {
            val mainTag = CompoundTag()
            val listTag = ListTag()

            for (aabb in aabbSet) {
                val aabbTag = CompoundTag()
                aabbTag.putLong("lowerCorner", BlockPos(aabb.minX(), aabb.minY(), aabb.minZ()).asLong())
                aabbTag.putLong("upperCorner", BlockPos(aabb.maxX(), aabb.maxY(), aabb.maxZ()).asLong())
                listTag.add(aabbTag)
            }

            mainTag.put("AABBSet", listTag)
            return mainTag
        }

        @JvmStatic
        fun readAABBSetFromNBT(mainTag: CompoundTag): ArrayList<AABBic> {
            val aabbSet: ArrayList<AABBic> = ArrayList()
            val listTag = mainTag.getList("AABBSet", 10) // 10 stands for CompoundTag type

            for (i in listTag.indices) {
                val aabbTag = listTag.getCompound(i)
                val lowerCornerLong = aabbTag.getLong("lowerCorner")
                val upperCornerLong = aabbTag.getLong("upperCorner")
                val lowerPos = BlockPos.of(lowerCornerLong)
                val upperPos = BlockPos.of(upperCornerLong)
                val aabb = AABBi(
                    lowerPos.x,
                    lowerPos.y,
                    lowerPos.z,
                    upperPos.x,
                    upperPos.y,
                    upperPos.z
                )
                aabbSet.add(aabb)
            }
            return aabbSet
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
        fun findIsolatedComponents(list: List<AABBic>, level: Level) : ArrayList<HashSet<BlockPos>> {
            val allPositions = HashSet<BlockPos>()
            for (aabb in list) {
                val minX = aabb.minX()
                val minY = aabb.minY()
                val minZ = aabb.minZ()
                val maxX = aabb.maxX()
                val maxY = aabb.maxY()
                val maxZ = aabb.maxZ()

                for (x in minX until maxX) {
                    for (y in minY until maxY) {
                        for (z in minZ until maxZ) {
                            if (level.getBlockState(BlockPos(x, y, z)).isAir) continue
                            allPositions.add(BlockPos(x, y, z))
                        }
                    }
                }
            }
            return ArrayList(findIsolatedComponents(allPositions))
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
