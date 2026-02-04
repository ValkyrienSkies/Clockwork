package org.valkyrienskies.clockwork.content.logistics.gas.docking_vent;

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
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkSounds

import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d
import kotlin.math.PI

class DockingVentBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState)
    : KNodeBlockEntity(type, pos, state), BlockEntityPhysicsListener {

    var partner : DockingVentBlockEntity? = null
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

    var kelvinConnection: DuctEdge? = null

    val position : BlockPos
        get() = this.worldPosition
    val facing: Direction
        get() = DockingVentBlock.getFacing(blockState)

    @Volatile
    var shouldVerifyConnection: Boolean = false
    @Volatile
    var shouldRemoveJoint: Boolean = false
    var shouldVerifyPartner: Boolean = false

    var reconnectDelay: Byte = 0
    var snapDelay: Byte = 0
    var canConnect : Boolean = true

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) {}

    override lateinit var dimension: DimensionId


    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (clientPacket) { super.write(tag, clientPacket) }
        partnerPos?.runCatching { tag.putVector3d("partner_", partnerPos!!.toVector3d()) }?.onFailure {
            ClockworkMod.LOGGER.warn("Oops! Value was nulled by other thread. Ignoring")
        }
        tag.putInt("jointId", jointId)
        tag.putBoolean("isLeader", isLeader)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        tag.getVector3d("partner_")?.let {
            partnerPos = it.toMinecraft()
        }
        isLeader = tag.getBoolean("isLeader")
        jointId = tag.getInt("jointId")
        shouldVerifyPartner = true
    }

    override fun destroy() {
        (level as? ServerLevel)?.gtpa?.removeJoint(jointId)
        partner?.disconnect()
        this.disconnect()
        super.destroy()
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return
        if (shouldVerifyPartner) verifyPartner()

        val sLevel = level as ServerLevel
        canConnect = (reconnectDelay <= 0) && !sLevel.hasNeighborSignal(position)

        if (partner != null) {
            if (!canConnect) {
                partner?.disconnect()
                disconnect()
            } else if (isOverstressed()) {
                partner?.disconnect(isDestructive = true)
                disconnect(isDestructive = true)
            }
        }
        if (partner == null && canConnect) tryConnect(sLevel)

        if (reconnectDelay > 0) reconnectDelay--
        if (snapDelay > 0) snapDelay--
    }

    private fun verifyPartner() {
        partner = null
        partnerPos
            ?.let { level?.getBlockEntity(it, ) as? DockingVentBlockEntity }
            ?.let {
                partner = it
                partnerShipId = level.getLoadedShipManagingPos(it.position)?.id
                partnerFacing = partner!!.facing
                shouldVerifyConnection = isLeader
            }
        if (partner == null) {
            partnerPos = null
            partnerShipId = null
        }
        shouldVerifyPartner = false
    }

    @OptIn(GameTickOnly::class)
    private fun tryConnect(sLevel: ServerLevel) {
        ClipContext(
            sLevel.toWorldCoordinates(Vec3.atCenterOf(position)),
            sLevel.toWorldCoordinates(Vec3.atCenterOf(position.relative(facing, 1))),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            null
        ).let {sLevel.clipIncludeShips(it, skipShip = level!!.getLoadedShipManagingPos(position)?.id)}
            .takeIf { it.type == HitResult.Type.BLOCK }?.blockPos
            ?.let { sLevel.getBlockEntity(it) as? DockingVentBlockEntity }
            ?.takeIf { it.partner == null && it.canConnect }
            ?.let { be ->
                val selfShip = sLevel.getLoadedShipManagingPos(this.position)
                val partnerShip = sLevel.getLoadedShipManagingPos(be.position)

                if (selfShip == null && partnerShip == null) return@let
                if (selfShip?.id == partnerShip?.id) return@let

                val selfWorldFacing = facing.normal.toJOMLD().toWorldFacing(selfShip)
                val partnerWorldFacing = be.facing.normal.toJOMLD().toWorldFacing(partnerShip)

                if (selfWorldFacing.angle(partnerWorldFacing) < PI*5/6) return@let

                partner = be
                partnerPos = be.position
                partnerShipId = partnerShip?.id
                partnerFacing = partner!!.facing
                isLeader = true
                partner!!.partner = this
                partner!!.partnerPos = position
                partner!!.partnerShipId = level.getLoadedShipManagingPos(position)?.id
                partner!!.partnerFacing = this.facing
                partner!!.isLeader = false
                shouldVerifyConnection = true

                kelvinConnection = be.kelvinConnection ?:
                        createKelvinConnection(
                            self = this.position.toDuctNodePos(level!!.dimension().location()),
                            other = be.position.toDuctNodePos(be.level!!.dimension().location())
                        )

                sLevel.playSound(
                    null, position, ClockworkSounds.GEAR_WHIRR.mainEvent!!, SoundSource.BLOCKS,
                    0.5f, 1.0f
                )
                snapDelay = 10
            }
    }

    @OptIn(GameTickOnly::class)
    private fun isOverstressed(): Boolean {

        if(snapDelay > 0) return false

        val sLevel = level as ServerLevel

        val selfShip = sLevel.getLoadedShipManagingPos(this.position)
        val partnerShip = sLevel.getLoadedShipManagingPos(partnerPos!!.toVector3d())

        val selfWorldPosition = position.toJOMLD().toWorldPosition(selfShip)
        val partnerWorldPosition = partnerPos!!.toJOMLD().toWorldPosition(partnerShip)

        if ((selfWorldPosition - partnerWorldPosition).length() > 2) return true

        val selfWorldFacing = facing.normal.toJOMLD().toWorldFacing(selfShip)
        val partnerWorldFacing = partner!!.facing.normal.toJOMLD().toWorldFacing(partnerShip)

        if (selfWorldFacing.angle(partnerWorldFacing) < PI*4/6) return true

        return false
    }



    fun disconnect(isDestructive: Boolean = false) {
        this.shouldRemoveJoint = true
        partner = null
        partnerPos = null
        partnerShipId = null
        partnerFacing = null
        reconnectDelay = 10

        kelvinConnection?.let {
            ClockworkMod.getKelvin().removeEdge(it.nodeA, it.nodeB)
        }
        kelvinConnection = null

        if (isDestructive) {
            (level as? ServerLevel)?.let {
                it.playSound(
                    null, position, ClockworkSounds.GEAR_WHIRR.mainEvent!!, SoundSource.BLOCKS,
                    0.5f, 1.0f
                )
                it.destroyBlock(position, true)
            }

        }
    }

    @OptIn(PhysTickOnly::class)
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
                        driveFreeSpin = true,
                        compliance = 1e-5
                    )
                } else {
                    VSRevoluteJoint(
                        partnerShipId,
                        partnerPose,
                        physShip.id,
                        selfPose,
                        driveFreeSpin = true,
                        compliance = 1e-5
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

    companion object {

        @JvmStatic
        private fun Direction.getQuaternion(): Quaterniondc =
            when (this) {
                DOWN -> Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
                NORTH -> Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0)))
                    .mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
                EAST -> Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0)))
                    .mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
                SOUTH -> Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
                WEST -> Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0)))
                    .mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
                UP -> Quaterniond()
            }

        @JvmStatic
        @GameTickOnly
        private fun Vector3dc.toWorldFacing(ship: LoadedServerShip?): Vector3dc =
            ship?.transform?.rotation?.transform(this, Vector3d()) ?: this

        @JvmStatic
        @GameTickOnly
        private fun Vector3dc.toWorldPosition(ship: LoadedServerShip?): Vector3dc =
            ship?.transform?.toWorld?.transformPosition(this, Vector3d()) ?: this

        @JvmStatic
        fun createKelvinConnection( self: DuctNodePos, other: DuctNodePos): DuctEdge =
            PipeDuctEdge(nodeA = self, nodeB = other, type = ConnectionType.PIPE).apply {
                ClockworkMod.getKelvin().addEdge(self, other, this)
            }

    }

}
