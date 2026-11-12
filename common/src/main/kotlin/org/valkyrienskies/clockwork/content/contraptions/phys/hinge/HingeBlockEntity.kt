package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.entity.player.Player
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max

class HingeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state), IHaveGoggleInformation {
    lateinit var lower: ScrollValueBehaviour
    lateinit var upper: ScrollValueBehaviour
    var guideBase: BlockPos? = null
        private set
    var guideHead: BlockPos? = null
        private set
    var strained = false
        private set
    private var stressSound: HingeStressSound? = null

    fun updateStrain(value: Boolean) {
        if (strained == value) return
        strained = value
        sendData()
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        if (clientPacket) {
            tag.putBoolean("Strained", strained)
            guideBase?.let { tag.putLong("GuideBase", it.asLong()) }
            guideHead?.let { tag.putLong("GuideHead", it.asLong()) }
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (clientPacket) {
            strained = tag.getBoolean("Strained")
            guideBase = if (tag.contains("GuideBase")) BlockPos.of(tag.getLong("GuideBase")) else null
            guideHead = if (tag.contains("GuideHead")) BlockPos.of(tag.getLong("GuideHead")) else null
        }
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        lower = Limit(this, false)
            .between(0, 180).withFormatter { "${-it}°" }.requiresWrench().withCallback { updateLimit(false, it) }
        upper = Limit(this, true)
            .between(0, 180).withFormatter { "+$it°" }.requiresWrench().withCallback { updateLimit(true, it) }
        lower.value = 90; upper.value = 90
        behaviours.add(lower); behaviours.add(upper)
    }

    private fun updateLimit(positive: Boolean, value: Int) {
        val world = level as? ServerLevel ?: return
        val data = HingeLinks.get(world)
        val link = data.at(blockPos) ?: return
        if (positive) link.upper = value else link.lower = -value
        val angle = HingeGeometry.angle(HingeLinks.worldRotation(world, link.a), HingeLinks.worldRotation(world, link.b))
        link.appliedLower = min(link.lower, floor(angle).toInt())
        link.appliedUpper = max(link.upper, ceil(angle).toInt())
        link.dirty = true
        link.age = 0
        data.setDirty()
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) {
            if (strained && stressSound?.isStopped != false) {
                stressSound = HingeStressSound(this).also { Minecraft.getInstance().soundManager.play(it) }
            }
            return
        }
        val world = level as? ServerLevel ?: return
        val link = HingeLinks.get(world).at(blockPos)
        val base = link?.takeIf { it.connected }?.a?.pos
        val head = link?.takeIf { it.connected }?.b?.pos
        var changed = guideBase != base || guideHead != head
        guideBase = base; guideHead = head
        if (link != null && (link.lower != -lower.value || link.upper != upper.value)) {
            lower.value = -link.lower; upper.value = link.upper
            setChanged()
            changed = true
        }
        if (changed) sendData()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Component.translatable("block.vs_clockwork.hinge.limits"))
        tooltip.add(Component.translatable("block.vs_clockwork.hinge.lower").append(": −${lower.value}°").withStyle(color(false)))
        tooltip.add(Component.translatable("block.vs_clockwork.hinge.upper").append(": +${upper.value}°").withStyle(color(true)))
        tooltip.add(Component.translatable("block.vs_clockwork.hinge.configure"))
        return true
    }

    private class Slot(private val positive: Boolean) : ValueBoxTransform.Sided() {
        override fun getScale() = .65f
        override fun getOverrideColor() = if (positive) POSITIVE_COLOR else NEGATIVE_COLOR
        override fun getSouthLocation() = Vec3(0.5, 0.5, 0.76)
        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val axis = HingeGeometry.axis(state)
            return direction == if (positive) axis else axis.opposite
        }
    }

    private class Limit(private val be: HingeBlockEntity, private val positive: Boolean) : ScrollValueBehaviour(
        Component.translatable("block.vs_clockwork.hinge.${if (positive) "upper" else "lower"}"), be, Slot(positive)) {
        override fun getType(): BehaviourType<*> = if (positive) UPPER else LOWER
        override fun netId() = if (positive) 1 else 0
        override fun getClipboardKey() = if (positive) "HingeUpper" else "HingeLower"
        override fun write(tag: CompoundTag, clientPacket: Boolean) { tag.putInt(clipboardKey, value) }
        override fun read(tag: CompoundTag, clientPacket: Boolean) {
            if (tag.contains(clipboardKey)) value = tag.getInt(clipboardKey).coerceIn(0, 180)
        }
        override fun readFromClipboard(tag: CompoundTag, player: Player?, side: Direction?, simulate: Boolean): Boolean {
            if (!tag.contains("Value")) return false
            if (!simulate) setValue(tag.getInt("Value"))
            return true
        }
        override fun getValueSettings() = ValueSettings(if (positive) 1 else 0, value)
        override fun setValueSettings(player: Player?, settings: ValueSettings, ctrlHeld: Boolean) {
            if (settings.row() !in 0..1) return
            val target = if (settings.row() == 1) be.upper else be.lower
            if (target.value == settings.value()) return
            target.setValue(settings.value())
            playFeedbackSound(this)
        }
        override fun createBoard(player: Player?, hit: BlockHitResult?) = ValueSettingsBoard(
            Component.translatable("block.vs_clockwork.hinge.limits"), 180, 15,
            listOf(Component.literal("−").withStyle(color(false)), Component.literal("+").withStyle(color(true))),
            ValueSettingsFormatter { Component.literal("${if (it.row() == 1) "+" else "−"}${it.value()}°").withStyle(color(it.row() == 1)) })
    }

    companion object {
        const val POSITIVE_COLOR = 0x61DCC5
        const val NEGATIVE_COLOR = 0xF39B77
        private fun color(positive: Boolean): Style = Style.EMPTY.withColor(TextColor.fromRgb(if (positive) POSITIVE_COLOR else NEGATIVE_COLOR))
        private val LOWER = BehaviourType<ScrollValueBehaviour>("hinge_lower")
        private val UPPER = BehaviourType<ScrollValueBehaviour>("hinge_upper")
    }
}
