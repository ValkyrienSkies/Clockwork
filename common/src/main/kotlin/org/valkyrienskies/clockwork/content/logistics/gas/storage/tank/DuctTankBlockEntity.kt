package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.content.fluids.tank.FluidTankBlock
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import io.github.fabricators_of_create.porting_lib.util.Constants.BlockFlags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

class DuctTankBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity,
    IMultiBlockEntityContainer {

    protected val maxHeight = 5

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

    fun queueConnectivityUpdate() {
        updateConnectivity = true
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        if (tag.contains("Controller")) controllerCT = NbtUtils.readBlockPos(tag.getCompound("Controller"))
        if (tag.contains("Height")) heightCT = tag.getInt("Height")
        if (tag.contains("Width")) widthCT = tag.getInt("Width")
        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        tag.putInt("Height", height)
        tag.putInt("Width", width)
        if (controller != null) tag.put("Controller", NbtUtils.writeBlockPos(controller!!))
        super.write(tag, clientPacket)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    fun updateConnectivity() {
        if (level!!.isClientSide) return
        if (!isController) return

        updateConnectivity = false
        ConnectivityHandler.formMulti(this)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return controller!!.toDuctNodePos(level!!.dimension().location())
    }

    override fun getController(): BlockPos? {
        return if (isController) blockPos else controllerCT
    }

    // This is hideously stupid, but intelliJ won't let me do it in a normal way, so...
    override fun <T> getControllerBE(): T? where T : BlockEntity?, T : IMultiBlockEntityContainer? {
        if (isController) return this as T
        return level?.getBlockEntity(controllerCT!!) as? T
    }

    override fun isController(): Boolean {
        return controllerCT == null || controllerCT == blockPos
    }

    override fun setController(pos: BlockPos?) {
        controllerCT = pos
        notifyUpdate()


    }

    override fun removeController(keepContents: Boolean) {
        controllerCT = null
        heightCT = 1
        widthCT = 1
        queueConnectivityUpdate()
        notifyMultiUpdated()

        if (isController) {
            (blockState.block as? DuctTankBlock)?.nodeRemove(blockState, level!!, blockPos, blockState, false)
        }
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
            state = state.setValue(DuctTankBlock.BOTTOM, controller!!.y == blockPos.y)
            state = state.setValue(DuctTankBlock.TOP, controller!!.y + height - 1 == blockPos.y)
            level!!.setBlock(blockPos, state,23)

            if (isController) {
                (blockState.block as? DuctTankBlock)?.nodeRemove(blockState, level!!, blockPos, blockState, false)
                (blockState.block as? DuctTankBlock)?.nodePlace(blockState, level!!, blockPos, blockState, false)
            }


        }
        notifyUpdate()
    }


    override fun getMainConnectionAxis(): Direction.Axis { return Direction.Axis.Y }

    override fun getMaxLength(longAxis: Direction.Axis, width: Int): Int {
        return if (longAxis === Direction.Axis.Y) maxHeight else maxWidth
    }

    override fun getMaxWidth(): Int {
        return 3
    }

    override fun getHeight(): Int {
        return heightCT
    }

    override fun setHeight(height: Int) {
        this.heightCT = height
    }

    override fun getWidth(): Int {
        return widthCT
    }

    override fun setWidth(width: Int) {
        this.widthCT = width
    }

}