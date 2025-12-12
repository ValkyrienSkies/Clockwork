package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.AllKeys
import com.simibubi.create.AllPackets
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import java.util.function.Consumer

class BladeControlScreen(pos: BlockPos, board: ValueSettingsBoard,
                         valueSettings: ValueSettingsBehaviour.ValueSettings,
                         onHover: Consumer<ValueSettingsBehaviour.ValueSettings>, netId: Int
) : ValueSettingsScreen(pos, board, valueSettings, onHover, netId) {
    override fun saveAndClose(pMouseX: Double, pMouseY: Double) {
        val closest = getClosestCoordinate(pMouseX.toInt(), pMouseY.toInt())

        onClose()

    }
}
