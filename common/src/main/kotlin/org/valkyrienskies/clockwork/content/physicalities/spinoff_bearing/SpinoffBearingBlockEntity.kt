package org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.DOWN
import net.minecraft.core.Direction.EAST
import net.minecraft.core.Direction.NORTH
import net.minecraft.core.Direction.SOUTH
import net.minecraft.core.Direction.UP
import net.minecraft.core.Direction.WEST
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.DirectionalBlock.FACING
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips

@OptIn(PhysTickOnly::class, GameTickOnly::class, VsBeta::class)
class SpinoffBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
), BlockEntityPhysicsListener {

    var partner : SpinoffBearingBlockEntity? = null
    @Volatile
    var partnerPos : BlockPos? = null
    @Volatile
    var partnerFacing : Direction? = null
    @Volatile
    var partnerShipId : ShipId? = null
    @Volatile
    var isLeader: Boolean = false

    @Volatile
    var isConnected : Boolean = false
    @Volatile
    var jointId : VSJointId = -1

    val position : BlockPos
        get() = this.worldPosition
    val facing: Direction
        get() = blockState.getValue(BlockStateProperties.FACING)

    override var dimension: DimensionId
        get() = level!!.dimensionId
        set(value) { }

    @Volatile
    var shouldVerifyConnection: Boolean = false
    @Volatile
    var shouldRemoveJoint: Boolean = false
    var shouldVerifyPartner: Boolean = false

    var reconnectDelay: Int = 0
    var canConnect : Boolean = true

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (clientPacket) {
            super.write(tag, clientPacket)
        }
        if (partnerPos != null) {
            tag.putInt("partnerX", partnerPos!!.x)
            tag.putInt("partnerY", partnerPos!!.y)
            tag.putInt("partnerZ", partnerPos!!.z)
        }
        tag.putInt("jointId", jointId)
        tag.putBoolean("isLeader", isLeader)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("partnerX") && tag.contains("partnerY") && tag.contains("partnerZ")) {
            val x = tag.getInt("partnerX")
            val y = tag.getInt("partnerY")
            val z = tag.getInt("partnerZ")
            partnerPos = BlockPos(x, y, z)
        }
        if (tag.contains("isLeader")) {
            isLeader = tag.getBoolean("isLeader")
        }
        jointId = tag.getInt("jointId")
        shouldVerifyPartner = true
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return
        val sLevel = level as ServerLevel
        if (shouldVerifyPartner) {
            partner = null
            partnerPos?.let { pPos ->
                level?.getBlockEntity(pPos)?.let { be ->
                    if (be is SpinoffBearingBlockEntity) {
                        partner = be
                        partnerShipId = level.getLoadedShipManagingPos(pPos)?.id
                        partnerFacing = partner!!.facing
                        if (isLeader) {
                            shouldVerifyConnection = true
                        }
                    }
                }
            }
            if (partner == null) {
                partnerPos = null
                partnerShipId = null
            }
            shouldVerifyPartner = false
        }
        val selfIsPowered = sLevel.hasNeighborSignal(position)
        canConnect = (reconnectDelay <= 0) && !selfIsPowered
        if (partner != null && !canConnect) {
            partner?.disconnect()
            disconnect()
        }
        if (partner == null && canConnect) {
            //search for a partner

            val clip = ClipContext(
                sLevel.toWorldCoordinates(Vec3.atCenterOf(position)),
                sLevel.toWorldCoordinates(Vec3.atCenterOf(position.relative(facing, 2))),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )

            val hitResult = sLevel.clipIncludeShips(clip, skipShip = level!!.getLoadedShipManagingPos(position)?.id)
            if (hitResult.type == HitResult.Type.BLOCK) {
                //println("hit!")
                //println(hitResult.blockPos)
                val hitPos = hitResult.blockPos
                sLevel.getBlockEntity(hitPos)?.let { be ->
                    if (be is SpinoffBearingBlockEntity && (be.partner == null || be.partner == this) && be.canConnect) {
                        //check the two bearings are facing eachother mostly
                        val selfShip = sLevel.getLoadedShipManagingPos(this.position)
                        val partnerShip = sLevel.getLoadedShipManagingPos(hitPos)

                        if (selfShip == null && partnerShip == null) return@let
                        if (selfShip?.id == partnerShip?.id) return@let

                        val selfWorldFacing = facing.normal.toJOMLD().toWorldFacing(
                            selfShip
                        )
                        val partnerWorldFacing = be.facing.normal.toJOMLD().toWorldFacing(
                            partnerShip
                        )
                        val dot = selfWorldFacing.dot(partnerWorldFacing)
                        if (dot > -0.75) return@let //not facing eachother enough

                        partner = be
                        partnerPos = hitPos
                        partnerShipId = level.getLoadedShipManagingPos(hitPos)?.id
                        partnerFacing = partner!!.facing
                        isLeader = true
                        partner!!.partner = this
                        partner!!.partnerPos = position
                        partner!!.partnerShipId = level.getLoadedShipManagingPos(position)?.id
                        partner!!.partnerFacing = this.facing
                        partner!!.isLeader = false
                        if (isLeader) {
                            shouldVerifyConnection = true
                            sLevel.playSound(
                                null, position, ClockworkSounds.GEAR_WHIRR.mainEvent!!, SoundSource.BLOCKS,
                                0.5f, 1.0f
                            )
                        }
                    }
                }
            }
        }
        if (reconnectDelay > 0) {
            reconnectDelay--
        }
    }

    override fun physTick(
        physShip: PhysShip?,
        physLevel: PhysLevel
    ) {
        if (shouldVerifyConnection) {
            val vsiPhysLevel = physLevel as VsiPhysLevel
            vsiPhysLevel.getJointById(jointId)?.let {
                isConnected = true
                shouldVerifyConnection = false
            } ?: run {
                isConnected = false
                jointId = -1
            }
            if (!isConnected) {
                if (partnerShipId == physShip?.id) return
                if (partnerFacing == null || partnerPos == null) return
                //create joint between the two bearings
                val selfRotation = facing.getQuaternion()
                val partnerRotation = partnerFacing?.opposite!!.getQuaternion()
                val hingeOrientation = selfRotation.mul(Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)), Quaterniond()).normalize()
                val partnerHingeOrientation = partnerRotation.mul(Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)), Quaterniond()).normalize()
                val attachmentOffset0: Vector3dc = selfRotation.transform(Vector3d(0.0, 0.5, 0.0))
                val attachmentOffset1: Vector3dc = partnerRotation.transform(Vector3d(0.0, 0.5, 0.0))
                val selfPose = VSJointPose(
                    this.position.toJOMLD().add(0.5, 0.5, 0.5).add(attachmentOffset0),
                    hingeOrientation
                )
                val partnerPose = VSJointPose(
                    partnerPos!!.toJOMLD().add(0.5, 0.5, 0.5).add(attachmentOffset1),
                    partnerHingeOrientation
                )
                // we need to have the one that isn't on a ship be the first one for the calcs
                val revoluteJoint = if (physShip == null) {
                    VSRevoluteJoint(
                        null,
                        selfPose,
                        partnerShipId,
                        partnerPose,
                        driveFreeSpin = true
                    )
                } else {
                    VSRevoluteJoint(
                        partnerShipId,
                        partnerPose,
                        physShip.id,
                        selfPose,
                        driveFreeSpin = true
                    )
                }
                jointId = vsiPhysLevel.addJoint(revoluteJoint)
                shouldVerifyConnection = false
            }
        }
        if (shouldRemoveJoint) {
            val vsiPhysLevel = physLevel as VsiPhysLevel
            vsiPhysLevel.removeJoint(jointId)
            isConnected = false
            jointId = -1
            shouldRemoveJoint = false
        }
    }

    fun disconnect() {
        this.shouldRemoveJoint = true
        partner = null
        partnerPos = null
        partnerShipId = null
        partnerFacing = null
        reconnectDelay = 10
    }

    override fun destroy() {
        if (level is ServerLevel) {
            val sLevel = (level as ServerLevel)
            sLevel.gtpa.removeJoint(jointId)
            //println("tried to use GTPA to remove joint")
        }
        partner?.disconnect()
        this.disconnect()
        super.destroy()
    }

    private fun Vector3dc.toWorldFacing(ship: LoadedServerShip?): Vector3dc {
        if (ship == null) return this
        return ship.kinematics.rotation.transform(this, Vector3d())
    }

    private fun Quaterniondc.toWorldFacing(ship: PhysShip?): Quaterniond {
        return if (ship == null) {
            Quaterniond(this)
        } else {
            val shipRot = ship.kinematics.rotation
            Quaterniond(shipRot).mul(this)
        }
    }

    private fun Direction.getQuaternion(): Quaterniondc {
        return when (this) {
            DOWN -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            }
            NORTH -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            EAST -> {
                Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            SOUTH -> {
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            }
            WEST -> {
                Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            UP -> {
                // Do nothing
                Quaterniond()
            }
            else -> {
                // This should be impossible, but have this here just in case
                Quaterniond()
            }
        }
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) {
    }
}
