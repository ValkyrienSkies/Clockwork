package org.valkyrienskies.clockwork.util.universal_joint

import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement
import com.simibubi.create.content.schematics.requirement.ItemRequirement
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
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftStressSound
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.UUID
import kotlin.math.roundToInt

abstract class GasLinkBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), BlockAnchorEndpoint, SpecialBlockEntityItemRequirement {
    override var connectedJoint: IUniversalJoint? = null
    override var pos = blockPos
    override var linkId: UUID? = null
    override var connectedPos: BlockPos? = null
    override var main = false
    var connectedBe: GasLinkBlockEntity? = null
    var distanceJoint: VSDistanceJoint? = null
    var distanceJointId: Int? = null
    var sphericalJoint: VSD6Joint? = null
    var sphericalJointId: Int? = null
    internal val legacyJointIds = mutableSetOf<Int>()
    final override var stress = 0f
        private set
    var previousStress = 0f
        private set
    var displayStress = 0f
        private set
    private var stressSound: UniversalShaftStressSound? = null
    var connectionLength = 0.0
        private set

    internal fun updateLength(value: Double) {
        if (connectionLength == value) return
        connectionLength = value
        if (linkKind == BlockAnchorLinks.Kind.HOSE) sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}
    override fun getRenderBoundingBox(): AABB = INFINITE_EXTENT_AABB
    fun getShipID() = level.getShipManagingPos(blockPos)?.id

    override fun setConnection(id: UUID?, target: BlockPos?, primary: Boolean) {
        val changed = linkId != id || connectedPos != target || main != primary
        if (linkId != id || connectedPos != target) clearPhysics()
        linkId = id; connectedPos = target; main = primary
        connectedBe = target?.takeIf { level?.hasChunkAt(it) == true }?.let { level?.getBlockEntity(it) as? GasLinkBlockEntity }
            ?.takeIf { it.linkKind == linkKind }
        connectedJoint = connectedBe
        if (target == null) updateStress(0f)
        if (changed) { setChanged(); sendData() }
    }

    internal fun clearPhysics() {
        distanceJoint = null; distanceJointId = null
        sphericalJoint = null; sphericalJointId = null
    }

    override fun updateStress(value: Float) {
        val next = (value.coerceIn(0f, 1f) * 20).toInt() / 20f
        if (stress == next) return
        stress = next
        sendData()
    }

    override fun tick() {
        super.tick()
        val world = level ?: return
        if (world is ServerLevel) BlockAnchorLinks.get(world).reconcile(world, this) else {
            connectedBe = connectedPos?.takeIf { world.hasChunkAt(it) }?.let { world.getBlockEntity(it) as? GasLinkBlockEntity }
                ?.takeIf { it.linkId == linkId && it.connectedPos == blockPos && it.linkKind == linkKind }
            connectedJoint = connectedBe
            previousStress = displayStress
            displayStress += (stress - displayStress) * .4f
            if (main && stress > .15f && stressSound?.isStopped != false)
                stressSound = UniversalShaftStressSound(this).also { Minecraft.getInstance().soundManager.play(it) }
        }
    }

    override fun tryConnect(level: Level, blockPos: BlockPos): Boolean {
        if (level !is ServerLevel || this.level !== level || connectedPos != null) return false
        val other = level.getBlockEntity(blockPos) as? GasLinkBlockEntity ?: return false
        if (other.connectedPos != null) return false
        return BlockAnchorLinks.get(level).add(level, this, other) != null
    }

    override fun connectTo(other: IUniversalJoint) {
        val world = level as? ServerLevel ?: return
        if (other is GasLinkBlockEntity && connectedPos == null && other.connectedPos == null)
            BlockAnchorLinks.get(world).add(world, this, other)
    }

    override fun disconnect() {
        val world = level as? ServerLevel ?: return
        val data = BlockAnchorLinks.get(world)
        val link = data.at(blockPos)
        if (link != null) data.remove(world, link) else setConnection(null, null, false)
        ClockworkSounds.HOSE_RELEASE.playOnServer(world, blockPos)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        linkId?.let { tag.putUUID("BlockAnchorLink", it) }
        connectedPos?.let {
            tag.putInt("ConnectedPosX", it.x); tag.putInt("ConnectedPosY", it.y); tag.putInt("ConnectedPosZ", it.z)
            tag.putLong("ConnectionOffset", it.subtract(blockPos).asLong())
            tag.putBoolean("IsMain", main)
        }
        if (clientPacket) { tag.putFloat("Stress", stress); tag.putDouble("ConnectionLength", connectionLength) }
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        val previousLink = linkId; val previousPos = connectedPos
        linkId = if (tag.hasUUID("BlockAnchorLink")) tag.getUUID("BlockAnchorLink") else null
        connectedPos = if (tag.contains("ConnectedPosX")) BlockPos(tag.getInt("ConnectedPosX"), tag.getInt("ConnectedPosY"), tag.getInt("ConnectedPosZ")) else null
        main = tag.getBoolean("IsMain")
        connectionLength = tag.getDouble("ConnectionLength").takeIf { it.isFinite() && it >= 0 } ?: 0.0
        if (clientPacket) stress = tag.getFloat("Stress").takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        else if (linkId == null) for (key in listOf("DistanceJointId", "SphericalJointId"))
            if (tag.contains(key)) legacyJointIds.add(tag.getInt(key))
        if (previousLink != linkId || previousPos != connectedPos) {
            connectedBe = null; connectedJoint = null; clearPhysics()
        }
        super.read(tag, clientPacket)
    }

    override fun getRequiredItems(state: BlockState?): ItemRequirement = if (connectedPos != null && main)
        ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ClockworkItems.EXTENDON_HOSE.asStack()) else ItemRequirement.NONE

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


}
