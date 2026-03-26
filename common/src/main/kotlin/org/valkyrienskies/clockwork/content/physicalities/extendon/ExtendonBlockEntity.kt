package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.platform.Platform
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.util.findMatchingJoint
import org.valkyrienskies.clockwork.util.findMatchingJointIds
import org.valkyrienskies.clockwork.util.hasFinitePoseData
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.removeMatchingJointsExcept
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Axis
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Motion
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.EnumMap
import org.valkyrienskies.kelvin.api.DuctNetwork.Companion.idealGasConstant
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class ExtendonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), IUniversalJoint {

    override var connectedJoint: IUniversalJoint? = null
    override var pos: BlockPos = pos

    var connectedBe: ExtendonBlockEntity? = null
    var edge: DuctEdge? = null

    var distanceJoint: VSDistanceJoint? = null
    var distanceJointId: Int? = null

    var sphericalJoint: VSD6Joint? = null
    var sphericalJointId: Int? = null

    var main: Boolean = false

    var loadFn: (() -> Boolean)? = null
    private var pendingJointCreationToken: Long = 0L
    private var deferredRestoreTries: Int = 0

    private fun invalidatePendingJointCreation() {
        pendingJointCreationToken++
    }

    private fun clearJointStateOnly() {
        distanceJoint = null
        distanceJointId = null
        sphericalJoint = null
        sphericalJointId = null
        main = false
    }

    private fun hasTrackedJointIds(): Boolean = distanceJointId != null || sphericalJointId != null

    private fun hasLoadedJointState(): Boolean =
        distanceJoint != null &&
            distanceJointId != null &&
            sphericalJoint != null &&
            sphericalJointId != null

    private fun tryAdoptTrackedJoints(serverLevel: ServerLevel): Boolean? {
        val trackedDistanceJointId = distanceJointId
        val trackedSphericalJointId = sphericalJointId
        if (trackedDistanceJointId == null && trackedSphericalJointId == null) {
            return false
        }
        if (trackedDistanceJointId == null || trackedSphericalJointId == null) {
            ClockworkMod.LOGGER.warn(
                "Discarding partial restored extendon joint state at {} (distanceJointId={}, sphericalJointId={}).",
                blockPos,
                trackedDistanceJointId,
                trackedSphericalJointId
            )
            removeJoint()
            return false
        }

        val existingDistanceJoint = serverLevel.gtpa.getJointById(trackedDistanceJointId) ?: return null
        val existingSphericalJoint = serverLevel.gtpa.getJointById(trackedSphericalJointId) ?: return null

        if (
            existingDistanceJoint !is VSDistanceJoint ||
            !existingDistanceJoint.hasFinitePoseData() ||
            existingSphericalJoint !is VSD6Joint ||
            !existingSphericalJoint.hasFinitePoseData()
        ) {
            ClockworkMod.LOGGER.warn(
                "Discarding invalid restored extendon joints at {} (distanceJointId={}, sphericalJointId={}).",
                blockPos,
                trackedDistanceJointId,
                trackedSphericalJointId
            )
            removeJoint()
            return false
        }

        distanceJoint = existingDistanceJoint
        sphericalJoint = existingSphericalJoint
        serverLevel.gtpa.removeMatchingJointsExcept(existingDistanceJoint, trackedDistanceJointId)
        serverLevel.gtpa.removeMatchingJointsExcept(existingSphericalJoint, trackedSphericalJointId)
        return true
    }

    private fun buildJointBlueprints(other: ExtendonBlockEntity): Pair<VSDistanceJoint, VSD6Joint>? {
        val level = level as? ServerLevel ?: return null

        val shipId0 = getShipID()
        val shipId1 = other.getShipID()
        val pos0 = blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val pos1 = other.blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val quater0 = getQuaterniond(level.getBlockState(blockPos).getValue(BlockStateProperties.FACING))
        val quater1 = getQuaterniond(level.getBlockState(other.blockPos).getValue(BlockStateProperties.FACING))

        val distanceJoint = VSDistanceJoint(
            pose0 = VSJointPose(pos0, quater0),
            pose1 = VSJointPose(pos1, quater1),
            shipId0 = shipId0,
            shipId1 = shipId1,
            minDistance = 0.5f,
            maxDistance = 1000f,
            damping = 1000f
        )

        val limit = VSD6Joint.LimitCone(Math.PI.toFloat() / 4f, Math.PI.toFloat() / 4f)
        val motions = EnumMap<D6Axis, D6Motion>(D6Axis::class.java)
        motions[D6Axis.X] = D6Motion.FREE
        motions[D6Axis.Y] = D6Motion.FREE
        motions[D6Axis.Z] = D6Motion.FREE
        motions[D6Axis.TWIST] = D6Motion.LOCKED
        motions[D6Axis.SWING1] = D6Motion.LIMITED
        motions[D6Axis.SWING2] = D6Motion.LIMITED

        val sphericalJoint = VSD6Joint(
            pose0 = VSJointPose(pos0, quater0),
            pose1 = VSJointPose(pos1, quater1),
            shipId0 = shipId0,
            shipId1 = shipId1,
            swingLimit = limit,
            motions = motions,
        )

        return distanceJoint to sphericalJoint
    }

    private fun tryAdoptMatchingJoints(serverLevel: ServerLevel, other: ExtendonBlockEntity): Boolean? {
        val (expectedDistanceJoint, expectedSphericalJoint) = buildJointBlueprints(other) ?: return false

        val matchingDistance = serverLevel.gtpa.findMatchingJoint(expectedDistanceJoint)
        val matchingSpherical = serverLevel.gtpa.findMatchingJoint(expectedSphericalJoint)

        if (matchingDistance == null && matchingSpherical == null) {
            return false
        }
        if (matchingDistance == null || matchingSpherical == null) {
            return null
        }

        val adoptedDistanceJoint = matchingDistance.joint as? VSDistanceJoint
        val adoptedSphericalJoint = matchingSpherical.joint as? VSD6Joint
        if (
            adoptedDistanceJoint == null ||
            !adoptedDistanceJoint.hasFinitePoseData() ||
            adoptedSphericalJoint == null ||
            !adoptedSphericalJoint.hasFinitePoseData()
        ) {
            ClockworkMod.LOGGER.warn(
                "Discarding invalid structurally matched extendon joints at {} (distanceJointId={}, sphericalJointId={}).",
                blockPos,
                matchingDistance.jointId,
                matchingSpherical.jointId
            )
            serverLevel.gtpa.removeJoint(matchingDistance.jointId)
            serverLevel.gtpa.removeJoint(matchingSpherical.jointId)
            return false
        }

        distanceJoint = adoptedDistanceJoint
        distanceJointId = matchingDistance.jointId
        sphericalJoint = adoptedSphericalJoint
        sphericalJointId = matchingSpherical.jointId
        serverLevel.gtpa.removeMatchingJointsExcept(adoptedDistanceJoint, matchingDistance.jointId)
        serverLevel.gtpa.removeMatchingJointsExcept(adoptedSphericalJoint, matchingSpherical.jointId)
        return true
    }

    private fun restoreConnection(other: ExtendonBlockEntity): Boolean {
        val serverLevel = level as? ServerLevel ?: return false

        if (connectedJoint != null && connectedJoint !== other) {
            disconnect()
        }
        if (other.connectedJoint != null && other.connectedJoint !== this) {
            other.disconnect()
        }

        val candidates = mutableListOf<ExtendonBlockEntity>()
        if (main && hasTrackedJointIds()) {
            candidates.add(this)
        }
        if (other.main && other.hasTrackedJointIds() && !candidates.contains(other)) {
            candidates.add(other)
        }
        if (hasTrackedJointIds() && !candidates.contains(this)) {
            candidates.add(this)
        }
        if (other.hasTrackedJointIds() && !candidates.contains(other)) {
            candidates.add(other)
        }

        var waitingOnRestoredJoint = false
        for (candidate in candidates) {
            when (candidate.tryAdoptTrackedJoints(serverLevel)) {
                true -> {
                    deferredRestoreTries = 0
                    if (candidate === this) {
                        main = true
                        other.main = false
                        connectTo(other)
                    } else {
                        other.main = true
                        main = false
                        other.connectTo(this)
                    }
                    return true
                }

                null -> waitingOnRestoredJoint = true
                false -> Unit
            }
        }

        when (tryAdoptMatchingJoints(serverLevel, other)) {
            true -> {
                deferredRestoreTries = 0
                main = true
                other.main = false
                connectTo(other)
                return true
            }

            null -> waitingOnRestoredJoint = true
            false -> Unit
        }

        when (other.tryAdoptMatchingJoints(serverLevel, this)) {
            true -> {
                deferredRestoreTries = 0
                other.main = true
                main = false
                other.connectTo(this)
                return true
            }

            null -> waitingOnRestoredJoint = true
            false -> Unit
        }

        if (waitingOnRestoredJoint) {
            deferredRestoreTries++
            if (deferredRestoreTries < MAX_RESTORE_TRIES) {
                if (deferredRestoreTries % 20 == 0) {
                    ClockworkMod.LOGGER.warn(
                        "Deferring extendon joint restore at {} while waiting for tracked joints to load (attempt {}).",
                        blockPos,
                        deferredRestoreTries
                    )
                }
                return false
            }

            ClockworkMod.LOGGER.warn(
                "Discarding stale extendon joint references at {} after {} restore attempts.",
                blockPos,
                deferredRestoreTries
            )
            if (hasTrackedJointIds()) {
                removeJoint()
            }
            if (other.hasTrackedJointIds()) {
                other.removeJoint()
            }
        }

        deferredRestoreTries = 0
        connectTo(other)
        return true
    }

    override fun tick() {
        super.tick()


        if (level!!.isClientSide) return

        loadFn?.also {
            if (it.invoke()) {
                loadFn = null
            }
        }

        if (
            connectedBe == null ||
            connectedJoint == null ||
            distanceJoint == null ||
            distanceJointId == null ||
            sphericalJoint == null ||
            sphericalJointId == null ||
            !main
        ) return


        val kelvin = ClockworkMod.getKelvin()
        val serverLevel = level as ServerLevel

        val previousDistance = distanceJoint!!.minDistance!!

        val distance = max(1.5f,(gasToDistance(kelvin, getDuctNodePosition(), level!!.dimensionId) + gasToDistance(kelvin, connectedBe!!.getDuctNodePosition(), level!!.dimensionId)))

        if (distance == previousDistance) return
        if (abs(distance - previousDistance) < 0.01f) return

        var lerpedDistance = previousDistance + (distance - previousDistance) * 0.1f
        if (lerpedDistance.isInfinite() || lerpedDistance.isNaN()) lerpedDistance = previousDistance
        if (abs(lerpedDistance - distance) < 0.001f) lerpedDistance = distance

        val tempJoint = VSJointAndId(distanceJointId!!, VSDistanceJoint(distanceJoint!!.shipId0, distanceJoint!!.pose0, distanceJoint!!.shipId1, distanceJoint!!.pose1, minDistance = lerpedDistance, maxDistance = lerpedDistance))

        serverLevel.gtpa.updateJoint(distanceJointId!!, tempJoint.joint)
        distanceJoint = tempJoint.joint as VSDistanceJoint
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { return }

    override fun connectTo(other: IUniversalJoint) {
        if (connectedJoint != null) return

        connectedBe = other as? ExtendonBlockEntity ?: return
        if (connectedBe!!.edge != null) edge = connectedBe!!.edge
        else createEdge(blockPos.toDuctNodePos(level!!.dimension().location()), other.pos.toDuctNodePos(connectedBe!!.level!!.dimension().location()))

        if (hasLoadedJointState()) {
            // Keep the restored joint state that this side already owns.
        } else if (connectedBe!!.hasLoadedJointState()) {
            distanceJoint = connectedBe!!.distanceJoint
            distanceJointId = connectedBe!!.distanceJointId
            sphericalJoint = connectedBe!!.sphericalJoint
            sphericalJointId = connectedBe!!.sphericalJointId
            main = false
        } else {
            clearJointStateOnly()
            createJoint()
        }

        level?.playSound(null, blockPos, ClockworkSounds.HOSE_ATTACH.mainEvent, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f)

        super.connectTo(other)
        setChanged()
        sendData()
    }

    override fun disconnect() {
        invalidatePendingJointCreation()
        val other = connectedBe
        val hadLogicalConnection = connectedJoint != null || other != null

        if (!hadLogicalConnection && !hasTrackedJointIds()) {
            clearJointStateOnly()
            connectedBe = null
            connectedJoint = null
            edge = null
            return
        }

        if (other?.edge == null) edge = null
        else if (edge != null) removeEdge()

        if (hasTrackedJointIds()) {
            removeJoint()
        } else {
            clearJointStateOnly()
        }

        connectedBe = null
        connectedJoint = null

        if (hadLogicalConnection) {
            level?.playSound(null, blockPos, ClockworkSounds.HOSE_RELEASE.mainEvent, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f)
        }

        other?.also {
            it.invalidatePendingJointCreation()
            if (it.connectedBe === this || it.connectedJoint === this) {
                it.connectedBe = null
                it.connectedJoint = null
                it.edge = null
                it.clearJointStateOnly()
                it.setChanged()
                it.sendData()
            }
        }

        super.disconnect()
        setChanged()
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

    override fun getRenderBoundingBox(): AABB? {
        return INFINITE_EXTENT_AABB
    }

    private fun createJoint() {
        val level = level as ServerLevel

        if (connectedBe == null) throw IllegalStateException("Null connected block entity")
        val (distanceJoint, sphericalJoint) = buildJointBlueprints(connectedBe!!) ?: return
        if (!distanceJoint.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn("Rejecting corrupted extendon distance joint at {} during creation.", blockPos)
            invalidatePendingJointCreation()
            clearJointStateOnly()
            return
        }
        if (!sphericalJoint.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn("Rejecting corrupted extendon spherical joint at {} during creation.", blockPos)
            invalidatePendingJointCreation()
            clearJointStateOnly()
            return
        }

        this.distanceJoint = distanceJoint
        this.sphericalJoint = sphericalJoint

        val creationToken = ++pendingJointCreationToken
        level.gtpa.addJoint(distanceJoint) {
            if (creationToken != pendingJointCreationToken) {
                level.gtpa.removeJoint(it)
                return@addJoint
            }
            distanceJointId = it
            level.gtpa.removeMatchingJointsExcept(distanceJoint, it)
        }
        level.gtpa.addJoint(sphericalJoint) {
            if (creationToken != pendingJointCreationToken) {
                level.gtpa.removeJoint(it)
                return@addJoint
            }
            sphericalJointId = it
            level.gtpa.removeMatchingJointsExcept(sphericalJoint, it)
        }

        main = true
    }

    private fun removeJoint() {
        val level = level as? ServerLevel
        invalidatePendingJointCreation()

        val idsToRemove = linkedSetOf<Int>()
        distanceJointId?.let(idsToRemove::add)
        sphericalJointId?.let(idsToRemove::add)
        if (level != null) {
            distanceJoint?.also { idsToRemove.addAll(level.gtpa.findMatchingJointIds(it)) }
            sphericalJoint?.also { idsToRemove.addAll(level.gtpa.findMatchingJointIds(it)) }
        }

        idsToRemove.forEach { jointId -> level?.gtpa?.removeJoint(jointId) }

        clearJointStateOnly()
    }


    fun getShipID(): ShipId? {
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
            distanceJointId?.let { compound.putInt(DISTANCE_JOINT_ID_TAG, it) }
            sphericalJointId?.let { compound.putInt(SPHERICAL_JOINT_ID_TAG, it) }
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("ConnectedPosX")) {
            val bpos = BlockPos(compound.getInt("ConnectedPosX"),compound.getInt("ConnectedPosY"),compound.getInt("ConnectedPosZ"))
            distanceJointId = if (compound.contains(DISTANCE_JOINT_ID_TAG)) compound.getInt(DISTANCE_JOINT_ID_TAG) else null
            sphericalJointId = if (compound.contains(SPHERICAL_JOINT_ID_TAG)) compound.getInt(SPHERICAL_JOINT_ID_TAG) else null
            if (!clientPacket) {
                loadFn = {
                    val other = level!!.getBlockEntity(bpos) as? ExtendonBlockEntity
                    if (other == null) {
                        disconnect()
                        true
                    } else {
                        restoreConnection(other)
                    }
                }
            } else {
                connectedBe = level?.getBlockEntity(BlockPos(compound.getInt("ConnectedPosX"),compound.getInt("ConnectedPosY"),compound.getInt("ConnectedPosZ"))) as? ExtendonBlockEntity
                connectedJoint = connectedBe
                connectedBe?.connectedJoint = this
                connectedBe?.connectedBe = this
            }
            main = compound.getBoolean("IsMain")
        } else {
            connectedBe = null
            connectedJoint = null
            edge = null
            clearJointStateOnly()
        }

        super.read(compound, clientPacket)
    }

    override fun remove() {
        invalidatePendingJointCreation()
        disconnect()
        super.remove()
    }

    override fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        val og = super.heatableGoggleTooltip(tooltip, isPlayerSneaking)
        tooltip.add(Component.empty())
        val other = if (connectedBe != null) {
            tooltip.add(Component.translatable("vs_clockwork.hose_port.connected"))
            // display connected port position
            tooltip.add(Component.translatable("vs_clockwork.hose_port.connected_to",
                Component.literal("${connectedBe!!.blockPos.x}, ${connectedBe!!.blockPos.y}, ${connectedBe!!.blockPos.z}")
                    .withStyle(ChatFormatting.AQUA)))

            // extendon specific: display current length
            val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

            val currentLength = (max(1.5f,(gasToDistance(kelvin, getDuctNodePosition(), level!!.dimensionId) + gasToDistance(kelvin, connectedBe!!.getDuctNodePosition(), level!!.dimensionId))) * 10.0f).roundToInt() / 10.0f
            tooltip.add(Component.translatable("vs_clockwork.extendon.current_length").append(Component.literal(currentLength.toString()).append("m").withStyle(ChatFormatting.YELLOW)))


            // display other node's info
            tooltip.add(Component.translatable("vs_clockwork.hose_port.other_port_info").withStyle(ChatFormatting.GRAY))
            connectedBe!!.safeHeatableGoggleTooltip(tooltip, isPlayerSneaking)
        } else {
            tooltip.add(Component.translatable("vs_clockwork.hose_port.disconnected"))
        }
        return og || other
    }

    override fun getConnectionItem(): ItemStack {
        return ClockworkItems.EXTENDON_HOSE.asStack()
    }

    fun safeHeatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        var found = false

        val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

        if (kelvin.getTemperatureAt(this.getDuctNodePosition()) > 0.0) {
            tooltip.add(Component.literal("Temperature: ${kelvin.getTemperatureAt(this.getDuctNodePosition()).toInt()} K").withStyle(ChatFormatting.GOLD))
            found = true
        }
        if (kelvin.nodeInfo[this.getDuctNodePosition()] != null && kelvin.nodeInfo[this.getDuctNodePosition()]!!.currentEnergy > 0.0 && isPlayerSneaking) {
            val currentEnergy = kelvin.nodeInfo[this.getDuctNodePosition()]!!.currentEnergy
            if (currentEnergy < 100000.0) {
                tooltip.add(Component.literal("Thermal Energy: ${(currentEnergy).roundToInt()} J").withStyle(ChatFormatting.RED))
            } else {
                tooltip.add(Component.literal("Thermal Energy: ${(currentEnergy/1000.0).roundToInt()} kJ").withStyle(ChatFormatting.RED))
            }
        }
        if (kelvin.getPressureAt(this.getDuctNodePosition()) > 0.0) {
            val currentPressure = kelvin.getPressureAt(this.getDuctNodePosition())
            if (currentPressure < 100000.0) {
                tooltip.add(Component.literal("Pressure: ${currentPressure.roundToInt()} Pa").withStyle(ChatFormatting.BLUE))
            } else {
                tooltip.add(Component.literal("Pressure: ${(currentPressure/1000.0).roundToInt()} kPa").withStyle(ChatFormatting.BLUE))
            }
            found = true
        }
        if (kelvin.getGasMassAt(this.getDuctNodePosition()).isNotEmpty()) {
            //tooltip.add(Component.literal("Gas Masses:"))
            val sortedByAmount = kelvin.getGasMassAt(this.getDuctNodePosition()).entries.sortedByDescending { it.value }
            val rows = mutableListOf<Pair<Component, Component>>()
            val finishedComponents = mutableListOf<Component>()

            for (entry in sortedByAmount) {
                val iconComponent = Component.literal(ClockworkGasses.getDisplayCharacterCode(entry.key)).withStyle {it.withFont(
                    ClockworkGasses.ICON_FONT_LOCATION)}
                val nameComponent = if (isPlayerSneaking) Component.literal(" ${entry.key.name} ").withStyle(ChatFormatting.GRAY)
                else Component.literal(" ")
                val amtComponent =
                    if (entry.value > 0 && entry.value < 1) Component.literal("${(entry.value*1000.0).roundToInt()}g")
                    else if (entry.value >= 1) Component.literal("${(entry.value*1000.0).roundToInt()/1000.0}kg")
                    else null
                if (amtComponent != null) {
                    val finalComponent = Component.empty().append(iconComponent).append(nameComponent).append(amtComponent)
                    finishedComponents.add(finalComponent)
                }
            }
            for (num in finishedComponents.indices step 2) {
                if (num + 1 < finishedComponents.size) {
                    rows.add(Pair(finishedComponents[num], finishedComponents[num + 1]))
                } else {
                    rows.add(Pair(finishedComponents[num], Component.empty()))
                }
            }
            for (row in rows) {
                val rowComponent = Component.empty()
                    .append(row.first)
                    .append(Component.literal("  "))
                    .append(row.second)
                tooltip.add(rowComponent)
            }

            found = true
        }

        if (!found) {
            tooltip.add(Component.literal("Connected node is empty.").withStyle(ChatFormatting.DARK_GRAY).withStyle(
                ChatFormatting.ITALIC))
        }

        return found
    }

    companion object {
        private const val DISTANCE_JOINT_ID_TAG = "DistanceJointId"
        private const val SPHERICAL_JOINT_ID_TAG = "SphericalJointId"
        private const val MAX_RESTORE_TRIES = 40

        // Calculates volume of cylinder via Ideal Gas Law, and then calculates said cylinder's height
        // Doesn't account for the elastic force of the hose, because doing so would require solving a cubic polynomial
        fun gasToDistance(network: DuctNetwork<*>, pos: DuctNodePos, dimensionId: DimensionId): Float {
            var moles = 0.0
            for ((gas, mass) in network.getGasMassAt(pos)) moles +=  gas.massToMoles(mass)

            val pressure = vsApi.getServerShipWorld(ValkyrienSkiesMod.currentServer)?.aerodynamicUtils?.getAirPressureForY(pos.y, dimensionId) ?: 1.0
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

}
