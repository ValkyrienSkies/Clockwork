package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Clearable
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.min

class CoalBurnerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), Clearable,
    ISyncableStorage {


    var fuelTicks: Int = 0
    var maxBurnTime: Double = 0.0

    var storedFuelStack: ItemStack = ItemStack.EMPTY
    var remainingItemStack: ItemStack = ItemStack.EMPTY
    var previousTotalItems: Int = 0

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return

        // Bit of a cheesy way to check for a change but whatever
        val totalItems = storedFuelStack.count + remainingItemStack.count
        if (totalItems != previousTotalItems) {
            ClockworkPackets.sendToNear(
                level as ServerLevel,
                this.worldPosition,
                64,
                SyncableStoragePacket(this)
            )
            this.previousTotalItems = totalItems
        }

        val kelvin = ClockworkMod.getKelvin(level)

        kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return
        if (fuelTicks>0) {
            fuelTicks-=1
            val ductPos = blockPos.toDuctNodePos(level!!.dimension().location())
            val currentInternalGasses = kelvin.getGasMassAt(ductPos)
            val currentInternalTemperature = kelvin.getTemperatureAt(ductPos)
            if (currentInternalGasses.values.sum() > 1e-5) {
                // Use combined gas+wall heat capacity: hitting the target temperature
                // requires heating both the gas and the duct wall, otherwise the burner
                // wastes work to a wall that keeps draining gas heat back down.
                val currentInternalHeatCapacity = kelvin.getNodeHeatCapacity(ductPos)
                val targetTemperature = 850.0
                val energyToAdd = min(currentInternalHeatCapacity * (targetTemperature - currentInternalTemperature), MAX_JOULES_PER_TICK)
                if (energyToAdd > 0) {
                    kelvin.modHeatEnergy(ductPos, energyToAdd)
                }
            }

            if (blockState.getValue(CoalBurnerBlock.LIT)==false) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,true), 15)
        } else {
            if (storedFuelStack.isEmpty and blockState.getValue(CoalBurnerBlock.LIT)) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,false), 15)

            if (!storedFuelStack.isEmpty) {
                val burnTime = FuelRegistry.get(storedFuelStack)
                fuelTicks += burnTime
                maxBurnTime = burnTime.toDouble()
                if (storedFuelStack.item.hasCraftingRemainingItem()) {
                    val remaining = storedFuelStack.item.craftingRemainingItem
                    if (remainingItemStack.isEmpty) {
                        remainingItemStack = ItemStack(remaining)
                    } else if (remainingItemStack.item.equals(remaining) && remainingItemStack.count + 1 <= remaining!!.maxStackSize) {
                        remainingItemStack.grow(1)
                    } else {
                        val dropped = ItemEntity(level, blockPos.x.toDouble() + 0.5,
                            (blockPos.y + 1).toDouble(), blockPos.z.toDouble() + 0.5, ItemStack(remaining))
                        dropped.setDefaultPickUpDelay()
                        dropped.setDeltaMovement(0.0, 0.25, 0.0)
                        level!!.addFreshEntity(dropped)
                    }
                }
                storedFuelStack.count -= 1
                sendData()
            }

        }
    }


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        // For some reason the tag is null when pasted as a schematic?
        // I suspect it's create clearing NBT on schematic pasted blocks
        // to prevent things like signs with command-click events
        val subTag: Tag? = tag.get("StoredFuelStack")
        storedFuelStack = if (subTag == null) {
            ItemStack.EMPTY
        } else {
            ItemStack.of(subTag as CompoundTag)
        }

        val remainingTag: Tag? = tag.get("RemainingItemStack")
        remainingItemStack = if (remainingTag == null) {
            ItemStack.EMPTY
        } else {
            ItemStack.of(remainingTag as CompoundTag)
        }

        fuelTicks = tag.getInt("FuelTicks")
        maxBurnTime = tag.getDouble("MaxBurnTime")

        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        // We use a sub tag, instead of saving directly to BE tag
        // So that it doesn't load the burner block as the item
        val subTag = CompoundTag()
        storedFuelStack.save(subTag)
        val remainingTag = CompoundTag()
        remainingItemStack.save(remainingTag)
        tag.put("StoredFuelStack", subTag)
        tag.put("RemainingItemStack", remainingTag)
        tag.putInt("FuelTicks", fuelTicks)
        tag.putDouble("MaxBurnTime", maxBurnTime)
        super.write(tag, clientPacket)
    }

    override fun destroy() {
        val vec3d = blockPos.toJOMLD()

        val ie = ItemEntity(level!!, vec3d.x, vec3d.y, vec3d.z, storedFuelStack)
        level!!.addFreshEntity(ie)
        val ie2 = ItemEntity(level!!, vec3d.x, vec3d.y, vec3d.z, remainingItemStack)
        level!!.addFreshEntity(ie2)
        super.destroy()
    }

    override fun clearContent() {
        storedFuelStack = ItemStack.EMPTY
        remainingItemStack = ItemStack.EMPTY
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        if (!storedFuelStack.isEmpty || !remainingItemStack.isEmpty) {
            (tooltip as MutableList).add(Component.literal("    Coal burner Info").withStyle(ChatFormatting.GRAY))
            if (!storedFuelStack.isEmpty) {
                tooltip.add(Component.literal("Fuel: ").withStyle(ChatFormatting.GOLD)
                    .append(storedFuelStack.displayName)
                    .append((Component.literal("x ${storedFuelStack.count}")).withStyle(ChatFormatting.GOLD)))
            }
            if (!remainingItemStack.isEmpty) {
                tooltip.add(Component.literal("Remaining: ").withStyle(ChatFormatting.GOLD)
                    .append(remainingItemStack.displayName)
                    .append((Component.literal("x ${remainingItemStack.count}")).withStyle(ChatFormatting.GOLD)))
            }
        }

        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        return intArrayOf(if (side == Direction.DOWN) 1 else 0)
    }

    override fun canPlaceItemThroughFace(
        index: Int,
        itemStack: ItemStack,
        direction: Direction?
    ): Boolean {
        return (direction != Direction.DOWN) && (FuelRegistry.get(itemStack) > 0) && (index == 0)
    }

    override fun canTakeItemThroughFace(
        index: Int,
        stack: ItemStack,
        direction: Direction
    ): Boolean {
        return (direction == Direction.DOWN) && (index == 1)
    }

    override fun getContainerSize(): Int {
        return 2
    }

    override fun isEmpty(): Boolean {
        return storedFuelStack.isEmpty && remainingItemStack.isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return if (slot == 0) storedFuelStack else remainingItemStack
    }

    // We let the remainder item (bucket) be extracted from DOWN
    // You can't extract the fuel from DOWN because... why would you want that
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (remainingItemStack.isEmpty) {
            return ItemStack.EMPTY
        }
        val result = remainingItemStack.split(amount)
        if (remainingItemStack.isEmpty) {
            remainingItemStack = ItemStack.EMPTY
        }
        return result
    }
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val result = remainingItemStack.copy()
        remainingItemStack = ItemStack.EMPTY
        return result
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot != 0) return
        storedFuelStack = stack
    }

    override fun stillValid(player: Player): Boolean {
        return level!!.getBlockEntity(this.worldPosition) === this
    }

    override fun sync(storage: NonNullList<ItemStack>) {
        storedFuelStack = storage[0]
        remainingItemStack = storage[1]
    }

    override fun getStorageInventory(): NonNullList<ItemStack> {
        var list = NonNullList.withSize(2, ItemStack.EMPTY)
        list[0] = storedFuelStack
        list[1] = remainingItemStack
        return list
    }

    override fun getStorageInventorySize(): Int {
        return 1
    }

    override fun getBlockPositionFromISS(): BlockPos {
        return this.worldPosition
    }


    companion object {
        const val MAX_JOULES_PER_TICK = 10000.0
    }
}
