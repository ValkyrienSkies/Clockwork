package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class FrequencySlotBehaviour(be: SmartBlockEntity?, val slot: ValueBoxTransform) : BlockEntityBehaviour(be), ClipboardCloneable  {



    var frequency: Frequency = Frequency.EMPTY

    override fun write(nbt: CompoundTag, clientPacket: Boolean) {
        super.write(nbt, clientPacket)
        nbt.put(
            "Frequency", frequency.stack
                .save(CompoundTag())
        )

    }

    override fun read(nbt: CompoundTag, clientPacket: Boolean) {

        super.read(nbt, clientPacket)
        frequency = Frequency.of(ItemStack.of(nbt.getCompound("Frequency")))
    }

    fun setFrequency(stack: ItemStack) {
        var stack = stack
        stack = stack.copy()
        stack.count = 1

        frequency = Frequency.of(stack)

        blockEntity.sendData()

    }

    override fun getType(): BehaviourType<*> {
        return TYPE
    }

    override fun getClipboardKey(): String {
        return "DeliveryFrequencies"
    }

    override fun writeToClipboard(tag: CompoundTag, side: Direction?): Boolean {
        tag.put(
            "Frequency", frequency.stack
                .save(CompoundTag())
        )
        return true
    }

    override fun readFromClipboard(tag: CompoundTag, player: Player?, side: Direction?, simulate: Boolean): Boolean {
        if (!tag.contains("Frequency")) return false
        if (simulate) return true
        setFrequency(ItemStack.of(tag.getCompound("Frequency")))
        return true
    }

    fun testHit( hit: Vec3): Boolean {
        val state = blockEntity.blockState
        val localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.blockPos))
        return slot.testHit(state, localHit)
    }


    companion object {
        val TYPE: BehaviourType<FrequencySlotBehaviour> = BehaviourType()
    }

}