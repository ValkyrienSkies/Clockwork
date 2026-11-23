package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.Locale

internal class TireSizeScreen(private val hub: WheelHubBlockEntity, tire: WheelTire, private val mode: AirPumpSetting,
                              private val hand: InteractionHand) : ValueSettingsScreen(hub.blockPos, board(tire, mode),
    ValueSettings(0, mode.value(tire) - mode.range(tire).first), {}, 0) {
    private val bodyId = hub.bodyId
    private val initial = mode.value(tire)
    private val minimum = mode.range(tire).first

    override fun saveAndClose(mouseX: Double, mouseY: Double) {
        val selected = minimum + getClosestCoordinate(mouseX.toInt(), mouseY.toInt()).value()
        if (selected != initial)
            ClockworkPackets.sendToServer(ResizeTirePacket(hub.blockPos, hand, bodyId, mode, initial, selected))
        onClose()
    }

    companion object {
        private fun board(tire: WheelTire, mode: AirPumpSetting): ValueSettingsBoard {
            val range = mode.range(tire)
            val base = if (mode == AirPumpSetting.WIDTH) tire.size.width else tire.size.radius - tire.rimRadius
            return ValueSettingsBoard(mode.label, range.last - range.first, 5, listOf(Component.literal("%")),
                ValueSettingsFormatter { settings ->
                    val percent = (range.first + settings.value()) * 5
                    Component.literal(String.format(Locale.ROOT, "%d%% (%.2f m)", percent, base * percent / 100))
                })
        }
    }
}
