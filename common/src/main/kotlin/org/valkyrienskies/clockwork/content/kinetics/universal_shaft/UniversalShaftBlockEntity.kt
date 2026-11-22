package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import org.valkyrienskies.clockwork.util.universal_joint.BlockAnchorLinks
import org.valkyrienskies.clockwork.util.universal_joint.BlockAnchorEndpoint

import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import net.minecraft.core.BlockPos
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.UUID

class UniversalShaftBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos, state), BlockAnchorEndpoint, SpecialBlockEntityItemRequirement {
    override val linkKind = BlockAnchorLinks.Kind.SHAFT
    override var connectedJoint: IUniversalJoint? = null
    override var pos = blockPos
    override var connectedPos: BlockPos? = null
    var connectedBe: UniversalShaftBlockEntity? = null
    override var linkId: UUID? = null
    override var stress = 0f
        private set
    var previousStress = 0f
        private set
    var displayStress = 0f
        private set
    private var stressSound: UniversalShaftStressSound? = null

    override var main: Boolean = false

    override val maxCreationDistance: Double
        get() = ClockworkConfig.SERVER.universalJoint.maxUniversalJointDistance

    // Custom Propagation
    override fun propagateRotationTo(
        target: KineticBlockEntity, stateFrom: BlockState, stateTo: BlockState, diff: BlockPos,
        connectedViaAxes: Boolean, connectedViaCogs: Boolean
    ): Float {
        if (connectedJoint == null || target.blockPos != connectedPos) return 0f

        // Output must be negated whenever the two shafts share an axis sign
        // (e.g. both directions are positive axis or both are negative axis)
        // Why? Idk it's probably create's fault.
        val facingFrom = stateFrom.getValue(BlockStateProperties.FACING)
        val facingTo = stateTo.getValue(BlockStateProperties.FACING)
        return -facingFrom.axisDirection.step.toFloat() * facingTo.axisDirection.step.toFloat()
    }

    override fun getRenderBoundingBox(): AABB? {
        val aabb = AABB(this.worldPosition)
        if (connectedPos == null) return aabb
        // Yes this is a stupid way to do it but its what Create does so sue me
        return aabb.inflate(maxCreationDistance);
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
        val world = level as? ServerLevel ?: return
        val data = BlockAnchorLinks.get(world)
        val link = data.at(blockPos)
        if (link != null) data.remove(world, link) else setConnection(null, null, false)
    }

    override fun connectTo(other: IUniversalJoint) {
        val world = level as? ServerLevel ?: return
        if (other is UniversalShaftBlockEntity) BlockAnchorLinks.get(world).add(world, this, other)
    }

    override fun tryConnect(level: Level, blockPos: BlockPos): Boolean {
        if (level !is ServerLevel || this.level !== level) return false
        val other = level.getBlockEntity(blockPos) as? UniversalShaftBlockEntity ?: return false
        if (connectedPos != null || other.connectedPos != null) return false
        return BlockAnchorLinks.get(level).add(level, this, other) != null
    }

    override fun setConnection(id: UUID?, target: BlockPos?, primary: Boolean) {
        val changed = linkId != id || connectedPos != target || main != primary
        val other = target?.takeIf { level?.hasChunkAt(it) == true }?.let { level?.getBlockEntity(it) as? UniversalShaftBlockEntity }
        val reconnect = connectedPos != target || connectedBe !== other
        if (reconnect && !isRemoved) {
            detachKinetics()
            clearKineticInformation()
            setSpeed(0f)
        }
        linkId = id; connectedPos = target; main = primary
        connectedBe = other; connectedJoint = other
        if (target == null) updateStress(0f)
        if (reconnect && !isRemoved) attachKinetics()
        if (changed) { setChanged(); sendData() }
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
        if (world is ServerLevel) BlockAnchorLinks.get(world).reconcile(world, this)
        else {
            previousStress = displayStress
            displayStress += (stress - displayStress) * .4f
            connectedBe = connectedPos?.takeIf { world.hasChunkAt(it) }?.let { world.getBlockEntity(it) as? UniversalShaftBlockEntity }
                ?.takeIf { it.connectedPos == blockPos && it.linkId == linkId }
            connectedJoint = connectedBe
            if (main && stress > .15f && stressSound?.isStopped != false)
                stressSound = UniversalShaftStressSound(this).also { Minecraft.getInstance().soundManager.play(it) }
        }
    }

    override fun isThisJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }

    override fun getConnectionItem(): ItemStack {
        return ClockworkItems.UNIVERSAL_SHAFT_ITEM.asStack()
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        if (clientPacket) compound.putFloat("Stress", stress)
        linkId?.let { compound.putUUID("ShaftLink", it) }
        if (connectedPos != null) {
            val offset = connectedPos!!.subtract(worldPosition)
            compound.putInt("otherPosXr",offset.x)
            compound.putInt("otherPosYr",offset.y)
            compound.putInt("otherPosZr",offset.z)
            compound.putBoolean("main", main)

            // Used for onPaste
            val ship = level.getShipManagingPos(connectedPos!!)
            compound.putLong("otherShipId", ship?.id ?: -1L)
        }

        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        val previousLink = linkId
        if (clientPacket) stress = compound.getFloat("Stress").takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        linkId = if (compound.hasUUID("ShaftLink")) compound.getUUID("ShaftLink") else null
        // Old serialization
        if (compound.contains("otherPosX")) {
            connectedPos = BlockPos(compound.getInt("otherPosX"),compound.getInt("otherPosY"),compound.getInt("otherPosZ"))
            main = compound.getBoolean("main")
        }
        // New serialization
        else if (compound.contains("otherPosXr")) {
            val offset = BlockPos(compound.getInt("otherPosXr"),compound.getInt("otherPosYr"),compound.getInt("otherPosZr"))
            connectedPos = worldPosition.offset(offset)
            main = compound.getBoolean("main")
        } else {
            connectedPos = null
            connectedBe = null
            connectedJoint = null
            main = false
            setSpeed(0f)
        }

        super.read(compound, clientPacket)
        if (previousLink != linkId || connectedBe?.blockPos != connectedPos || connectedBe?.isRemoved == true) {
            connectedBe = null
            connectedJoint = null
        }
    }

    override fun getRequiredItems(state: BlockState?): ItemRequirement? {
        return if (connectedPos != null && main)
            ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ClockworkItems.UNIVERSAL_SHAFT_ITEM.asStack())
        else ItemRequirement.NONE
    }

}
