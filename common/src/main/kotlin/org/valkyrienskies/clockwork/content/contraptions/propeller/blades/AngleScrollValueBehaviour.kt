package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.google.common.collect.ImmutableList
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.*
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.Components
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.abs
import kotlin.math.max

class AngleScrollValueBehaviour(label: Component, be: SmartBlockEntity, slot: ValueBoxTransform) : ScrollValueBehaviour(label, be, slot) {
    override fun getType(): BehaviourType<*> {
        return super.getType()
    }

    override fun createBoard(player: Player?, hitResult: BlockHitResult?): ValueSettingsBoard {
        val rows = ImmutableList.of<Component>(
            Components.literal("\u27f3")
                .withStyle(ChatFormatting.BOLD),
            Components.literal("\u27f2")
                .withStyle(ChatFormatting.BOLD)
        )
        val formatter = ValueSettingsFormatter { settings: ValueSettingsBehaviour.ValueSettings ->
            this.formatSettings(
                settings
            )
        }
        return ValueSettingsBoard(label, 180, 10, rows, formatter)
    }

    override fun setValueSettings(
        player: Player?,
        valueSetting: ValueSettingsBehaviour.ValueSettings,
        ctrlHeld: Boolean
    ) {
        val value = max(0.0, valueSetting.value().toDouble()).toInt()
        if (valueSetting != valueSettings) playFeedbackSound(this)
        setValue(if (valueSetting.row() == 0) -value else value)
    }

    override fun getValueSettings(): ValueSettingsBehaviour.ValueSettings {
        return ValueSettingsBehaviour.ValueSettings(if (value < 0) 0 else 1, abs(value.toDouble()).toInt())
    }

    fun formatSettings(settings: ValueSettingsBehaviour.ValueSettings): MutableComponent {
        return Lang.number(max(1.0, abs(settings.value().toDouble())))
            .add(
                Lang.text(if (settings.row() == 0) "\u27f3" else "\u27f2")
                    .style(ChatFormatting.BOLD)
            )
            .component()
    }

    override fun getClipboardKey(): String {
        return "BladeAngle"
    }
}