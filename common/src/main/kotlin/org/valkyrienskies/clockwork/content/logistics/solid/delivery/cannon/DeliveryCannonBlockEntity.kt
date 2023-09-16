package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket

class DeliveryCannonBlockEntity(typeIn: BlockEntityType<DeliveryCannonBlockEntity>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos, state), ISyncableStorage {

    var fireRate: Float = 0f
    var currentTarget: BlockPos? = null
    var currentTargetWorldPos: Vector3dc? = null

    var canFire: Boolean = false
    var timeSinceFired: Float = 0f

    var inventory = NonNullList.withSize(1, ItemStack.EMPTY)
    var previousInventory: NonNullList<ItemStack> = inventory

    var range: Double = 256.0

    var id: Int = 1


    override fun tick() {
        super.tick()
        if (this.level == null) return
        if (this.level!!.isClientSide) return

        this.fireRate = this.speed / 256f

        if (previousInventory != inventory) {
            ClockworkPackets.sendToNear(
                this.level!! as ServerLevel,
                this.worldPosition,
                64,
                SyncableStoragePacket(this))
        }
        previousInventory = inventory

        searchTargets()
        if (currentTarget == null) return

        if (!this.canFire) {
            this.timeSinceFired += this.fireRate
        }
        if (this.timeSinceFired >= 5f) {
            this.canFire = true
            this.timeSinceFired = 0f
        }

        if (this.canFire) {
            fireItemToTarget()
        }
    }

    private fun searchTargets() {
        currentTarget = ActiveChutes.getNearestChuteWithId(this.worldPosition, range, id)
        currentTargetWorldPos = ActiveChutes.getChuteRealPos(currentTarget!!)
    }

    private fun fireItemToTarget(): Boolean {
        if (currentTarget == null) {
            canFire = false
            return false
        }
        //visual stuff

        //server only beyond this point
        return if (this.level!!.getBlockEntity(currentTarget!!) is DeliveryChuteBlockEntity) {
            val targetEntity = this.level!!.getBlockEntity(currentTarget!!) as DeliveryChuteBlockEntity
            if (targetEntity.receiveItem(this.inventory[0])) {
                this.inventory[0] = ItemStack.EMPTY
                canFire = false
                true
            } else {
                canFire = false
                false
            }
        } else {
            canFire = false
            false
        }
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
        return this.worldPosition
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
        return false
    }
}