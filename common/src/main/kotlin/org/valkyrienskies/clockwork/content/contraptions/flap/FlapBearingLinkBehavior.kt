package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.redstone.link.LinkBehaviour
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import it.unimi.dsi.fastutil.ints.IntConsumer
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.tuple.Pair
import java.lang.reflect.Field

class FlapBearingLinkBehavior(be: SmartBlockEntity,
                              slots: Pair<ValueBoxTransform, ValueBoxTransform>, val recieverFunc: IntConsumer, val first: Boolean = true
): LinkBehaviour(be, slots) {

    val slotOne: ValueBoxTransform
    val slotTwo: ValueBoxTransform

    /**
     * This is disgusting, awful, and I hate the create devs and anyone who's ever touched that codebase for making me implement this scar on humanity
     * But most of all
     * I hate myself for writing it
     */
    private fun setLinkMode() {
        val enumClass = Class.forName("com.simibubi.create.content.redstone.link.LinkBehaviour\$Mode")
        val enumValue = java.lang.Enum.valueOf(enumClass as Class<out Enum<*>>, "RECEIVE")

        val recieverField: Field = LinkBehaviour::class.java.getDeclaredField("signalCallback")
        val enumField: Field = LinkBehaviour::class.java.getDeclaredField("mode")

        enumField.isAccessible = true
        recieverField.isAccessible = true

        recieverField.set(this, recieverFunc)
        enumField.set(this, enumValue)
    }

    override fun getType(): BehaviourType<*> {
        return TYPE
    }

    init {
        setLinkMode()
        slotOne = slots.left
        slotTwo = slots.right
    }

    override fun write(nbt: CompoundTag?, clientPacket: Boolean) {
        super.write(nbt, clientPacket)
        val addition = if (first) {
            "First"
        } else {
            "Second"
        }

        nbt!!.put(
            "${addition}FrequencyFirst", networkKey.first.stack
                .save(CompoundTag())
        )
        nbt.put(
            "${addition}FrequencyLast", networkKey.second.stack
                .save(CompoundTag())
        )
        nbt.putLong(
            "${addition}LastKnownPosition", blockEntity.blockPos
                .asLong()
        )
    }

    override fun read(nbt: CompoundTag?, clientPacket: Boolean) {
        super.read(nbt, clientPacket)
        val addition = if (first) {
            "First"
        } else {
            "Second"
        }
        val positionInTag = blockEntity.blockPos
            .asLong()
        newPosition = nbt!!.getLong("${addition}LastKnownPosition") != positionInTag

        setFrequency(true, ItemStack.of(nbt.getCompound("${addition}FrequencyFirst")))
        setFrequency(false, ItemStack.of(nbt.getCompound("${addition}FrequencyLast")))
    }

    companion object {
        @JvmStatic
        val TYPE: BehaviourType<FlapBearingLinkBehavior> = BehaviourType()
    }
}