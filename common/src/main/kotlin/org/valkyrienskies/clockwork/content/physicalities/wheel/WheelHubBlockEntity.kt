package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.BlockHitResult
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.core.api.bodies.VsBodyCreateData
import org.valkyrienskies.core.api.bodies.shape.WheelBodyShapeData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.impl.api_impl.config.ConfigPhysicsBackendType
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.util.WeakHashMap
import java.util.Locale

class WheelHubBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(type, pos, state), BlockEntityPhysicsListener, SpecialBlockEntityItemRequirement {
    var wheel: WheelSize? = null
        private set
    var tireType = TireType.RUBBER
        private set
    var bodyId = -1L
        private set
    var engaged = true
        private set
    lateinit var torque: ScrollValueBehaviour
    lateinit var tireWidth: ScrollValueBehaviour
    lateinit var tireThickness: ScrollValueBehaviour
    lateinit var tirePressure: ScrollValueBehaviour
    val tire: WheelTire? get() = wheel?.tire(tireWidth.value * 5, tireThickness.value * 5, tirePressure.value * 5, tireType)
    val facing: Direction get() = blockState.getValue(BlockStateProperties.FACING)
    @Volatile override lateinit var dimension: DimensionId
    @Volatile private var drive: Drive? = null
    @Volatile private var jointId = -1
    private var owner: BlockPos? = null
    private var parentId: Long? = null
    private var bodyDimension: String? = null
    private var appliedJoint: VSRevoluteJoint? = null
    private var collisionParent: Long? = null
    private val stepAssist = WheelStepAssist()

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        torque = ScrollValueBehaviour(Component.translatable("block.vs_clockwork.wheel_hub.torque"), this, SettingSlot(0))
            .between(1, 256).withFormatter { "${it * 1000} Nm" }.requiresWrench()
            .withCallback { refreshStress() }
        torque.setValue(8)
        behaviours.add(torque)
        tireWidth = TireSetting(this, 1, "width", "WheelWidth", 10, 40, 20)
            .withFormatter { length((wheel ?: WheelSize.MEDIUM).width * it / 20.0) }.requiresWrench()
        tireThickness = TireSetting(this, 2, "thickness", "WheelThickness", 5, WheelSize.MEDIUM.maximumThicknessPercent / 5, 20)
            .withFormatter { val size = wheel ?: WheelSize.MEDIUM; length((size.radius - size.rimRadius) * it / 20.0) }.requiresWrench()
        tirePressure = TireSetting(this, 3, "pressure", "WheelPressure", 20, 320, 160)
            .withFormatter { String.format(Locale.ROOT, "%.2f bar", it / 20.0) }.requiresWrench()
        behaviours.addAll(listOf(tireWidth, tireThickness, tirePressure))
        updateTireRanges()
    }

    private fun updateTireRanges() {
        if (!::tireWidth.isInitialized) return
        val minimum = (wheel?.minimumWidthPercent ?: 50) / 5
        tireWidth.between(minimum, 40)
        tireWidth.value = tireWidth.value.coerceIn(minimum, 40)
        val maximum = (wheel ?: WheelSize.MEDIUM).maximumThicknessPercent / 5
        tireThickness.between(5, maximum)
        tireThickness.value = tireThickness.value.coerceIn(5, maximum)
    }

    fun install(size: WheelSize, player: Player, settings: CompoundTag? = null, type: TireType = TireType.RUBBER): Boolean {
        if (wheel != null) return false
        if (!supportsTires()) {
            player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub.unsupported"), true)
            return false
        }
        if (level !is ServerLevel) return false
        val width = (settings?.takeIf { it.contains("WheelWidth") }?.getInt("WheelWidth") ?: tireWidth.value)
            .coerceIn(size.minimumWidthPercent / 5, 40)
        val thickness = (settings?.takeIf { it.contains("WheelThickness") }?.getInt("WheelThickness") ?: tireThickness.value)
            .coerceIn(5, size.maximumThicknessPercent / 5)
        val pressure = settings?.takeIf { it.contains("WheelPressure") }?.getInt("WheelPressure")?.coerceIn(20, 320) ?: tirePressure.value
        wheel = size
        tireType = type
        tireWidth.value = width
        tireThickness.value = thickness
        tirePressure.value = pressure
        updateTireRanges()
        refreshStress()
        setChanged()
        sendData()
        AllSoundEvents.WRENCH_ROTATE.playOnServer(level, blockPos)
        return true
    }

    fun toggleDrive(player: Player) {
        engaged = !engaged
        refreshStress()
        setChanged()
        sendData()
        player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub." +
            if (engaged) "engaged" else "freewheel"), true)
    }

    fun uninstall(player: Player?) {
        val size = wheel ?: return
        removeBody()
        wheel = null
        updateTireRanges()
        refreshStress()
        if (player?.isCreative != true) {
            val stack = ItemStack(WheelHubBlock.item(size, tireType))
            if (tireWidth.value != 20 || tireThickness.value != 20 || tirePressure.value != 160) stack.orCreateTag.apply {
                putInt("WheelWidth", tireWidth.value)
                putInt("WheelThickness", tireThickness.value)
                putInt("WheelPressure", tirePressure.value)
            }
            if (player == null || !player.inventory.add(stack)) Block.popResource(level, blockPos, stack)
        }
        setChanged()
        sendData()
        AllSoundEvents.WRENCH_REMOVE.playOnServer(level, blockPos)
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        dimension = world.dimensionId
        val size = tire ?: return
        if (!supportsTires()) { drive = null; return }
        val parent = world.getShipManagingPos(blockPos)
        val rotation = WheelSize.axleRotation(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())
        val center = Vector3d(blockPos.x + .5, blockPos.y + .5, blockPos.z + .5)
        val scale = parent?.transform?.scaling?.x() ?: 1.0
        var body = world.shipObjectWorld.allBodies.getById(bodyId)
        if (body != null && (body.dimension != dimension || body.collisionShape !is WheelBodyShapeData ||
                bodyDimension != dimension || copiedFromLiveHub(world))) {
            body = null
            bodyId = -1
            jointId = -1
        }
        if (body == null) {
            val position = parent?.transform?.shipToWorld?.transformPosition(center, Vector3d()) ?: Vector3d(center)
            val worldRotation = parent?.transform?.shipToWorldRotation?.mul(rotation, Quaterniond()) ?: Quaterniond(rotation)
            val velocity = parent?.let { Vector3d(it.angularVelocity)
                .cross(Vector3d(position).sub(it.kinematics.position)).add(it.velocity) } ?: Vector3d()
            body = world.shipObjectWorld.createBody(VsBodyCreateData(
                dimensionId = dimension,
                inertiaData = vsCore.newShipInertiaData(Vector3d(), size.mass, size.inertia()),
                kinematics = vsCore.newBodyKinematics(velocity, parent?.angularVelocity ?: Vector3d(),
                    position, worldRotation, Vector3d(scale), Vector3d()),
                collisionShape = size.shape(), staticFrictionCoefficient = size.type.physics.staticFriction,
                dynamicFrictionCoefficient = size.type.physics.dynamicFriction, restitutionCoefficient = size.type.physics.restitution,
            ))
            bodyId = body.id
            owner = blockPos
            parentId = parent?.id
            bodyDimension = dimension
            setChanged()
            sendData()
        } else if (owner != blockPos || parentId != parent?.id) {
            moveBody(body, center, rotation, scale)
            owner = blockPos
            parentId = parent?.id
            setChanged()
        }
        val shape = size.shape()
        if (body.collisionShape != shape) {
            body.setWheelShape(shape, vsCore.newShipInertiaData(Vector3d(), size.mass, size.inertia()))
        }
        if (body.isStatic) body.isStatic = false
        val rpm = if (engaged && !isOverStressed) getSpeed() * facing.axisDirection.step else null
        val config = ClockworkConfig.SERVER
        WheelContacts.watch(world, bodyId, size.width, config.wheelStepAssist && tireType.stepAssist, tireType)
        val assist = if (config.wheelStepAssist && tireType.stepAssist) WheelStepAssist.Settings(config.wheelStepAssistHeight,
            config.wheelStepAssistStrength, config.wheelStepAssistMaxForce) else null
        drive = Drive(bodyId, parent?.id, VSJointPose(center, rotation), rpm, torque.value * 1000f, size, assist)
    }

    private fun copiedFromLiveHub(world: ServerLevel): Boolean {
        val old = owner ?: return true
        if (old == blockPos) return false
        if (!world.hasChunkAt(old)) return true
        return (world.getBlockEntity(old) as? WheelHubBlockEntity)?.bodyId == bodyId
    }

    private fun moveBody(body: ServerVsBody, center: Vector3d, rotation: Quaterniond, scale: Double) {
        val parent = level?.getShipManagingPos(blockPos)
        body.setTransform(parent?.transform?.shipToWorld?.transformPosition(center, Vector3d()) ?: center,
            parent?.transform?.shipToWorldRotation?.mul(rotation, Quaterniond()) ?: rotation, Vector3d(scale))
    }

    @Synchronized
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        val current = drive ?: return
        if (physShip?.id != current.parentId || physLevel.getBodyById(current.bodyId) == null) return
        val physics = physLevel as VsiPhysLevel
        val next = current.joint()
        if (physics.getJointById(jointId)?.shipId1 != current.bodyId) {
            jointId = physics.getJointsFromShip(current.bodyId).firstOrNull {
                val candidate = physics.getJointById(it)
                candidate is VSRevoluteJoint && candidate.shipId1 == current.bodyId
            } ?: -1
        }
        val existing = physics.getJointById(jointId)
        if (existing?.shipId1 != current.bodyId || existing !is VSRevoluteJoint) {
            jointId = physics.addJoint(next)
            appliedJoint = next
        } else if (existing.shipId0 != next.shipId0 || existing.pose0 != next.pose0) {
            physics.removeJoint(jointId)
            jointId = physics.addJoint(next)
            appliedJoint = next
        } else if (appliedJoint != next || existing != next) {
            physics.updateJoint(jointId, next)
            appliedJoint = next
        }
        if (collisionParent != current.parentId) {
            collisionParent?.let { physics.enableCollisionBetween(it, current.bodyId) }
            current.parentId?.let { physics.disableCollisionBetween(it, current.bodyId) }
            collisionParent = current.parentId
        }
        current.assist?.let { settings ->
            if (physShip != null && !physShip.isStatic)
                physics.getBodyById(current.bodyId)?.let { stepAssist.apply(physics, it, current.tire, current.rpm, current.torque.toDouble(), settings) }
        }
    }

    @Synchronized
    private fun removeBody() {
        drive = null
        val world = level as? ServerLevel ?: return
        movingBodies[world]?.remove(blockPos)
        if (!copiedFromLiveHub(world) && bodyDimension == world.dimensionId) {
            val joint = world.gtpa.getJointById(jointId)
            if (joint?.shipId1 == bodyId) world.gtpa.removeJoint(jointId)
            world.shipObjectWorld.allBodies.getById(bodyId)?.takeIf { it.dimension == world.dimensionId }
                ?.let(world.shipObjectWorld::deleteBody)
        }
        bodyId = -1
        jointId = -1
        appliedJoint = null
        collisionParent = null
        owner = null
        parentId = null
    }

    override fun destroy() {
        if (level is ServerLevel) uninstall(null)
        super.destroy()
    }

    override fun remove() {
        drive = null
        val world = level as? ServerLevel
        if (world != null && owner == blockPos && bodyDimension == world.dimensionId)
            world.shipObjectWorld.allBodies.getById(bodyId)?.isStatic = true
        super.remove()
    }

    override fun calculateStressApplied(): Float {
        lastStressApplied = if (wheel != null && engaged && ::torque.isInitialized) torque.value.toFloat() else 0f
        return lastStressApplied
    }

    private fun refreshStress() {
        if (level?.isClientSide == false && hasNetwork()) orCreateNetwork?.updateStressFor(this, calculateStressApplied())
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        wheel?.let { tag.putString("Wheel", it.id) }
        tag.putString("TireType", tireType.id.toString())
        tag.putBoolean("WheelDrive", engaged)
        tag.putLong("WheelBody", bodyId)
        tag.putInt("WheelJoint", jointId)
        owner?.let { tag.putLong("WheelOwner", it.asLong()) }
        parentId?.let { tag.putLong("WheelParent", it) }
        bodyDimension?.let { tag.putString("WheelDimension", it) }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        if (tag.isEmpty) (level as? ServerLevel)?.let(::prepareMove)
        super.read(tag, clientPacket)
        drive = null
        wheel = WheelSize.byId(tag.getString("Wheel"))
        tireType = TireType.read(tag)
        updateTireRanges()
        engaged = !tag.contains("WheelDrive") || tag.getBoolean("WheelDrive")
        bodyId = if (tag.contains("WheelBody")) tag.getLong("WheelBody") else -1
        jointId = if (tag.contains("WheelJoint")) tag.getInt("WheelJoint") else -1
        owner = if (tag.contains("WheelOwner")) BlockPos.of(tag.getLong("WheelOwner")) else null
        parentId = if (tag.contains("WheelParent")) tag.getLong("WheelParent") else null
        bodyDimension = if (tag.contains("WheelDimension")) tag.getString("WheelDimension") else null
        if (!clientPacket && owner != blockPos) {
            bodyId = -1
            jointId = -1
            owner = null
            parentId = null
            bodyDimension = null
        }
    }

    override fun writeSafe(tag: CompoundTag) {
        super.writeSafe(tag)
        wheel?.let { tag.putString("Wheel", it.id) }
        tag.putString("TireType", tireType.id.toString())
        tag.putBoolean("WheelDrive", engaged)
    }

    override fun getRequiredItems(state: BlockState): ItemRequirement = wheel?.let {
        ItemRequirement(ItemRequirement.ItemUseType.CONSUME, WheelHubBlock.item(it, tireType))
    } ?: ItemRequirement.NONE

    fun prepareMove(world: ServerLevel) {
        if (bodyId >= 0 && owner == blockPos)
            movingBodies.getOrPut(world) { HashMap() }[blockPos] = BodyReference(bodyId, jointId, parentId)
    }

    private data class Drive(val bodyId: Long, val parentId: Long?, val pose: VSJointPose,
                             val rpm: Float?, val torque: Float, val tire: WheelTire, val assist: WheelStepAssist.Settings?) {
        fun joint() = VSRevoluteJoint(parentId, pose, bodyId, VSJointPose(Vector3d(), Quaterniond()),
            compliance = 1e-10,
            driveVelocity = rpm?.let { VSRevoluteJoint.VSRevoluteDriveVelocity((it * Math.PI / 30).toFloat()) },
            driveForceLimit = torque, driveFreeSpin = rpm != 0f)
    }

    private fun configurationError(index: Int, value: Int): String? {
        val world = level as? ServerLevel ?: return null
        val current = tire ?: return null
        if (index == 3) return null
        val next = current.size.tire(if (index == 1) value * 5 else current.widthPercent,
            if (index == 2) value * 5 else current.thicknessPercent, current.pressureKpa, current.type)
        if (next == current) return null
        val body = world.shipObjectWorld.allBodies.getById(bodyId) ?: return null
        if (body.velocity.lengthSquared() > .04 || body.angularVelocity.lengthSquared() > .25) return "moving"
        if (!WheelPlacement.hasClearance(world, blockPos, facing, next)) return "obstructed"
        return null
    }

    private class TireSetting(private val hub: WheelHubBlockEntity, private val index: Int, name: String,
                              private val key: String, private val minimum: Int, maximum: Int, private val initial: Int) :
        ScrollValueBehaviour(Component.translatable("block.vs_clockwork.wheel_hub.$name"), hub, SettingSlot(index)) {
        init { between(minimum, maximum); value = initial }
        override fun getType() = TIRE_SETTINGS[index - 1]
        override fun netId() = index
        override fun getClipboardKey() = key
        private val lowerBound get() = if (index == 1) (hub.wheel?.minimumWidthPercent ?: 50) / 5 else minimum
        override fun write(tag: CompoundTag, clientPacket: Boolean) { tag.putInt(key, value) }
        override fun read(tag: CompoundTag, clientPacket: Boolean) {
            val maximum = if (index == 2) WheelSize.entries.maxOf { it.maximumThicknessPercent } / 5 else max
            value = if (tag.contains(key)) tag.getInt(key).coerceIn(minimum, maximum) else initial
        }
        override fun setValue(value: Int) {
            if (hub.configurationError(index, value.coerceIn(lowerBound, max)) == null) super.setValue(value.coerceAtLeast(lowerBound))
            else hub.sendData()
        }
        override fun setValueSettings(player: Player, settings: ValueSettings, ctrlDown: Boolean) {
            val error = hub.configurationError(index, settings.value().coerceIn(lowerBound, max))
            if (error != null) {
                player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub.$error"), true)
                hub.sendData()
                return
            }
            super.setValueSettings(player, settings, ctrlDown)
        }
        override fun createBoard(player: Player, hitResult: BlockHitResult) =
            ValueSettingsBoard(label, max, if (index == 3) 20 else 5, listOf(Component.translatable("block.vs_clockwork.wheel_hub.setting")),
                ValueSettingsFormatter { setting ->
                    val size = hub.wheel ?: WheelSize.MEDIUM
                    Component.literal(when (index) {
                        1 -> length(size.width * setting.value() / 20.0)
                        2 -> length((size.radius - size.rimRadius) * setting.value() / 20.0)
                        else -> String.format(Locale.ROOT, "%.2f bar", setting.value() / 20.0)
                    })
                })
    }

    private class SettingSlot(private val index: Int) : ValueBoxTransform.Sided() {
        override fun getScale() = .125f
        override fun getSouthLocation() = Vec3(.5, .5, .6875)
        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            val facing = state.getValue(BlockStateProperties.FACING)
            val along = -.4375 + index * .125
            return Vec3(.5 + facing.stepX * along + side.stepX * .193,
                .5 + facing.stepY * along + side.stepY * .193,
                .5 + facing.stepZ * along + side.stepZ * .193)
        }
        override fun isSideActive(state: BlockState, direction: Direction) =
            direction.axis != state.getValue(BlockStateProperties.FACING).axis
    }

    companion object {
        private val TIRE_SETTINGS = listOf("wheel_width", "wheel_thickness", "wheel_pressure").map { BehaviourType<ScrollValueBehaviour>(it) }
        private fun length(value: Double) = String.format(Locale.ROOT, "%.2f m", value)
        private data class BodyReference(val body: Long, val joint: Int, val parent: Long?)
        private val movingBodies = WeakHashMap<ServerLevel, MutableMap<BlockPos, BodyReference>>()

        fun moveBodies(world: ServerLevel, destination: (BlockPos) -> BlockPos?) {
            val pending = movingBodies[world] ?: return
            for ((from, reference) in pending.toMap()) {
                val to = destination(from) ?: continue
                val hub = world.getBlockEntity(to) as? WheelHubBlockEntity ?: continue
                if (hub.wheel == null || world.shipObjectWorld.allBodies.getById(reference.body) == null) continue
                pending.remove(from)
                hub.bodyId = reference.body
                hub.jointId = reference.joint
                hub.owner = from
                hub.parentId = reference.parent
                hub.bodyDimension = world.dimensionId
                hub.setChanged()
                hub.sendData()
            }
        }

        fun supportsTires() = VSCoreConfig.SERVER.physics.physicsBackend == ConfigPhysicsBackendType.KRUNCH_KONSTANT
    }
}
