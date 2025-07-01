package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Clearable
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeSyncPacket
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*

class CoalBurnerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), Clearable {


    var fuelTicks: Int = 0

    var storedFuelStack: ItemStack = ItemStack.EMPTY

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return




        if (fuelTicks>0) {
            fuelTicks-=1
            if (kelvin.getTemperatureAt(blockPos.toDuctNodePos(level!!.dimension().location()))<2000.0) kelvin.modHeatEnergy(blockPos.toDuctNodePos(level!!.dimension().location()),1000.0)

            if (blockState.getValue(CoalBurnerBlock.LIT)==false) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,true), 15)
        } else {
            if (storedFuelStack.isEmpty and blockState.getValue(CoalBurnerBlock.LIT)) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,false), 15)

            if (!storedFuelStack.isEmpty) {
                fuelTicks += FuelRegistry.get(storedFuelStack)
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
        val subTag = tag.get("StoredFuelStack") as CompoundTag
        storedFuelStack = ItemStack.of(subTag)
        println("$storedFuelStack   $subTag")

        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        // We use a sub tag, instead of saving directly to BE tag
        // So that it doesn't load the burner block as the item
        val subTag = CompoundTag()
        storedFuelStack.save(subTag)
        tag.put("StoredFuelStack", subTag)
        println("$storedFuelStack   $subTag")
        super.write(tag, clientPacket)
    }

    override fun destroy() {
        val vec3d = blockPos.toJOMLD()

        val ie = ItemEntity(level!!, vec3d.x, vec3d.y, vec3d.z, storedFuelStack)
        level!!.addFreshEntity(ie)
        super.destroy()
    }

    override fun clearContent() {
        storedFuelStack = ItemStack.EMPTY
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        if (!storedFuelStack.isEmpty) {
            tooltip.add(Component.literal("    Coal burner Info").withStyle(ChatFormatting.GRAY))
            tooltip.add(Component.literal("Fuel: ").withStyle(ChatFormatting.GOLD)
                .append(storedFuelStack.displayName)
                .append((Component.literal("x ${storedFuelStack.count}")).withStyle(ChatFormatting.GOLD)))
        }

        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}