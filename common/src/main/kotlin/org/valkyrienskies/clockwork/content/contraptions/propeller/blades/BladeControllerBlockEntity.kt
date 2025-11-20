package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket

class BladeControllerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
), ISyncableStorage {

    var previousBladeCount = 0

    var blades: NonNullList<ItemStack> = NonNullList.withSize(8, ItemStack.EMPTY)

    var bladeAngle: Double = 0.0

    var clientBladeAngle = LerpedFloat.angular()
        .chase(bladeAngle, 0.5, LerpedFloat.Chaser.EXP)
    var bladeCooldown = 0

    lateinit var angleController: AngleScrollValueBehaviour
    lateinit var lengthController: LengthScrollValueBehaviour

    var clientBladeRotation = HashMap<Int, LerpedFloat>().withDefault { LerpedFloat.linear().chase(0.0, 0.5, LerpedFloat.Chaser.EXP) }



    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        this.angleController = AngleScrollValueBehaviour(Component.translatable("vs_clockwork.blade_controller.angle"), this, AngleControllerValueBoxTransform())
        this.angleController.between(-180,180) // Should do this in controller class, but due to certain questionable create coding decisions, we're doing it here
        //TODO: Figure out how to make lengthController work
        //this.lengthController = LengthScrollValueBehaviour(TranslatableComponent("vs_clockwork.blade_controller.length"), this, LengthControllerValueBoxTransform())

        this.angleController.withCallback{i -> this.updateBladeAngle(i.toDouble())};



        //behaviours.add(this.lengthController)
        behaviours.add(this.angleController)
    }

    fun updateBladeAngle(angle: Double) {
        if (level == null || level!!.isClientSide) {
            return
        } else {
            this.bladeAngle = angle
            notifyUpdate()
        }
    }


    fun getAllBlades(): List<ItemStack> {
        val list = ArrayList<ItemStack>()
        if (this.isEmpty) {
            return list
        }
        for (i in this.blades.indices) {
            if (!this.blades[i].isEmpty) {
                list.add(this.blades[i])
            }
        }
        return list
    }

    override fun tick() {
        super.tick()
        if (bladeCooldown > 0) bladeCooldown--
        if (level?.isClientSide == true) {
            val angleBetweenBlades = 360.0 / this.getBladeCount().toDouble()

            if (previousBladeCount != getBladeCount()) {
                for (i in blades.indices) {
                    clientBladeRotation[i]?.chase(angleBetweenBlades * i.toDouble(), 0.5, LerpedFloat.Chaser.EXP) ?: run {
                        clientBladeRotation[i] = LerpedFloat.linear().chase(angleBetweenBlades * i.toDouble(), 0.5, LerpedFloat.Chaser.EXP)
                    }
                }
                previousBladeCount = getBladeCount()
            }

            clientBladeAngle.tickChaser()
            for (i in blades.indices) {
                clientBladeRotation[i]?.tickChaser()
            }
        }

        if (level?.isClientSide() == false) {
            val sLevel = level as ServerLevel

            if (this.previousBladeCount != this.getBladeCount()) {
                ClockworkPackets.sendToNear(
                    sLevel,
                    this.worldPosition,
                    64,
                    SyncableStoragePacket(this)
                )
                this.previousBladeCount = this.getBladeCount()
            }
        }
    }

    override fun remove() {
        super.remove()
    }

    override fun destroy() {
        super.destroy()
        dropBlades()
    }

    override fun invalidate() {
        super.invalidate()
    }

    fun dropBlades() {
        if (level != null && level is ServerLevel) {
            this.blades.forEach {
                val itemEntity = ItemEntity(
                    level!!,
                    worldPosition.x.toDouble() + 0.5,
                    worldPosition.y.toDouble() + 0.5,
                    worldPosition.z.toDouble() + 0.5,
                    it
                )
                itemEntity.deltaMovement.add(0.0, 0.5, 0.0)
                    .scale((level!!.random.nextFloat() * .3f).toDouble())
                level!!.addFreshEntity(itemEntity)

            }
        }
        this.clearContent()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        val bladesTag = tag.getCompound("Blades")
        ContainerHelper.loadAllItems(bladesTag, this.blades)
        this.bladeCooldown = tag.getInt("BladeCooldown")
        this.bladeAngle = tag.getDouble("BladeAngle")

        if (tag.contains("ScrollValue")) {
            bladeAngle = tag.getDouble("ScrollValue")
        }

        if (clientPacket || this.level?.isClientSide == true) {
            this.clientBladeAngle.updateChaseTarget(this.bladeAngle.toFloat())
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        val bladesTag = CompoundTag()
        ContainerHelper.saveAllItems(bladesTag, this.blades)
        tag.put("Blades", bladesTag)
        tag.putInt("BladeCount", this.getBladeCount())
        tag.putInt("BladeCooldown", this.bladeCooldown)
        tag.putDouble("BladeAngle", this.bladeAngle)
        super.write(tag, clientPacket)
    }

    fun insertBlade(blade: ItemStack): Boolean {
        if (this.getBladeCount() < 8) {
            this.setItem(this.getOpenSpace(), blade)
            this.bladeCooldown = 10
            sendData()
            return true
        } else return false
    }

    fun removeBlade(index: Int): ItemStack {
        return blades.removeAt(index)
    }

    fun removeBlade(): ItemStack {
        if (this.isEmpty) return ItemStack.EMPTY
        val blade = removeItem(getHighestTakenSlot(), 1)
        this.bladeCooldown = 10
        sendData()
        return blade
    }

    override fun clearContent() {
        this.blades.clear()
    }

    override fun getContainerSize(): Int {
        return this.blades.size
    }

    fun getOpenSpace(): Int {
        var found = -1
        for (i in this.blades.indices) {
            if (this.blades[i].isEmpty) {
                found = i
                break
            }
        }
        return found
    }

    fun getHighestTakenSlot(): Int {
        var found = -1
        for (i in this.blades.indices) {
            if (!this.blades[i].isEmpty) {
                found = i
            }
        }
        return found
    }

    fun getBladeCount(): Int {
        var found = 0
        for (blade in this.blades) {
            if (!blade.isEmpty) {
                found++
            }
        }
        return found
    }

    override fun isEmpty(): Boolean {
        var found = false
        for (blade in this.blades) {
            if (!blade.isEmpty) {
                found = true
                break
            }
        }
        return !found
    }

    override fun getItem(slot: Int): ItemStack {
        return this.blades[slot]
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return ContainerHelper.removeItem(this.blades, slot, amount)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return ContainerHelper.takeItem(this.blades, slot)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        if (stack.`is`(ClockworkItems.PROPELLER_BLADE.get()) || stack.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())) {
            this.blades[slot] = stack
        }
    }

    override fun stillValid(player: Player): Boolean {
        return level!!.getBlockEntity(this.worldPosition) === this
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        val arr = IntArray(8)
        for (i in arr.indices) {
            arr[i] = i
        }
        return arr
    }

    override fun canPlaceItemThroughFace(index: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        if (itemStack.`is`(ClockworkItems.PROPELLER_BLADE.get()) || itemStack.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())) {
            return true
        }
        return false
    }

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean {
        return true
    }

    override fun sync(storage: NonNullList<ItemStack>) {
        this.blades = storage
    }

    override fun getStorageInventory(): NonNullList<ItemStack> {
        return this.blades
    }

    override fun getStorageInventorySize(): Int {
        return 8
    }

    override fun getBlockPositionFromISS(): BlockPos {
        return this.worldPosition
    }

    class AngleControllerValueBoxTransform: ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3 {
            return VecHelper.voxelSpace(4.0, 8.0, 18.5)
        }

        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return super.getLocalOffset(level, pos, state)
                .add(
                    Vec3.atLowerCornerOf(state.getValue(BlockStateProperties.FACING).normal)
                        .scale((-2 / 16f).toDouble())
                )
        }

        override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
            if (!side.axis.isHorizontal) TransformStack.of(ms)
                .rotateYDegrees((AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.FACING)) + 180))
            super.rotate(level, pos, state, ms)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val facing = state.getValue(BlockStateProperties.FACING)
            return direction == facing
        }
    }

    class LengthControllerValueBoxTransform: ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3 {
            return VecHelper.voxelSpace(12.0, 8.0, 18.5)
        }

        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return super.getLocalOffset(level, pos, state)
                .add(
                    Vec3.atLowerCornerOf(state.getValue(BlockStateProperties.FACING).normal)
                        .scale((-2 / 16f).toDouble())
                )
        }

        override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
            if (!side.axis.isHorizontal) TransformStack.of(ms)
                .rotateYDegrees((AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.FACING)) + 180))
            super.rotate(level, pos, state, ms)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val facing = state.getValue(BlockStateProperties.FACING)
            return direction == facing
        }
    }
}
