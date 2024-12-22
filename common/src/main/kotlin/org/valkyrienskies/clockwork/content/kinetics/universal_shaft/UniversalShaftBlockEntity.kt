package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint

class UniversalShaftBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IUniversalJoint {
    override var connectedJoint: IUniversalJoint? = null
    override var pos = blockPos
    var connectedPos: BlockPos? = null


    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        println("ns ${getSpeed()}")
    }

    // Custom Propagation
    override fun propagateRotationTo(
        target: KineticBlockEntity, stateFrom: BlockState, stateTo: BlockState, diff: BlockPos,
        connectedViaAxes: Boolean, connectedViaCogs: Boolean
    ): Float {
        println("${target.blockPos} $connectedPos")
        if (connectedJoint == null || target.blockPos != connectedPos) return 0f
        return 1f
    }

    override fun addPropagationLocations(
        block: IRotate,
        state: BlockState,
        neighbours: MutableList<BlockPos>
    ): List<BlockPos> {
        if (connectedJoint != null) neighbours.add(connectedPos!!)
        return neighbours
    }

    override fun isCustomConnection(other: KineticBlockEntity?, state: BlockState?, otherState: BlockState?): Boolean {
        return true
    }

    override fun disconnect() {
        super.disconnect()
        detachKinetics()
        sendData()
    }

    override fun connectTo(other: IUniversalJoint) {

        connectedPos = other.pos

        super.connectTo(other)

        sendData()
        attachKinetics()
    }

    override fun tick() {
        super.tick()

        if (connectedJoint == null && connectedPos != null) {
            val be = level!!.getBlockEntity(connectedPos!!)
            if (be != null && be !is UniversalShaftBlockEntity) connectedPos = null
            else if(be != null) connectedJoint = be as UniversalShaftBlockEntity
        }

    }

    override fun isThisJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        if (connectedJoint != null) {
            compound.putInt("otherPosX",connectedPos!!.x)
            compound.putInt("otherPosY",connectedPos!!.y)
            compound.putInt("otherPosZ",connectedPos!!.z)
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("otherPosX")) {
            connectedPos = BlockPos(compound.getInt("otherPosX"),compound.getInt("otherPosY"),compound.getInt("otherPosZ"))

        }

        super.read(compound, clientPacket)
    }

    override fun remove() {
        disconnect()
        super.remove()
    }
}