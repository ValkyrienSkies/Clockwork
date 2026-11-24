package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections

internal class GasPressureSetting(
    entity: SmartBlockEntity, private val defaultKPa: Int, private val maxKPa: Int, facingProperty: DirectionProperty,
) : ScrollValueBehaviour(Component.translatable("vs_clockwork.gas_control.pressure"), entity, Slot(facingProperty)) {

    init {
        between(0, maxKPa)
        withFormatter { "$it kPa" }
        setValue(defaultKPa)
        requiresWrench()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        value = if (tag.contains("ScrollValue")) value.coerceIn(0, maxKPa) else defaultKPa
    }

    fun target(entity: SmartBlockEntity, side: Direction): Double {
        val input = ThinkableConnections.input(entity.level, entity.blockPos, side)
        return if (input == null) value * 1000.0 else (input.value ?: 0.0).coerceIn(0.0, maxKPa * 1000.0)
    }

    private class Slot(private val facingProperty: DirectionProperty) : ValueBoxTransform.Sided() {
        override fun isSideActive(state: BlockState, side: Direction): Boolean =
            side.axis.isHorizontal && side.axis != state.getValue(facingProperty).axis

        override fun getSouthLocation() = Vec3(0.5, 0.7, 0.85)
    }
}
