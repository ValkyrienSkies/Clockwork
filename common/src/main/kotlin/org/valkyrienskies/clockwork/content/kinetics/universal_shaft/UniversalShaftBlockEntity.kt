package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.mod.common.toWorldCoordinates

class UniversalShaftBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), IUniversalJoint {
    override var connectedJoint: IUniversalJoint? = null
    override var pos = blockPos
    var connectedPos: BlockPos? = null
    var connectedBe: UniversalShaftBlockEntity? = null

    var main: Boolean = false

    override val maxCreationDistance: Double
        get() = ClockworkConfig.SERVER.maxUniversalJointDistance

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
    }

    // Custom Propagation
    override fun propagateRotationTo(
        target: KineticBlockEntity, stateFrom: BlockState, stateTo: BlockState, diff: BlockPos,
        connectedViaAxes: Boolean, connectedViaCogs: Boolean
    ): Float {

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
        if (connectedBe != null) {
            connectedBe!!.connectedBe = null
            connectedBe!!.connectedPos = null
            connectedBe!!.connectedJoint = null
            connectedBe!!.main = false
            connectedBe!!.detachKinetics()
            connectedBe!!.clearKineticInformation()
            connectedBe!!.sendData()
        }
        connectedPos = null
        connectedBe = null
        connectedJoint = null
        main = false
        detachKinetics()
        clearKineticInformation()
        sendData()
    }

    override fun connectTo(other: IUniversalJoint) {

        connectedPos = other.pos
        connectedJoint = other
        main = true

        super.connectTo(other)

        sendData()
        attachKinetics()
        if (other is UniversalShaftBlockEntity) {
            other.connectedBe = this
            other.connectedPos = this.pos
            other.connectedJoint = this
            other.main = false
            other.attachKinetics()
            other.sendData()
        }
    }

    override fun tick() {
        super.tick()

        if ((connectedJoint == null || connectedBe == null) && connectedPos != null) {
            val be = level!!.getBlockEntity(connectedPos!!)
            if (be != null && be !is UniversalShaftBlockEntity) connectedPos = null
            else if(be != null) {
                connectedJoint = be
                connectedBe = be
            }
        }
        if (level == null || level!!.isClientSide) return
        if (connectedPos != null && main) {
            val posInWorld = level.toWorldCoordinates(pos)
            val otherPosInWorld = level.toWorldCoordinates(connectedPos!!)
            val distance = posInWorld.distanceTo(otherPosInWorld)
            if (distance > maxCreationDistance && connectedBe != null) {
                connectedBe!!.disconnect()
                disconnect()
                (level as ServerLevel).playSound(null, pos, ClockworkSounds.JOINT_BREAK.mainEvent, SoundSource.BLOCKS, 1.0f, 1.0f)
            }
        }

    }

    override fun isThisJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }

    override fun getConnectionItem(): ItemStack {
        return ClockworkItems.UNIVERSAL_SHAFT_ITEM.asStack()
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        if (connectedPos != null) {
            compound.putInt("otherPosX",connectedPos!!.x)
            compound.putInt("otherPosY",connectedPos!!.y)
            compound.putInt("otherPosZ",connectedPos!!.z)
            compound.putBoolean("main", main)
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("otherPosX")) {
            connectedPos = BlockPos(compound.getInt("otherPosX"),compound.getInt("otherPosY"),compound.getInt("otherPosZ"))
            main = compound.getBoolean("main")
        } else {
            connectedPos = null
        }

        super.read(compound, clientPacket)
    }

    override fun remove() {
        disconnect()
        super.remove()
    }
}
