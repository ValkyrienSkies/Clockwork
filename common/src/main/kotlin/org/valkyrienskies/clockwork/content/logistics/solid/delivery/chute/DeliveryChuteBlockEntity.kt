package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dev.engine_room.flywheel.lib.transform.TransformStack
import kotlinx.coroutines.CompletableDeferred
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.chat.Component
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
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.gui.ClockworkTooltipHelper
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class DeliveryChuteBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state), IHaveGoggleInformation {

    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour

    var itemStack: ItemStack = ItemStack.EMPTY
    var busy = false
    var stuck = false

    val realPos: Vector3d get()
    { return ClockworkUtils.getRealPos(level, blockPos) }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        frequencySlotBehaviour = FrequencySlotBehaviour(this,FrequencySlot())
        behaviours.add(frequencySlotBehaviour)
    }

    override fun tick() {
        val level = this.level ?: return
        if (level.isClientSide) return

        ActiveChutes.addChute(level, this.worldPosition)

        if (!itemStack.isEmpty) SolidDeliveryMethods.pushTo(level, this)
        stuck = !itemStack.isEmpty
    }

    override fun remove() {
        ActiveChutes.removeChute(level, this.worldPosition)
        super.remove()
    }

    override fun destroy() {
        ActiveChutes.removeChute(level, this.worldPosition)
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
            if (!simulate) {
                itemStack = newStack
                sendData()
            }
            return true
        } else {
            if (itemStack.`is`(newStack.item)) {
                if (itemStack.count + newStack.count <= itemStack.maxStackSize) {
                    if (!simulate) {
                        itemStack.count += newStack.count
                        sendData()
                    }
                    return true
                }
            }
        }
        return false
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        itemStack = ItemStack.of(tag.getCompound("item"))
        stuck = tag.getBoolean("stuck")
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.put("item", itemStack.save(CompoundTag()))
        tag.putBoolean("stuck", stuck)
        super.write(tag, clientPacket)
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        if (stuck) {
            ClockworkTooltipHelper.addTitleAndHint(
                tooltip,
                "gui.delivery_chute.info.obstructed.title",
                "gui.delivery_chute.info.obstructed")
            return true
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
