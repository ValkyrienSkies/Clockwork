package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.Clearable
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.min

class CoalBurnerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), Clearable {


    var fuelTicks: Int = 0
    var maxBurnTime: Double = 0.0

    var storedFuelStack: ItemStack = ItemStack.EMPTY
    var remainingItemStack: ItemStack = ItemStack.EMPTY

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val kelvin = ClockworkMod.getKelvin()

        kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return
        if (fuelTicks>0) {
            fuelTicks-=1
            val currentInternalGasses = kelvin.getGasMassAt(blockPos.toDuctNodePos(level!!.dimension().location()))
            val currentInternalTemperature = kelvin.getTemperatureAt(blockPos.toDuctNodePos(level!!.dimension().location()))
            if (currentInternalGasses.values.sum() > 1e-5) {
                val currentInternalHeatCapacity = kelvin.mixtureCapacity(currentInternalGasses)
                val targetTemperature = 1000.0
                val maxEnergyAddedThisTick = (FUEL_ENERGY_DENSITY * (maxBurnTime / LOG_BURN_TIME)) / 20.0
                val energyToAdd = min(currentInternalHeatCapacity * (targetTemperature - currentInternalTemperature), maxEnergyAddedThisTick)
                if (energyToAdd > 0) {
                    kelvin.modHeatEnergy(blockPos.toDuctNodePos(level!!.dimension().location()), energyToAdd)
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

    companion object {
        const val FUEL_ENERGY_DENSITY = 17000.0 // J per kg
        const val LOG_BURN_TIME = 300.0 // ticks
    }
}
