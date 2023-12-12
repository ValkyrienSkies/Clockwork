package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.Lang
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class DeliveryChuteBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(typeIn, pos, state), ISyncableStorage {

    private var inventory: NonNullList<ItemStack> = NonNullList.withSize(1, ItemStack.EMPTY)
    private var previousInventory: NonNullList<ItemStack> = inventory
    var id = 0

    private lateinit var idBehavior: ScrollValueBehaviour

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {

        idBehavior = ScrollValueBehaviour(
            Lang.translateDirect("delivery.identifier"),
            this,
            ChuteSlot()
        )
            .between(1, 99)
            .requiresWrench()
            .withFormatter { i: Int -> if (i == 0) "*" else i.toString() }

        behaviours.add(idBehavior)
        super.addBehaviours(behaviours)
    }

    override fun tick() {
        if (this.level == null) return

        if (this.level!!.isClientSide) return

        if (!ActiveChutes.hasChute(this.worldPosition)) {
            ActiveChutes.addChute(this.worldPosition, this)
        }

        id = idBehavior.value

        if (previousInventory != inventory) {
            ClockworkPackets.sendToNear(
                this.level!! as ServerLevel,
                this.worldPosition,
                64,
                SyncableStoragePacket(this)
            )
        }
        previousInventory = inventory
    }

    override fun remove() {
        ActiveChutes.removeChute(this.worldPosition)
        super.remove()
    }

    override fun sync(storage: NonNullList<ItemStack>) {
        this.inventory = storage
    }

    override fun getStorageInventory(): NonNullList<ItemStack> {
        return this.inventory
    }

    override fun getStorageInventorySize(): Int {
        return 1
    }

    private fun isOnShip(): Boolean {
        if (this.level!!.isClientSide) return false
        return (this.level!! as ServerLevel).getShipObjectManagingPos(this.worldPosition) != null
    }

    fun getRealPos(): Vector3dc {
        return if (isOnShip()) {
            (this.level!! as ServerLevel).getShipObjectManagingPos(this.worldPosition)!!.transform.shipToWorld.transformPosition(
                this.worldPosition.toJOMLD()
            )
        } else {
            this.worldPosition.toJOMLD()
        }
    }

    fun receiveItem(itemStack: ItemStack): Boolean {
        if (inventory[0].isEmpty) {
            inventory[0] = itemStack
            return true
        } else {
            if (inventory[0].`is`(itemStack.item)) {
                if (inventory[0].count + itemStack.count <= inventory[0].maxStackSize) {
                    inventory[0].count += itemStack.count
                    return true
                }
            }
        }
        return false
    }

    override fun clearContent() {
        this.inventory[0] = ItemStack.EMPTY
    }

    override fun getContainerSize(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return this.inventory[0].isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return this.inventory[0]
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return this.inventory[0].split(amount)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = this.inventory[0]
        this.inventory[0] = ItemStack.EMPTY
        return stack
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        this.inventory[0] = stack
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        return IntArray(0)
    }

    override fun canPlaceItemThroughFace(index: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        return false
    }

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean {
        return true
    }

    override fun getBlockPositionFromISS(): BlockPos {
        return this.worldPosition
    }


    private class ChuteSlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(state) else Vec3(.5, 10.5 / 16f, .5).add(
                VecHelper.rotate(
                    VecHelper.voxelSpace(0.0, 0.0, -5.0), angle(state).toDouble(), Direction.Axis.Y
                )
            )
        }

        override fun rotate(state: BlockState, ms: PoseStack) {
            if (direction != Direction.UP) {
                super.rotate(state, ms)
                return
            }
            TransformStack.cast(ms)
                .rotateY(angle(state).toDouble())
                .rotateX(90.0)
        }

        private fun angle(state: BlockState): Float {
            return if (AllBlocks.WEIGHTED_EJECTOR.has(state)) AngleHelper.horizontalAngle(state.getValue(EjectorBlock.HORIZONTAL_FACING)) else 0f
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            return (direction.axis === state.getValue(EjectorBlock.HORIZONTAL_FACING).axis || direction == Direction.UP)
        }

        override fun getSouthLocation(): Vec3 {
            return if (direction == Direction.UP) Vec3.ZERO else VecHelper.voxelSpace(8.0, 6.0, 15.5)
        }
    }

}