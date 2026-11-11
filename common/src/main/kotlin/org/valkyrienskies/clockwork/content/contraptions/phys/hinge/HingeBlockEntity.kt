package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
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

class HingeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    lateinit var lower: ScrollValueBehaviour
    lateinit var upper: ScrollValueBehaviour
    private var syncing = false
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
        if (clientPacket) tag.putBoolean("Strained", strained)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (clientPacket) strained = tag.getBoolean("Strained")
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        lower = Limit(this, false)
            .between(0, 180).withFormatter { "${-it}°" }.requiresWrench().withCallback { updateLimits() }
        upper = Limit(this, true)
            .between(0, 180).withFormatter { "$it°" }.requiresWrench().withCallback { updateLimits() }
        syncing = true
        lower.setValue(90); upper.setValue(90)
        syncing = false
        behaviours.add(lower); behaviours.add(upper)
    }

    private fun updateLimits() {
        if (syncing || !::lower.isInitialized || !::upper.isInitialized) return
        val world = level as? ServerLevel ?: return
        val data = HingeLinks.get(world)
        val link = data.at(blockPos) ?: return
        link.lower = -lower.value; link.upper = upper.value
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
        val link = HingeLinks.get(world).at(blockPos) ?: return
        if (link.lower == -lower.value && link.upper == upper.value) return
        syncing = true
        lower.setValue(-link.lower); upper.setValue(link.upper)
        syncing = false
        sendData()
    }

    private class Slot(private val positive: Boolean) : ValueBoxTransform.Sided() {
        override fun getSouthLocation() = Vec3(0.5, 0.5, 0.76)
        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val axis = HingeGeometry.axis(state)
            return direction == if (positive) axis else axis.opposite
        }
    }

    private class Limit(be: HingeBlockEntity, private val positive: Boolean) : ScrollValueBehaviour(
        Component.translatable("block.vs_clockwork.hinge.${if (positive) "upper" else "lower"}"), be, Slot(positive)) {
        override fun getType(): BehaviourType<*> = if (positive) UPPER else LOWER
        override fun getClipboardKey() = if (positive) "HingeUpper" else "HingeLower"
        override fun write(tag: CompoundTag, clientPacket: Boolean) { tag.putInt(clipboardKey, value) }
        override fun read(tag: CompoundTag, clientPacket: Boolean) {
            if (tag.contains(clipboardKey)) value = tag.getInt(clipboardKey).coerceIn(0, 180)
        }
        override fun createBoard(player: Player?, hit: BlockHitResult?) = ValueSettingsBoard(label, 180, 15,
            listOf(Component.literal("°")), ValueSettingsFormatter { Component.literal("${if (positive) it.value() else -it.value()}°") })
    }

    companion object {
        private val LOWER = BehaviourType<ScrollValueBehaviour>("hinge_lower")
        private val UPPER = BehaviourType<ScrollValueBehaviour>("hinge_upper")
    }
}
