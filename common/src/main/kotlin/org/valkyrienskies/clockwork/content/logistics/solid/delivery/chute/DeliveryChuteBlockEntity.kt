package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotBehaviour
import org.valkyrienskies.clockwork.platform.SolidDeliveryMethods
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class DeliveryChuteBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state) {

    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour

    var itemStack: ItemStack = ItemStack.EMPTY
    var busy = false

    val realPos: Vector3d get()
    { return vsApi.getShipManagingBlock(level, blockPos)?.positionToWorld(blockPos.toJOMLD()) ?: blockPos.toJOMLD() }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        frequencySlotBehaviour = FrequencySlotBehaviour(this,FrequencySlot())
        behaviours.add(frequencySlotBehaviour)
    }

    override fun tick() {
        if (this.level == null || this.level!!.isClientSide) return

        if (ActiveChutes.actives[this.worldPosition] == null)
            ActiveChutes.addChute(this.worldPosition, this)

        if (!itemStack.isEmpty) SolidDeliveryMethods.pushTo(level, this)
    }

    override fun remove() {
        ActiveChutes.removeChute(this.worldPosition)
        super.remove()
    }

    override fun destroy() {
        ActiveChutes.removeChute(this.worldPosition)
        super.destroy()
    }

    fun isOnShip(): Boolean {
        if (this.level!!.isClientSide) return false
        return (this.level!! as ServerLevel).getShipObjectManagingPos(this.worldPosition) != null
    }

    fun getVelocity(): Vector3dc? {
        return if (isOnShip()) {
            (this.level!! as ServerLevel).getShipObjectManagingPos(this.worldPosition)!!.velocity
        } else null
    }

    fun receiveItem(newStack: ItemStack, simulate: Boolean = false): Boolean {
        if (itemStack.isEmpty) {
            if (!simulate) itemStack = newStack
            return true
        } else {
            if (itemStack.`is`(newStack.item)) {
                if (itemStack.count + newStack.count <= itemStack.maxStackSize) {
                    if (!simulate) newStack.count += itemStack.count
                    return true
                }
            }
        }
        return false
    }

    private class FrequencySlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(level, pos, state) else Vec3(
                .5,
                10.5 / 16f,
                .5
            ).add(
                VecHelper.rotate(
                    VecHelper.voxelSpace(0.0, 0.0, -5.0), angle(state).toDouble(), Direction.Axis.Y
                )
            )
        }

        override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
            if (direction != Direction.UP) {
                super.rotate(level, pos, state, ms)
                return
            }
            TransformStack.of(ms)
                .rotateYDegrees(angle(state))
                .rotateXDegrees(90.0f)
        }

        private fun angle(state: BlockState): Float {
            return if (AllBlocks.WEIGHTED_EJECTOR.has(state)) AngleHelper.horizontalAngle(
                state.getValue(
                    EjectorBlock.HORIZONTAL_FACING
                )
            ) else 0f
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            return direction != Direction.UP && direction != Direction.DOWN
        }

        override fun getSouthLocation(): Vec3 {
            return if (direction == Direction.UP) Vec3.ZERO else VecHelper.voxelSpace(8.0, 6.0, 15.5)
        }
    }

}
