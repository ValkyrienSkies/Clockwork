package org.valkyrienskies.clockwork.content.curiosities.sensor.rotation

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.SimpleContainer
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CompassItem.TAG_LODESTONE_TRACKED
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.mod.common.toWorldCoordinates
import java.util.Random

class LodefocusBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos,
    state
), WorldlyContainer {

    val inventory: NonNullList<ItemStack> = NonNullList.withSize(1, ItemStack.EMPTY)

    var insertionCooldown = 0

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
    }

    override fun tick() {
        super.tick()
        if (insertionCooldown > 0) {
            insertionCooldown--
        }
    }

    fun getTargetPosition(): BlockPos? {
        val stack = inventory[0]
        if (stack.isEmpty) {
            return null
        }
        val tag = stack.tag
        if (tag == null || !tag.contains(TAG_LODESTONE_TRACKED)) {
            return null
        }
        val pos = tag.getCompound("LodestonePos")
        val x = pos.getInt("X")
        val y = pos.getInt("Y")
        val z = pos.getInt("Z")
        return BlockPos(x, y, z)
    }

    fun getWorldspaceTargetPosition(): BlockPos? {
        val targetPosition = getTargetPosition()
        if (targetPosition == null) return null
        if (this.level == null) return targetPosition
        return BlockPos.containing(this.level!!.toWorldCoordinates(targetPosition))
    }

    fun dropCompass() {
        if (!inventory[0].isEmpty) {
            val oldItem = inventory[0]
            if (level != null && !level!!.isClientSide) {
                val sLevel = level as ServerLevel
                val random = Random()
                val launchX = random.nextDouble(0.0, 0.5)
                val launchY = random.nextDouble(0.0, 1.5)
                val launchZ = random.nextDouble(0.0, 0.5)
                val worldPos = sLevel.toWorldCoordinates(worldPosition)
                val itemEntity = ItemEntity(sLevel, worldPos.x, worldPos.y, worldPos.z, oldItem, launchX, launchY, launchZ)
                sLevel.addFreshEntity(itemEntity)
                sLevel.playSound(null, worldPos.x, worldPos.y, worldPos.z, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.65f, 1.0f)
            }
        }
    }

    override fun clearContent() {
        inventory.clear()
    }

    override fun getContainerSize(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return inventory[0].isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return inventory[slot]
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (inventory[slot].isEmpty) {
            return ItemStack.EMPTY
        }
        val result = inventory[slot].split(amount)
        if (inventory[slot].isEmpty) {
            inventory[slot] = ItemStack.EMPTY
        }
        return result
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val result = inventory[slot]
        inventory[slot] = ItemStack.EMPTY
        return result
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        if (stack.`is`(Items.COMPASS) && (stack.tag?.getBoolean(TAG_LODESTONE_TRACKED) == true)) {
            if (!inventory[slot].isEmpty) {
                val oldItem = inventory[slot]
                if (level != null && !level!!.isClientSide) {
                    val sLevel = level as ServerLevel
                    val random = Random()
                    val launchX = random.nextDouble(0.0, 0.5)
                    val launchY = random.nextDouble(0.0, 1.5)
                    val launchZ = random.nextDouble(0.0, 0.5)
                    val worldPos = sLevel.toWorldCoordinates(worldPosition)
                    val itemEntity = ItemEntity(sLevel, worldPos.x, worldPos.y, worldPos.z, oldItem, launchX, launchY, launchZ)
                    sLevel.addFreshEntity(itemEntity)
                    sLevel.playSound(null, worldPos.x, worldPos.y, worldPos.z, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.65f, 1.0f)
                }
            }
            inventory[slot] = stack
            insertionCooldown = 10
        }
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        return IntArray(1)
    }

    override fun canPlaceItemThroughFace(index: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        return itemStack.`is`(Items.COMPASS) && (itemStack.tag?.getBoolean(TAG_LODESTONE_TRACKED) == true)
    }

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean {
        return true
    }
}