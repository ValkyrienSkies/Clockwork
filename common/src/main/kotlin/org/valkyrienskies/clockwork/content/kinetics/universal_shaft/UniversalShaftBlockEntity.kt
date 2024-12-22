package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint

class UniversalShaftBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IUniversalJoint {
    override var connectedJoint: IUniversalJoint? = null
    override var pos = blockPos
    var connectedBe: UniversalShaftBlockEntity? = null


    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        println("ns ${getSpeed()}")
    }

    // Custom Propagation
    override fun propagateRotationTo(
        target: KineticBlockEntity, stateFrom: BlockState, stateTo: BlockState, diff: BlockPos,
        connectedViaAxes: Boolean, connectedViaCogs: Boolean
    ): Float {
        println("${target.blockPos} ${connectedBe?.blockPos}")
        if (connectedBe == null || target.blockPos != connectedBe!!.blockPos) return 0f
        return 1f
    }

    override fun addPropagationLocations(
        block: IRotate,
        state: BlockState,
        neighbours: MutableList<BlockPos>
    ): List<BlockPos> {
        if (connectedBe != null) neighbours.add(connectedBe!!.blockPos)
        return neighbours
    }

    override fun isCustomConnection(other: KineticBlockEntity?, state: BlockState?, otherState: BlockState?): Boolean {
        return true
    }

    override fun disconnect() {
        super.disconnect()
        detachKinetics()
    }

    override fun connectTo(other: IUniversalJoint) {
        val be = level!!.getBlockEntity(other.pos) as? UniversalShaftBlockEntity ?: return
        super.connectTo(other)
        connectedBe = be
        println("connected ${level!!.isClientSide}")
        attachKinetics()
    }

    override fun tick() {
        super.tick()

        println("tick ${level!!.isClientSide} ${connectedBe}")
    }

    override fun isThisJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }

}