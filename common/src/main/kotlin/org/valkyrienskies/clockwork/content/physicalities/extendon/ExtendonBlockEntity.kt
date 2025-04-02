package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.core.apigame.joints.VSD6Joint.D6Axis
import org.valkyrienskies.core.apigame.joints.VSD6Joint.D6Motion
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import java.lang.IllegalStateException
import java.util.EnumMap
import org.valkyrienskies.kelvin.api.DuctNetwork.Companion.idealGasConstant
import org.valkyrienskies.mod.api.dimensionId
import kotlin.math.PI

class ExtendonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState?) : SmartBlockEntity(type, pos, state), IUniversalJoint, IHeatableBlockEntity {

    override var connectedJoint: IUniversalJoint? = null
    override var pos: BlockPos = pos

    var connectedBe: ExtendonBlockEntity? = null
    var edge: DuctEdge? = null

    var distanceJoint: VSDistanceJoint? = null
    var distanceJointId: Int? = null

    var sphericalJoint: VSD6Joint? = null
    var sphericalJointId: Int? = null

    var main: Boolean = false

    override fun tick() {
        super.tick()


        if (level!!.isClientSide) return

        if (connectedBe == null || connectedJoint == null || distanceJoint == null || distanceJointId == null || !main) return


        val kelvin = ClockworkMod.getKelvin()
        val serverLevel = level as ServerLevel

        val distance = gasToDistance(kelvin, getDuctNodePosition(), level.dimensionId!!)


        val tempJoint = VSJointAndId(distanceJointId!!, VSDistanceJoint(distanceJoint!!.shipId0, distanceJoint!!.pose0, distanceJoint!!.shipId1, distanceJoint!!.pose1, minDistance = distance, maxDistance = distance))

        if (distance >= 0.15) serverLevel.shipObjectWorld.updateConstraint(distanceJointId!!, tempJoint.joint)
        distanceJoint = tempJoint.joint as VSDistanceJoint
    }



    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    override fun connectTo(other: IUniversalJoint) {
        if (connectedJoint != null) return
        val be = level?.getBlockEntity(other.pos) as? ExtendonBlockEntity ?: return

        connectedBe = be
        if (connectedBe!!.edge != null) edge = connectedBe!!.edge
        else createEdge(blockPos.toDuctNodePos(level!!.dimension().location()), other.pos.toDuctNodePos(connectedBe!!.level!!.dimension().location()))

        if (connectedBe!!.distanceJoint != null) {
            distanceJoint = connectedBe!!.distanceJoint
            distanceJointId = connectedBe!!.distanceJointId
            sphericalJoint = connectedBe!!.sphericalJoint
            sphericalJointId = connectedBe!!.sphericalJointId
            main = false
        } else createJoint()


        super.connectTo(other)
        sendData()
    }

    override fun disconnect() {
        if (connectedJoint == null) return

        if (connectedBe!!.edge == null) edge = null
        else if (edge != null) removeEdge()

        if (connectedBe!!.distanceJoint == null) {
            distanceJoint = null
            distanceJointId = null
            sphericalJoint = null
            sphericalJointId = null
        } else if (distanceJointId != null) removeJoint()

        connectedBe = null
        main = false


        super.disconnect()
        sendData()
    }

    private fun createEdge(nodeA: DuctNodePos, nodeB: DuctNodePos) {
        val kelvin = ClockworkMod.getKelvin()
        edge = PipeDuctEdge(nodeA = nodeA, nodeB = nodeB, type = ConnectionType.PIPE)
        kelvin.addEdge(nodeA, nodeB, edge!!)
    }

    private fun removeEdge() {
        val kelvin = ClockworkMod.getKelvin()
        kelvin.removeEdge(edge!!.nodeA,edge!!.nodeB)
        edge = null
    }

    private fun createJoint() {
        val serverLevel = level as ServerLevel

        if (connectedBe == null) throw IllegalStateException("Null connected block entity")

        val shipId0 = getShipID()
        val shipId1 = connectedBe!!.getShipID()
        val pos0 = blockPos.toJOMLD()
        val pos1 = connectedBe!!.blockPos.toJOMLD()
        val quater0 = getQuaterniond(level!!.getBlockState(blockPos).getValue(BlockStateProperties.FACING))
        val quater1 =getQuaterniond(level!!.getBlockState(connectedBe!!.blockPos).getValue(BlockStateProperties.FACING))

        distanceJoint = VSDistanceJoint(pose0 = VSJointPose(pos0, quater0), pose1 = VSJointPose(pos1, quater1) , shipId0 = shipId0, shipId1 = shipId1,
            minDistance = 0f, maxDistance = 1000f )
        distanceJointId = serverLevel.shipObjectWorld.createNewConstraint(distanceJoint!!)

        val limit = VSD6Joint.LimitCone(Math.PI.toFloat()/4f, Math.PI.toFloat()/4f)
        val motions = EnumMap<D6Axis, D6Motion>(D6Axis::class.java)

        motions[D6Axis.X] = D6Motion.FREE
        motions[D6Axis.Y] = D6Motion.FREE
        motions[D6Axis.Z] = D6Motion.FREE
        motions[D6Axis.TWIST] = D6Motion.LOCKED
        motions[D6Axis.SWING1] = D6Motion.LIMITED
        motions[D6Axis.SWING2] = D6Motion.LIMITED



        sphericalJoint = VSD6Joint(pose0 = VSJointPose(pos0, quater0), pose1 = VSJointPose(pos1, quater1) , shipId0 = shipId0, shipId1 = shipId1, swingLimit = limit, motions = motions  )
        sphericalJointId = serverLevel.shipObjectWorld.createNewConstraint(sphericalJoint!!)

        main = true
    }

    private fun removeJoint() {
        val serverLevel = level as ServerLevel

        serverLevel.shipObjectWorld.removeConstraint(distanceJointId!!)
        distanceJoint = null
        distanceJointId = null

        sphericalJoint = null
        sphericalJointId = null

        main = false
    }



    fun getShipID(): ShipId {
        val ship = level.getShipManagingPos(blockPos)

        if (ship == null) return -1L
        else return ship.id
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        if (connectedBe != null) {
            compound.putInt("ConnectedPosX",connectedBe!!.pos.x)
            compound.putInt("ConnectedPosY",connectedBe!!.pos.y)
            compound.putInt("ConnectedPosZ",connectedBe!!.pos.z)
            compound.putBoolean("IsMain", main)
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {


        if (compound.contains("ConnectedPosX")) {
            connectedBe = level?.getBlockEntity(BlockPos(compound.getInt("ConnectedPosX"),compound.getInt("ConnectedPosY"),compound.getInt("ConnectedPosZ"))) as? ExtendonBlockEntity
            connectedJoint = connectedBe
            main = compound.getBoolean("IsMain")
        } else disconnect()

        super.read(compound, clientPacket)
    }

    companion object {

        // Calculates volume of cylinder via Ideal Gas Law, and then calculates said cylinder's height
        // Doesn't account for the elastic force of the hose, because doing so would require solving a cubic polynomial
        fun gasToDistance(network: DuctNetwork<*>, pos: DuctNodePos, dimensionId: DimensionId): Float {
            var moles = 0.0
            for (gas in network.getGasMassAt(pos)) moles +=  gas.value / ( gas.key.density * 22.4)

            if (moles < 0.01) return 0f

            val pressure = AerodynamicUtils.getAirPressureForY(pos.y, dimensionId)
            val temperature = network.getTemperatureAt(pos)

            val volume = temperature*idealGasConstant*moles/pressure
            val height = 4 * volume / PI


            return height.toFloat()
        }

        fun getQuaterniond(direction: Direction): Quaterniond {
            return when (direction) {
                Direction.DOWN -> {
                    Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
                }

                Direction.NORTH -> {
                    Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                        Quaterniond(
                            AxisAngle4d(
                                Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                            )
                        )
                    ).normalize()
                }

                Direction.EAST -> {
                    Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                        Quaterniond(
                            AxisAngle4d(
                                Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                            )
                        )
                    ).normalize()
                }

                Direction.SOUTH -> {
                    Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
                }

                Direction.WEST -> {
                    Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                        Quaterniond(
                            AxisAngle4d(
                                Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                            )
                        )
                    ).normalize()
                }

                else -> {
                    // UP or null
                    Quaterniond()
                }
            }
        }
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return blockPos.toDuctNodePos(level!!.dimension().location())
    }
}