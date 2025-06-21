package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.content.fluids.tank.FluidTankBlock
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

class DuctTankBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity,
    IMultiBlockEntityContainer {

    protected var heightCT = 1
    protected var widthCT = 1

    protected var controllerCT: BlockPos? = null
    protected var lastKnownPosCT = blockPos

    var updateConnectivity: Boolean = false

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return

        if (updateConnectivity) updateConnectivity()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        if (tag.contains("Controller")) controller = NbtUtils.readBlockPos(tag.getCompound("Controller"))

        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        if (controller != null) tag.put("Controller", NbtUtils.writeBlockPos(controller))
        super.write(tag, clientPacket)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    fun updateConnectivity() {
        if (level!!.isClientSide) return
        if (!isController) return
        ConnectivityHandler.formMulti(this)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun getController(): BlockPos? {
        return if (isController()) blockPos else controllerCT
    }

    // This is hideously stupid, but intelliJ won't let me do it in a normal way, so... ¯\_(ツ)_/¯
    override fun <T> getControllerBE(): T? where T : BlockEntity?, T : IMultiBlockEntityContainer? {
        if (isController()) return this as T
        return level?.getBlockEntity(controllerCT!!) as? T
    }

    override fun isController(): Boolean {
        return controllerCT == null || controllerCT == blockPos
    }

    override fun setController(pos: BlockPos?) {
        controllerCT = pos
        sendData()
    }

    override fun removeController(keepContents: Boolean) {
        controllerCT = null
        sendData()
    }

    override fun getLastKnownPos(): BlockPos {
        return lastKnownPosCT
    }

    override fun preventConnectivityUpdate() {
        updateConnectivity = false
    }

    override fun notifyMultiUpdated() {
        var state = blockState
        if (blockState.block is DuctTankBlock) { // safety
            state = state.setValue(FluidTankBlock.BOTTOM, controller!!.y == blockPos.y)
            state = state.setValue(FluidTankBlock.TOP, controller!!.y + height - 1 == blockPos.y)
            println("$state ${controller!!.y == blockPos.y} ${controller!!.y + height - 1 == blockPos.y}")
            level!!.setBlock(blockPos, state, 6)
        }
        setChanged()
        sendData()
    }

    override fun getMainConnectionAxis(): Direction.Axis {
        return Direction.Axis.Y
    }

    override fun getMaxLength(longAxis: Direction.Axis?, width: Int): Int {
        return MAX_HEIGHT
    }

    override fun getMaxWidth(): Int {
        return MAX_WIDTH
    }

    override fun getHeight(): Int {
        return heightCT
    }

    override fun setHeight(height: Int) {
        this.heightCT = height
        sendData()
    }

    override fun getWidth(): Int {
        return widthCT
    }

    override fun setWidth(width: Int) {
        this.widthCT = width
        sendData()
    }

    companion object {
        val MAX_WIDTH = 3
        val MAX_HEIGHT = 5
    }
}