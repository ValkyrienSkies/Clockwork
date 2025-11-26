package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.item.ItemHelper
import dev.engine_room.flywheel.lib.transform.TransformStack
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil
import io.github.fabricators_of_create.porting_lib.util.StorageProvider
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotBehaviour
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class DeliveryChuteBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state), ISyncableStorage {

    lateinit var capBelow: StorageProvider<ItemVariant>

    private var inventory: NonNullList<ItemStack> = NonNullList.withSize(1, ItemStack.EMPTY)
    private var previousInventory: NonNullList<ItemStack> = inventory


    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour
    var busy = false

    val realPos: Vector3d? get()
    { return vsApi.getShipManagingBlock(level, blockPos)?.positionToWorld(blockPos.toJOMLD()) }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {

        frequencySlotBehaviour = FrequencySlotBehaviour(this,FrequencySlot())

        behaviours.add(frequencySlotBehaviour)

    }

    override fun tick() {
        if (this.level == null || this.level!!.isClientSide) return

        if (ActiveChutes.actives[this.worldPosition] == null) {
            ActiveChutes.addChute(this.worldPosition, this)
        }

        if (previousInventory != inventory) {
            ClockworkPackets.sendToNear(
                this.level!! as ServerLevel,
                this.worldPosition,
                64,
                SyncableStoragePacket(this)
            )
        }
        previousInventory = inventory

        if (!isEmpty) handleDownwardOutput(false)
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        capBelow = StorageProvider.createForItems(level, worldPosition.below())
    }

    override fun remove() {
        ActiveChutes.removeChute(this.worldPosition)
        super.remove()
    }

    override fun destroy() {
        ActiveChutes.removeChute(this.worldPosition)
        super.destroy()
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

    override fun getBlockPositionFromISS(): BlockPos {
        return worldPosition
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

    fun receiveItem(itemStack: ItemStack, simulate: Boolean): Boolean {
        if (inventory[0].isEmpty) {
            if (!simulate) inventory[0] = itemStack
            return true
        } else {
            if (inventory[0].`is`(itemStack.item)) {
                if (inventory[0].count + itemStack.count <= inventory[0].maxStackSize) {
                    if (!simulate) inventory[0].count += itemStack.count
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

    fun handleDownwardOutput(simulate: Boolean): Boolean {


//		if (level == null) return false
//		val inv  = grabCapability(Direction.DOWN);
//		if (!isEmpty) {
//            if (level!!.isClientSide && !isVirtual)
//                return false;
//
//
//            TransferUtil.getTransaction().use { t ->
//                val inserted = inv!!.insert(
//                    ItemVariant.of(getItem(0)),
//                    getItem(0).getCount().toLong(),
//                    t
//                )
//                if (inserted != 0L && !simulate) t.commit()
//                val held = getItem(0)
//                if (!simulate) {
//                    val newStack = held.copy()
//                    newStack.shrink(ItemHelper.truncateLong(inserted))
//                    setItem(0, newStack)
//                }
//                if (inserted != 0L) return true
//            }
//        }
//        return false
   }

    public class FrequencySlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(level, pos, state) else Vec3(.5, 10.5 / 16f, .5).add(
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
            return if (AllBlocks.WEIGHTED_EJECTOR.has(state)) AngleHelper.horizontalAngle(state.getValue(EjectorBlock.HORIZONTAL_FACING)) else 0f
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            return direction != Direction.UP && direction != Direction.DOWN
        }

        override fun getSouthLocation(): Vec3 {
            return if (direction == Direction.UP) Vec3.ZERO else VecHelper.voxelSpace(8.0, 6.0, 15.5)
        }
    }

}
