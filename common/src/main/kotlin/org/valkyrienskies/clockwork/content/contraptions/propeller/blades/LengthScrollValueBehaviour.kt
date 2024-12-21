package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.google.common.collect.ImmutableList
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.*
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.Components
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult

class LengthScrollValueBehaviour(label: Component, be: SmartBlockEntity, slot: ValueBoxTransform) : ScrollValueBehaviour(label, be, slot) {
    override fun getType(): BehaviourType<*> {
        return LENGTH_TYPE
    }

    override fun getClipboardKey(): String {
        return "BladeLength"
    }

    override fun write(nbt: CompoundTag, clientPacket: Boolean) {
        nbt.putInt("ScrollValueLength", value)
    }

    override fun read(nbt: CompoundTag, clientPacket: Boolean) {
        value = nbt.getInt("ScrollValueLength")
    }

    override fun createBoard(player: Player?, hitResult: BlockHitResult?): ValueSettingsBoard {
        return ValueSettingsBoard(
            label, max, 1, ImmutableList.of<Component>(Components.literal("Length")),
            ValueSettingsFormatter { obj: ValueSettingsBehaviour.ValueSettings -> obj.format() });
    }

    companion object {
        val LENGTH_TYPE = BehaviourType<LengthScrollValueBehaviour>("second_scroll_value")
    }
}