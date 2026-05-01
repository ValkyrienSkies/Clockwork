package org.valkyrienskies.clockwork.content.logistics.gas.hoseport

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import dev.architectury.platform.Platform
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.util.findMatchingJoint
import org.valkyrienskies.clockwork.util.findMatchingJointIds
import org.valkyrienskies.clockwork.util.hasFinitePoseData
import org.valkyrienskies.clockwork.util.removeMatchingJointsExcept
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity.Companion.getQuaterniond
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.roundToInt

class HosePortBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), IUniversalJoint {
    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {
    }

    override var connectedJoint: IUniversalJoint? = null
    override var pos: BlockPos = pos

    var connectedBe: HosePortBlockEntity? = null
    var edge: DuctEdge? = null

    var distanceJoint: VSDistanceJoint? = null
    var distanceJointId: Int? = null

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
        main = false
    }

    private fun hasTrackedJointId(): Boolean = distanceJointId != null

    private fun tryAdoptTrackedJoint(serverLevel: ServerLevel): Boolean? {
        val trackedJointId = distanceJointId ?: return false
        val existingJoint = serverLevel.gtpa.getJointById(trackedJointId) ?: return null

        if (existingJoint !is VSDistanceJoint || !existingJoint.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn(
                "Discarding invalid restored hose port joint at {} (joint={}).",
                blockPos,
                trackedJointId
            )
            removeJoint()
            return false
        }

        distanceJoint = existingJoint
        serverLevel.gtpa.removeMatchingJointsExcept(existingJoint, trackedJointId)
        return true
    }

    private fun buildJointBlueprint(other: HosePortBlockEntity): VSDistanceJoint? {
        val serverLevel = level as? ServerLevel ?: return null
        val shipId0 = getShipID()
        val shipId1 = other.getShipID()
        val pos0 = blockPos.toJOMLD()
        val pos1 = other.blockPos.toJOMLD()
        val quater0 = getQuaterniond(serverLevel.getBlockState(blockPos).getValue(BlockStateProperties.FACING))
        val quater1 = getQuaterniond(serverLevel.getBlockState(other.blockPos).getValue(BlockStateProperties.FACING))
        val distanceInWorld = serverLevel.toWorldCoordinates(pos0).distance(serverLevel.toWorldCoordinates(pos1))

        return VSDistanceJoint(
            pose0 = VSJointPose(pos0, quater0),
            pose1 = VSJointPose(pos1, quater1),
            shipId0 = shipId0,
            shipId1 = shipId1,
            minDistance = 0f,
            maxDistance = (distanceInWorld + 1.0).roundToInt().toFloat()
        )
    }

    private fun tryAdoptMatchingJoint(serverLevel: ServerLevel, other: HosePortBlockEntity): Boolean {
        val expectedJoint = buildJointBlueprint(other) ?: return false
        val matchingJoint = serverLevel.gtpa.findMatchingJoint(expectedJoint) ?: return false
        val adoptedJoint = matchingJoint.joint as? VSDistanceJoint
        if (adoptedJoint == null || !adoptedJoint.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn(
                "Discarding invalid structurally matched hose port joint at {} (joint={}).",
                blockPos,
                matchingJoint.jointId
            )
            serverLevel.gtpa.removeJoint(matchingJoint.jointId)
            return false
        }

        distanceJoint = adoptedJoint
        distanceJointId = matchingJoint.jointId
        serverLevel.gtpa.removeMatchingJointsExcept(adoptedJoint, matchingJoint.jointId)
        return true
    }

    private fun restoreConnection(other: HosePortBlockEntity): Boolean {
        val serverLevel = level as? ServerLevel ?: return false

        if (connectedJoint != null && connectedJoint !== other) {
            disconnect()
        }
        if (other.connectedJoint != null && other.connectedJoint !== this) {
            other.disconnect()
        }

        val candidates = mutableListOf<HosePortBlockEntity>()
        if (main && hasTrackedJointId()) {
            candidates.add(this)
        }
        if (other.main && other.hasTrackedJointId() && !candidates.contains(other)) {
            candidates.add(other)
        }
        if (hasTrackedJointId() && !candidates.contains(this)) {
            candidates.add(this)
        }
        if (other.hasTrackedJointId() && !candidates.contains(other)) {
            candidates.add(other)
        }

        var waitingOnRestoredJoint = false
        for (candidate in candidates) {
            when (candidate.tryAdoptTrackedJoint(serverLevel)) {
                true -> {
                    deferredRestoreTries = 0
                    if (candidate === this) {
                        connectTo(other)
                    } else {
                        other.connectTo(this)
                    }
                    return true
                }

                null -> waitingOnRestoredJoint = true
                false -> Unit
            }
        }

        if (tryAdoptMatchingJoint(serverLevel, other)) {
            deferredRestoreTries = 0
            connectTo(other)
            return true
        }

        if (waitingOnRestoredJoint) {
            deferredRestoreTries++
            if (deferredRestoreTries < MAX_RESTORE_TRIES) {
                if (deferredRestoreTries % 20 == 0) {
                    ClockworkMod.LOGGER.warn(
                        "Deferring hose port joint restore at {} while waiting for tracked joints to load (attempt {}).",
                        blockPos,
                        deferredRestoreTries
                    )
                }
                return false
            }

            ClockworkMod.LOGGER.warn(
                "Discarding stale hose port joint references at {} after {} restore attempts.",
                blockPos,
                deferredRestoreTries
            )
            if (hasTrackedJointId()) {
                removeJoint()
            }
            if (other.hasTrackedJointId()) {
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

        if (distanceJointId != null && distanceJoint != null) {
            val level = level as ServerLevel
            level.gtpa.updateJoint(distanceJointId!!, this.distanceJoint!!)
        }
    }

    override fun connectTo(other: IUniversalJoint) {
        if (connectedJoint != null) return

        connectedBe = other as? HosePortBlockEntity ?: return
        if (connectedBe!!.edge != null) edge = connectedBe!!.edge
        else createEdge(blockPos.toDuctNodePos(level!!.dimension().location()), other.pos.toDuctNodePos(connectedBe!!.level!!.dimension().location()))

        if (distanceJoint != null && distanceJointId != null) {
            // Keep the restored joint state that this side already owns.
        } else if (connectedBe!!.distanceJoint != null && connectedBe!!.distanceJointId != null) {
            distanceJoint = connectedBe!!.distanceJoint
            distanceJointId = connectedBe!!.distanceJointId
            main = false
        } else createJoint()

        level?.playSound(null, blockPos, ClockworkSounds.HOSE_ATTACH.mainEvent, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f)

        super.connectTo(other)
        sendData()
    }

    override fun disconnect() {
        invalidatePendingJointCreation()
        val other = connectedBe
        val hadLogicalConnection = connectedJoint != null || other != null

        if (!hadLogicalConnection && distanceJointId == null) {
            clearJointStateOnly()
            connectedBe = null
            connectedJoint = null
            edge = null
            return
        }

        if (other?.edge == null) edge = null
        else if (edge != null) removeEdge()

        if (distanceJointId != null) {
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
        val kelvin = ClockworkMod.getKelvin(level)
        edge = PipeDuctEdge(nodeA = nodeA, nodeB = nodeB, type = ConnectionType.PIPE)
        kelvin.addEdge(nodeA, nodeB, edge!!)
    }

    private fun removeEdge() {
        val kelvin = ClockworkMod.getKelvin(level)
        kelvin.removeEdge(edge!!.nodeA,edge!!.nodeB)
        edge = null
    }

    private fun createJoint() {
        val level = level as ServerLevel

        if (connectedBe == null) throw IllegalStateException("Null connected block entity")
        distanceJoint = buildJointBlueprint(connectedBe!!) ?: return
        if (!distanceJoint!!.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn("Rejecting corrupted hose port joint at {} during creation.", blockPos)
            invalidatePendingJointCreation()
            clearJointStateOnly()
            return
        }
        val creationToken = ++pendingJointCreationToken
        level.gtpa.addJoint(distanceJoint!!) {
            if (creationToken != pendingJointCreationToken) {
                level.gtpa.removeJoint(it)
                return@addJoint
            }
            distanceJointId = it
            level.gtpa.removeMatchingJointsExcept(distanceJoint!!, it)
        }

        main = true
    }

    private fun removeJoint() {
        val level = level as ServerLevel
        invalidatePendingJointCreation()

        val idsToRemove = linkedSetOf<Int>()
        distanceJointId?.let(idsToRemove::add)
        distanceJoint?.also { idsToRemove.addAll(level.gtpa.findMatchingJointIds(it)) }
            ?: connectedBe?.let { other -> buildJointBlueprint(other)?.also { idsToRemove.addAll(level.gtpa.findMatchingJointIds(it)) } }

        idsToRemove.forEach(level.gtpa::removeJoint)

        clearJointStateOnly()
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
            distanceJointId?.let { compound.putInt(DISTANCE_JOINT_ID_TAG, it) }
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("ConnectedPosX")) {
            val bpos = BlockPos(compound.getInt("ConnectedPosX"),compound.getInt("ConnectedPosY"),compound.getInt("ConnectedPosZ"))
            distanceJointId = if (compound.contains(DISTANCE_JOINT_ID_TAG)) compound.getInt(DISTANCE_JOINT_ID_TAG) else null
            if (!clientPacket) {
                loadFn = {
                    val other = level!!.getBlockEntity(bpos) as? HosePortBlockEntity
                    if (other == null) {
                        disconnect()
                        true
                    } else {
                        restoreConnection(other)
                    }
                }
            } else {
                connectedBe = level?.getBlockEntity(BlockPos(compound.getInt("ConnectedPosX"),compound.getInt("ConnectedPosY"),compound.getInt("ConnectedPosZ"))) as? HosePortBlockEntity
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

            // display other node's info
            tooltip.add(Component.translatable("vs_clockwork.hose_port.other_port_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
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

        val kelvin = if (level is PonderLevel) ClockworkMod.getKelvin(level)
            else if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin()
            else ClockworkModClient.getKelvin()

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
        private const val MAX_RESTORE_TRIES = 40
    }
}
