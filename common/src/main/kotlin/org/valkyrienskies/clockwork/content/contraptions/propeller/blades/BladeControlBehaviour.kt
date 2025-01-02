package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player

class BladeControlBehaviour(be: SmartBlockEntity?) : BlockEntityBehaviour(be), ClipboardCloneable {
    var bladeAngle = 0.0
    var bladeLength = 1

    override fun getType(): BehaviourType<*> {
        return TYPE
    }

    override fun getClipboardKey(): String {
        return "Blades"
    }

    override fun writeToClipboard(tag: CompoundTag, side: Direction): Boolean {
        tag.putDouble("BladeAngle", bladeAngle)
        tag.putInt("BladeLength", bladeLength)
        return true
    }

    override fun readFromClipboard(tag: CompoundTag, player: Player, side: Direction, simulate: Boolean): Boolean {
        if (tag.contains("BladeAngle")) {
            bladeAngle = tag.getDouble("BladeAngle")
        } else return false
        if (tag.contains("BladeLength")) {
            bladeLength = tag.getInt("BladeLength")
        } else return false
        return true
    }

    companion object {
        val TYPE = BehaviourType<BladeControlBehaviour>("blade_control")
    }
}