package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustColorTransitionOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.GravitronController
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.world.clipIncludeShips

class ClampotronBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(type, pos, state), IThinkable {
    lateinit var reach: ScrollValueBehaviour
    var heldShip = -1L; private set
    var anchor = Vector3d(); private set
    var strain = 0f; private set
    var positionError = 0.0; private set
    var enabled = false; private set
    private var rotation = Quaterniond()
    private var controller: ClampotronController? = null
    private var overloadTicks = 0
    private var cooldown = 0
    private var heldMass = 0.0
    val facing: Direction get() = blockState.getValue(BlockStateProperties.FACING)
    val reachSide: Direction get() = if (facing.axis == Direction.Axis.Y) Direction.NORTH else Direction.UP
    val enableSide: Direction get() = if (facing.axis == Direction.Axis.X) Direction.NORTH else Direction.EAST
    val statusSide: Direction get() = reachSide.opposite

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        reach = ScrollValueBehaviour(Component.translatable("block.vs_clockwork.clampotron.reach"), this, Slot())
            .between(1, 32).withFormatter { "$it m" }.requiresWrench()
        reach.setValue(3)
        behaviours.add(reach)
    }

    override fun acceptsThinkable(side: Direction) = side == enableSide || side == reachSide
    override fun providesThinkable(side: Direction) = side == statusSide || side == enableSide.opposite
    override fun thinkablePortName(side: Direction): Component? = when {
        side == enableSide -> Component.translatable("thinkable.vs_clockwork.port.clampotron_enable")
        side == reachSide -> Component.translatable("thinkable.vs_clockwork.port.clampotron_reach")
        side == statusSide -> Component.translatable("thinkable.vs_clockwork.port.clampotron_holding")
        providesThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.clampotron_error")
        else -> null
    }
    override fun thinkableValue(side: Direction): Double? = when {
        !providesThinkable(side) -> null
        side == statusSide -> if (heldShip >= 0) 15.0 else 0.0
        heldShip >= 0 -> positionError
        else -> null
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) { ClampotronClient.track(this); return }
        val world = level as? ServerLevel ?: return
        val input = ThinkableConnections.input(world, blockPos, enableSide)
        val requested = if (input == null) world.hasNeighborSignal(blockPos) else input.value?.let { it != 0.0 } == true
        val on = requested && ClampotronPower.running(getSpeed().toDouble(), isOverStressed)
        if (on != enabled) {
            enabled = on
            sound(if (on) ClockworkSounds.CLAMPOTRON_POWER_ON.mainEvent!! else ClockworkSounds.CLAMPOTRON_POWER_OFF.mainEvent!!)
            sendData()
        }
        if (!on) { release(); return }
        if (cooldown > 0) { cooldown--; return }
        val carrier = world.getShipManagingPos(blockPos)
        val reachInput = ThinkableConnections.input(world, blockPos, reachSide)
        val distance = if (reachInput == null) reach.value.toDouble() else reachInput.value
        if (distance == null || !distance.isFinite()) { release(); return }
        val target = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
            .fma(0.5 + distance.coerceIn(1.0, 32.0), Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble()))
        if (heldShip < 0 && world.gameTime % 5L == 0L) {
            val from = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
                .fma(0.55, Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble()))
            val to = Vector3d(target)
            carrier?.shipToWorld?.transformPosition(from)
            carrier?.shipToWorld?.transformPosition(to)
            val hit = world.clipIncludeShips(ClipContext(vec(from), vec(to), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, null), shouldTransformHitPos = false, skipShip = carrier?.id)
            if (hit.type == HitResult.Type.BLOCK && !VdexConstruction.locked(world, hit.blockPos)) {
                val ship = world.getShipManagingPos(hit.blockPos)?.let { world.shipObjectWorld.loadedShips.getById(it.id) }
                if (ship != null && ship.id != carrier?.id && !ship.isStatic &&
                    ship.getAttachment<GravitronController>()?.data == null &&
                    ship.getAttachment<ClampotronController>()?.active() != true &&
                    world.players().none { org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState.getState(it).shipID == ship.id }) {
                    heldShip = ship.id
                    anchor = Vector3d(hit.location.x, hit.location.y, hit.location.z)
                    rotation = Quaterniond(ship.transform.shipToWorldRotation)
                    carrier?.transform?.shipToWorldRotation?.invert(Quaterniond())?.mul(rotation, rotation)
                    controller = ClampotronController.getOrCreate(ship).also {
                        it.strain = 0f
                        it.error = 0.0
                    }
                    sound(ClockworkSounds.CLAMPOTRON_GRAB.mainEvent!!)
                    sendData()
                }
            }
        }
        if (heldShip < 0) return
        val ship = world.shipObjectWorld.loadedShips.getById(heldShip)
        if (ship == null || ship.isStatic || ship.id == carrier?.id || ship.getAttachment<GravitronController>()?.data != null) {
            release(); return
        }
        val control = controller ?: run { release(); return }
        val mass = ship.inertiaData.mass
        if (mass != heldMass) {
            heldMass = mass
            orCreateNetwork?.updateStressFor(this, calculateStressApplied())
        }
        if (!ClampotronPower.running(getSpeed().toDouble(), isOverStressed) || heldShip < 0) { release(); return }
        strain = control.strain
        positionError = control.error
        overloadTicks = ClampotronPower.overloadTicks(overloadTicks, strain)
        if (ClampotronPower.shouldSnap(overloadTicks, positionError)) { release(snapped = true); cooldown = 40; return }
        control.grip = ClampotronController.Grip(blockPos.asLong(), carrier?.id ?: -1, Vector3d(anchor), target, Quaterniond(rotation),
            ClampotronPower.force(getSpeed().toDouble(), ClockworkConfig.SERVER.clampotronForcePerRpm))
        if (world.gameTime % 4L == 0L) sendData()
    }

    private fun sound(event: SoundEvent) {
        val world = level ?: return
        val p = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
        world.getShipManagingPos(blockPos)?.shipToWorld?.transformPosition(p)
        world.playSound(null, p.x, p.y, p.z, event, SoundSource.BLOCKS, 0.8f, 1f)
    }

    private fun snapParticles() {
        val world = level as? ServerLevel ?: return
        val ship = world.shipObjectWorld.loadedShips.getById(heldShip) ?: return
        val start = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
            .fma(0.55, Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble()))
        world.getShipManagingPos(blockPos)?.shipToWorld?.transformPosition(start)
        val end = ship.shipToWorld.transformPosition(anchor, Vector3d())
        val inward = Vector3d(start).sub(end)
        if (inward.lengthSquared() > 1e-6) end.add(inward.normalize(0.12))
        val dust = DustColorTransitionOptions(Vector3f(1f, 0.25f, 0.3f), Vector3f(0.765f, 0.627f, 0.89f), 1.5f)
        for (point in listOf(start, end)) {
            world.sendParticles(dust, point.x, point.y, point.z, 24, 0.25, 0.25, 0.25, 0.08)
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 18, 0.12, 0.12, 0.12, 0.3)
        }
        for (i in 1 until 12) {
            val point = Vector3d(start).lerp(end, i / 12.0)
            world.sendParticles(dust, point.x, point.y, point.z, 3, 0.12, 0.12, 0.12, 0.04)
        }
    }

    private fun release(snapped: Boolean = false) {
        controller?.let { if (it.grip?.owner == blockPos.asLong()) it.grip = null }
        controller = null
        if (heldShip < 0) return
        if (snapped) snapParticles()
        heldShip = -1
        heldMass = 0.0
        strain = 0f
        positionError = 0.0
        overloadTicks = 0
        if (level?.isClientSide == false) {
            sound(if (snapped) ClockworkSounds.CLAMPOTRON_SNAP.mainEvent!! else ClockworkSounds.CLAMPOTRON_RELEASE.mainEvent!!)
            sendData()
        }
        if (level?.isClientSide == false && hasNetwork()) orCreateNetwork?.updateStressFor(this, calculateStressApplied())
    }

    override fun calculateStressApplied(): Float {
        if (level?.isClientSide == true) return lastStressApplied
        lastStressApplied = ClampotronPower.stress(heldMass, ClockworkConfig.SERVER.clampotronMassPerStress).toFloat()
        return lastStressApplied
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        if (level?.isClientSide == false && !ClampotronPower.running(getSpeed().toDouble(), isOverStressed)) {
            if (heldShip >= 0) cooldown = 40
            if (enabled) sound(ClockworkSounds.CLAMPOTRON_POWER_OFF.mainEvent!!)
            enabled = false
            release(snapped = isOverStressed)
            sendData()
        }
    }

    override fun invalidate() { release(); super.invalidate() }
    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        if (!clientPacket) return
        tag.putLong("HeldShip", heldShip)
        tag.putDouble("AnchorX", anchor.x); tag.putDouble("AnchorY", anchor.y); tag.putDouble("AnchorZ", anchor.z)
        tag.putFloat("Strain", strain); tag.putBoolean("Enabled", enabled)
    }
    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (!clientPacket) return
        heldShip = if (tag.contains("HeldShip")) tag.getLong("HeldShip") else -1
        anchor = Vector3d(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"))
        strain = tag.getFloat("Strain"); enabled = tag.getBoolean("Enabled")
    }

    class Slot : ValueBoxTransform.Sided() {
        override fun getSouthLocation() = Vec3(0.5, 0.5, 1.0)
        override fun isSideActive(state: BlockState, side: Direction) = side.axis != state.getValue(BlockStateProperties.FACING).axis
    }
    companion object { fun vec(v: Vector3d) = Vec3(v.x, v.y, v.z) }
}
