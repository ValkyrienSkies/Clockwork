package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.chassis.StickerBlock
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueItem
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import java.util.function.Supplier

class SlickerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {

    var piston: LerpedFloat? = null
    var update = false

    var attachedShipId: Long = -1

    init {
        piston = LerpedFloat.linear()
        update = false
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun initialize() {
        super.initialize()
        if (!level!!.isClientSide) return
        piston!!.startWithValue((if (isBlockStateExtended()) 1 else 0).toDouble())
    }

    fun isBlockStateExtended(): Boolean {
        val blockState = blockState
        return AllBlocks.STICKER.has(blockState) && blockState.getValue(StickerBlock.EXTENDED)
    }

    override fun tick() {
        super.tick()
        if (!level!!.isClientSide) return
        piston!!.tickChaser()
        if (isAttachedToShip() && piston!!.getValue(0f) != piston!!.value && piston!!.value == 1f) {
            SuperGlueItem.spawnParticles(
                level, worldPosition, blockState.getValue(
                    StickerBlock.FACING
                ), true
            )
            playSound(true)
        }
        if (!update) return
        update = false
        val target = if (isBlockStateExtended()) 1 else 0
        if (isAttachedToShip() && target == 0 && piston!!.chaseTarget == 1f) playSound(false)
        piston!!.chase(target.toDouble(), .4, LerpedFloat.Chaser.LINEAR)
        InstancedRenderDispatcher.enqueueUpdate(this)
    }

    fun isAttachedToShip(): Boolean {
        val blockState = blockState
        if (!AllBlocks.STICKER.has(blockState)) return false

        return attachedShipId != -1L
    }

    fun getCheckAABB(): AABB {
        val blockState = blockState
        val direction = blockState.getValue(StickerBlock.FACING)
        val pos = worldPosition
        return AABB(pos.relative(direction))
    }

    override fun write(tag: CompoundTag?, clientPacket: Boolean) {
        super.write(tag, clientPacket)
    }

    override fun read(compound: CompoundTag?, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (clientPacket) update = true
    }

    fun playSound(attach: Boolean) {
        AllSoundEvents.SLIME_ADDED.playAt(
            level,
            worldPosition,
            0.35f,
            if (attach) 0.75f else 0.2f,
            false
        )
    }
}