package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction

class ThrottleLeverBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), IThinkable, IHaveGoggleInformation {
    private lateinit var setting: ScrollValueBehaviour
    val throttle get() = setting.value / 100.0
    private var previousAngle = -45f
    private var displayedAngle = -45f

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        setting = object : ScrollValueBehaviour(Component.translatable("block.vs_clockwork.throttle_lever.setting"), this, SettingSlot()) {
            override fun read(tag: CompoundTag, clientPacket: Boolean) {
                super.read(tag, clientPacket)
                value = value.coerceIn(0, 100)
            }
        }.between(0, 100).withFormatter { "$it%" }.withCallback { updatePower() }.requiresWrench()
        behaviours.add(setting)
    }

    internal fun adjust(step: Int) {
        setting.setValue(setting.value + step)
    }

    override fun tick() {
        super.tick()
        val world = level ?: return
        if (world.isClientSide) {
            previousAngle = displayedAngle
            displayedAngle = Mth.lerp(.5f, displayedAngle, -45f + setting.value * .9f)
            return
        }
        if (world !is ServerLevel || VdexConstruction.locked(world, blockPos)) return
        val adjustment = DrivingControlLinks.get(world).controller(world, blockPos)?.throttleAdjustment ?: 0
        if (adjustment != 0) adjust(adjustment)
        updatePower()
    }

    private fun updatePower() {
        val world = level as? ServerLevel ?: return
        val state = blockState
        if (state.block !is ThrottleLeverBlock) return
        val power = (setting.value * 15 + 50) / 100
        if (state.getValue(ThrottleLeverBlock.POWER) != power)
            world.setBlock(blockPos, state.setValue(ThrottleLeverBlock.POWER, power), Block.UPDATE_ALL)
    }

    fun renderedAngle(partialTicks: Float) = Mth.lerp(partialTicks, previousAngle, displayedAngle)

    override fun providesThinkable(side: Direction) = true
    override fun thinkableValue(side: Direction) = throttle
    override fun thinkableValue(side: Direction, reading: String): Double? =
        if (reading.isEmpty() || reading == "throttle") throttle else null

    override fun thinkableReadings(side: Direction) =
        listOf(ThinkableReading("throttle", Component.translatable("thinkable.vs_clockwork.port.throttle_lever")))

    override fun thinkablePortName(side: Direction): Component =
        Component.translatable("thinkable.vs_clockwork.port.throttle_lever")

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Component.translatable("block.vs_clockwork.throttle_lever.readout", setting.value))
        return true
    }

    private class SettingSlot : ValueBoxTransform.Sided() {
        override fun isSideActive(state: BlockState, side: Direction) = side == Direction.UP
        override fun getSouthLocation() = Vec3(.5, .5, .875)
    }
}
