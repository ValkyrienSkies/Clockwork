package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import kotlin.math.abs
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.api.wheel.TireTypes
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.core.api.bodies.VsBodyDefaults
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

class WheelHubBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(type, pos, state), BlockEntityPhysicsListener, SpecialBlockEntityItemRequirement {
    var wheel: WheelSize? = null
        private set
    var tireType: TireType? = null
        private set
    var wood = Blocks.OAK_PLANKS
        private set
    var bodyId = -1L
        private set
    var engaged = true
        private set
    lateinit var torque: ScrollValueBehaviour
    private var tireWidth = 20
    private var tireThickness = 20
    private var tirePressure = 160
    internal val condition = TireCondition()
    val tire: WheelTire? get() = tireType?.let { type ->
        wheel?.tire(tireWidth * 5, tireThickness * 5, tirePressure * 5, type)?.let {
            if (condition.wearGrade == 0) it else it.copy(wearGrade = condition.wearGrade)
        }
    }
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
    private val interactions = WheelInteractions()
    private val hazards = TireHazards()

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        torque = ScrollValueBehaviour(Component.translatable("block.vs_clockwork.wheel_hub.torque"), this, TorqueSlot())
            .between(1, 256).withFormatter { "${it * 1000} Nm" }.requiresWrench()
            .withCallback { refreshStress() }
        torque.setValue(8)
        behaviours.add(torque)
        behaviours.add(SizeClipboard(this, AirPumpSetting.WIDTH, "WheelWidth", WIDTH_CLIPBOARD))
        behaviours.add(SizeClipboard(this, AirPumpSetting.THICKNESS, "WheelThickness", THICKNESS_CLIPBOARD))
    }

    private fun readTireSettings(tag: CompoundTag?, size: WheelSize, width: Int, thickness: Int, pressure: Int) {
        tireWidth = (tag?.takeIf { it.contains("WheelWidth") }?.getInt("WheelWidth") ?: width)
            .coerceIn(size.minimumWidthPercent / 5, 40)
        tireThickness = (tag?.takeIf { it.contains("WheelThickness") }?.getInt("WheelThickness") ?: thickness)
            .coerceIn(5, size.maximumThicknessPercent / 5)
        tirePressure = (tag?.takeIf { it.contains("WheelPressure") }?.getInt("WheelPressure") ?: pressure).coerceIn(0, 320)
    }

    private fun writeTireSettings(tag: CompoundTag) {
        tag.putInt("WheelWidth", tireWidth)
        tag.putInt("WheelThickness", tireThickness)
        tag.putInt("WheelPressure", tirePressure)
    }

    fun install(size: WheelSize, player: Player, settings: CompoundTag? = null, type: TireType? = null,
                replace: Boolean = false): Boolean {
        if (wheel != null && !replace) return false
        if (!supportsTires()) {
            player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub.unsupported"), true)
            return false
        }
        if (level !is ServerLevel) return false
        val replacing = wheel != null
        if (replacing) uninstall(player)
        wheel = size
        tireType = type
        wood = WheelItem.wood(settings)
        if (type?.driven == false) engaged = false
        readTireSettings(settings, size, tireWidth, tireThickness, tirePressure)
        condition.read(settings, type)
        refreshStress()
        setChanged()
        sendData()
        if (!replacing) AllSoundEvents.WRENCH_ROTATE.playOnServer(level, blockPos)
        return true
    }

    fun toggleDrive(player: Player) {
        if (wheel == null) return
        if (tireType?.driven == false) {
            player.displayClientMessage(Component.translatable(if (tireType == TireType.WOOD) "block.vs_clockwork.wheel_hub.wood_freewheel" else "block.vs_clockwork.wheel_hub.passive_tire"), true)
            return
        }
        engaged = !engaged
        refreshStress()
        setChanged()
        sendData()
        player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub." +
            if (engaged) "engaged" else "freewheel"), true)
    }

    fun fitTire(type: TireType, player: Player, replace: Boolean, itemData: CompoundTag? = null): Boolean {
        if (level !is ServerLevel || wheel == null || type == TireType.WOOD || tireType == TireType.WOOD ||
            (tireType != null && !replace)) return false
        if (!supportsTires()) {
            player.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub.unsupported"), true)
            return false
        }
        if (tireType != null) removeTire(player) else if (bodyId >= 0) removeBody()
        tireType = type
        condition.read(itemData, type)
        if (itemData?.contains("WheelPressure") == true) tirePressure = itemData.getInt("WheelPressure").coerceIn(0, 320)
        if (!type.driven) engaged = false
        refreshStress()
        setChanged()
        sendData()
        ClockworkSounds.TIRE_ATTACH.playOnServer(level, blockPos)
        return true
    }

    private fun returnPart(stack: ItemStack, player: Player?) {
        if (player?.isCreative != true && (player == null || !player.inventory.add(stack)))
            Block.popResource(level, blockPos, stack)
    }

    fun removeTire(player: Player?) {
        val type = tireType ?: return
        if (type == TireType.WOOD || level !is ServerLevel) return
        removeBody()
        tireType = null
        val removed = ItemStack(TireTypes.item(type))
        val tag = CompoundTag()
        condition.write(tag)
        if (!tag.isEmpty || tirePressure != 160) tag.putInt("WheelPressure", tirePressure)
        if (!tag.isEmpty) removed.tag = tag
        returnPart(removed, player)
        condition.read(null, null)
        tirePressure = 160
        refreshStress()
        setChanged()
        sendData()
        ClockworkSounds.TIRE_REMOVE.playOnServer(level, blockPos)
    }

    fun uninstall(player: Player?) {
        val size = wheel ?: return
        if (level !is ServerLevel) return
        removeTire(player)
        val stack = ItemStack(WheelHubBlock.item(size, tireType ?: TireType.RUBBER))
        stack.orCreateTag.apply {
            writeTireSettings(this)
            if (tireType == TireType.WOOD) putString("WheelWood", BuiltInRegistries.BLOCK.getKey(wood).toString())
        }
        if (bodyId >= 0) removeBody()
        wheel = null
        tireType = null
        refreshStress()
        returnPart(stack, player)
        setChanged()
        sendData()
        ClockworkSounds.WHEEL_UNSCREW.playOnServer(level, blockPos)
    }

    override fun tick() {
        super.tick()
        val rim = wheel ?: return
        if (level?.isClientSide == true) {
            if (condition.leaks > 0 && tirePressure > 0 && bodyId >= 0) WheelEffects.trackLeak(this)
            if (tireType == null && bodyId >= 0) WheelEffects.sampleRim(this, rim)
            return
        }
        val world = level as? ServerLevel ?: return
        dimension = world.dimensionId
        if (condition.leaks > 0 && tirePressure > 0 && (world.gameTime + blockPos.asLong()) % 20 == 0L) {
            tirePressure = (tirePressure - condition.leaks).coerceAtLeast(0)
            setChanged()
            sendData()
        }
        val tire = tire
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
        val config = ClockworkConfig.SERVER.wheel
        val assistMode = tire?.type?.stepAssist(config) ?: TireType.StepAssist.NONE
        val shape = tire?.shape(assistMode) ?: WheelBodyShapeData(rim.rimRadius, rim.rimWidth / 2)
        fun inertia() = vsCore.newShipInertiaData(Vector3d(), tire?.mass ?: rim.rimMass, tire?.inertia() ?: rim.rimInertia())
        if (body == null) {
            val position = parent?.transform?.shipToWorld?.transformPosition(center, Vector3d()) ?: Vector3d(center)
            val worldRotation = parent?.transform?.shipToWorldRotation?.mul(rotation, Quaterniond()) ?: Quaterniond(rotation)
            val velocity = parent?.let { Vector3d(it.angularVelocity)
                .cross(Vector3d(position).sub(it.kinematics.position)).add(it.velocity) } ?: Vector3d()
            body = world.shipObjectWorld.createBody(VsBodyCreateData(
                dimensionId = dimension,
                inertiaData = inertia(),
                kinematics = vsCore.newBodyKinematics(velocity, parent?.angularVelocity ?: Vector3d(),
                    position, worldRotation, Vector3d(scale), Vector3d()),
                collisionShape = shape,
                staticFrictionCoefficient = tire?.type?.physics?.staticFriction ?: VsBodyDefaults.DEFAULT_STATIC_FRICTION_COEFFICIENT,
                dynamicFrictionCoefficient = tire?.type?.physics?.dynamicFriction ?: VsBodyDefaults.DEFAULT_DYNAMIC_FRICTION_COEFFICIENT,
                restitutionCoefficient = tire?.type?.physics?.restitution ?: VsBodyDefaults.DEFAULT_RESTITUTION_COEFFICIENT,
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
        if (body.collisionShape != shape) {
            body.setWheelShape(shape, inertia())
        }
        if (body.isStatic) body.isStatic = false
        var rpm = if (engaged && tire?.type?.driven != false && !isOverStressed) getSpeed() * facing.axisDirection.step else null
        var axleTorque = torque.value * 1000f
        if (DrivingControlLinks.get(world).braked(world, blockPos)) {
            rpm = 0f
            axleTorque = maxOf(axleTorque, 256_000f)
        } else if (rpm != null && rpm != 0f) {
            val throttle = DrivingControlLinks.get(world).throttle(world, blockPos)
            if (throttle != null) {
                axleTorque *= abs(throttle)
                if (throttle == 0f) rpm = null
                else {
                    val axle = body.kinematics.rotation.transform(Vector3d(1.0, 0.0, 0.0))
                    val spin = body.angularVelocity.dot(axle) - (parent?.angularVelocity?.dot(axle) ?: 0.0)
                    val target = if (throttle > 0f) rpm else -rpm
                    rpm = if (spin * Math.signum(target) < -.5) 0f else target
                }
            }
        }
        val raycastAssist = assistMode == TireType.StepAssist.RAYCAST
        if (tire != null) {
            if (tire.type.maintenance.canPuncture) {
                hazards.check(world, body, tire, ::puncture)
            }
            interactions.tick(world, body, tire, parent?.id)
            WheelContacts.watch(world, bodyId, tire.width, raycastAssist, tire.type, blockPos)
        }
        val assist = if (raycastAssist) WheelStepAssist.Settings(config.wheelStepAssistHeight,
            config.wheelStepAssistStrength, config.wheelStepAssistMaxForce) else null
        drive = Drive(bodyId, parent?.id, VSJointPose(center, rotation), rpm, axleTorque, tire, assist)
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
        val tire = current.tire ?: return
        if (tire.type.floating) physics.getBodyById(current.bodyId)?.let {
            interactions.applyWater(physics, it, tire)
        }
        current.assist?.let { settings ->
            if (physShip != null && !physShip.isStatic)
                physics.getBodyById(current.bodyId)?.let { stepAssist.apply(physics, it, tire, current.rpm, current.torque.toDouble(), settings) }
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

    override fun initialize() {
        super.initialize()
        if (level?.isClientSide == true) WheelEffects.indexHub(this)
    }

    override fun remove() {
        if (level?.isClientSide == true) WheelEffects.forgetHub(this)
        drive = null
        val world = level as? ServerLevel
        if (world != null && owner == blockPos && bodyDimension == world.dimensionId)
            world.shipObjectWorld.allBodies.getById(bodyId)?.isStatic = true
        super.remove()
    }

    override fun calculateStressApplied(): Float {
        lastStressApplied = if (wheel != null && tireType?.driven != false && engaged && ::torque.isInitialized) torque.value.toFloat() else 0f
        return lastStressApplied
    }

    private fun refreshStress() {
        if (level?.isClientSide == false && hasNetwork()) orCreateNetwork?.updateStressFor(this, calculateStressApplied())
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        writeWheel(tag)
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
        tireType = wheel?.let { TireType.read(tag) }
        wood = WheelItem.wood(tag)
        readTireSettings(tag, wheel ?: WheelSize.MEDIUM, 20, 20, 160)
        condition.read(tag, tireType)
        engaged = tireType?.driven != false && (!tag.contains("WheelDrive") || tag.getBoolean("WheelDrive"))
        if (level?.isClientSide == true) WheelEffects.forgetHub(this)
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
        if (level?.isClientSide == true) WheelEffects.indexHub(this)
    }

    private fun writeWheel(tag: CompoundTag) {
        writeTireSettings(tag)
        condition.write(tag)
        wheel?.let { tag.putString("Wheel", it.id) }
        val type = tireType
        if (type != null) tag.putString("TireType", type.id.toString())
        else if (wheel != null) tag.putBoolean("BareWheel", true)
        if (type == TireType.WOOD) tag.putString("WheelWood", BuiltInRegistries.BLOCK.getKey(wood).toString())
        tag.putBoolean("WheelDrive", engaged)
    }

    override fun writeSafe(tag: CompoundTag) {
        super.writeSafe(tag)
        writeWheel(tag)
    }

    override fun getRequiredItems(state: BlockState): ItemRequirement = wheel?.let {
        WheelHubBlock.parts(it, tireType).map { part -> ItemRequirement(ItemRequirement.ItemUseType.CONSUME, part) }
            .reduce(ItemRequirement::union)
    } ?: ItemRequirement.NONE

    fun prepareMove(world: ServerLevel) {
        if (bodyId >= 0 && owner == blockPos)
            movingBodies.getOrPut(world) { HashMap() }[blockPos] = BodyReference(bodyId, jointId, parentId)
    }

    private data class Drive(val bodyId: Long, val parentId: Long?, val pose: VSJointPose,
                             val rpm: Float?, val torque: Float, val tire: WheelTire?, val assist: WheelStepAssist.Settings?) {
        fun joint() = VSRevoluteJoint(parentId, pose, bodyId, VSJointPose(Vector3d(), Quaterniond()),
            compliance = 1e-10,
            driveVelocity = rpm?.let { VSRevoluteJoint.VSRevoluteDriveVelocity((it * Math.PI / 30).toFloat()) },
            driveForceLimit = torque, driveFreeSpin = rpm != 0f)
    }

    fun setTireSetting(setting: AirPumpSetting, value: Int, player: Player? = null): Boolean {
        val world = level as? ServerLevel ?: return false
        val current = tire ?: return false
        val next = value.coerceIn(setting.range(current))
        if (next == setting.value(current)) {
            player?.displayClientMessage(readout(setting, current), true)
            return false
        }
        if (setting != AirPumpSetting.PRESSURE) {
            val body = world.shipObjectWorld.allBodies.getById(bodyId)
            if (body != null && (body.velocity.lengthSquared() > .04 || body.angularVelocity.lengthSquared() > .25)) {
                player?.displayClientMessage(Component.translatable("block.vs_clockwork.wheel_hub.moving"), true)
                return false
            }
        }
        when (setting) {
            AirPumpSetting.PRESSURE -> tirePressure = next
            AirPumpSetting.WIDTH -> tireWidth = next
            AirPumpSetting.THICKNESS -> tireThickness = next
        }
        setChanged()
        sendData()
        player?.displayClientMessage(readout(setting, tire!!), true)
        return true
    }

    private class SizeClipboard(private val hub: WheelHubBlockEntity, private val setting: AirPumpSetting,
                                private val key: String, private val behaviourType: BehaviourType<SizeClipboard>) :
        BlockEntityBehaviour(hub), ClipboardCloneable {
        override fun getType() = behaviourType
        override fun getClipboardKey() = key

        override fun writeToClipboard(tag: CompoundTag, side: Direction): Boolean {
            val tire = hub.tire ?: return false
            tag.putInt("Row", 0)
            tag.putInt("Value", setting.value(tire))
            return true
        }

        override fun readFromClipboard(tag: CompoundTag, player: Player, side: Direction, simulate: Boolean): Boolean {
            val tire = hub.tire ?: return false
            if (!tag.contains("Value", Tag.TAG_INT.toInt()) || !player.mayBuild()) return false
            val value = tag.getInt("Value")
            return simulate || setting.value(tire) == value || hub.setTireSetting(setting, value, player)
        }
    }

    private fun readout(setting: AirPumpSetting, tire: WheelTire): Component {
        val value = setting.readout(tire)
        return if (condition.wear >= .01 || condition.leaks > 0) value.copy().append(" · ").append(condition.status()) else value
    }

    fun puncture(severity: Double, hit: Vector3d? = null) {
        val current = tire ?: return
        if (level !is ServerLevel) return
        val before = condition.leaks
        if (!condition.puncture(current, severity, hit)) return
        if (condition.leaks > before && tirePressure > 0)
            ClockworkSounds.TIRE_AIR_RELEASE.playOnServer(level, blockPos)
        setChanged()
        sendData()
    }

    internal fun applyTreadWear(type: TireType, contact: TireSurfaceContact) {
        val current = tire ?: return
        if (current.type != type) return
        val grade = condition.wearGrade
        val distance = (contact.sliding - .4).coerceIn(0.0, 50.0) * CONTACT_INTERVAL / 20.0
        if (condition.abrade(current, distance, if (contact.material.loose || contact.material == WheelSurface.ICE) .25 else 1.0)) {
            setChanged()
            if (condition.wearGrade != grade) sendData()
        }
    }

    fun repairTire(stack: ItemStack, player: Player): Boolean {
        val type = tireType ?: return false
        if (!condition.canRepair(stack, type)) return false
        if (level?.isClientSide == true) return true
        condition.repair(stack, type)
        if (!player.isCreative) stack.shrink(1)
        ClockworkSounds.TIRE_ATTACH.playOnServer(level, blockPos)
        player.displayClientMessage(condition.status(), true)
        setChanged()
        sendData()
        return true
    }

    private class TorqueSlot : ValueBoxTransform.Sided() {
        override fun getScale() = .125f
        override fun getSouthLocation() = Vec3(.5, .5, .6875)
        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            val facing = state.getValue(BlockStateProperties.FACING)
            val along = -.25
            return Vec3(.5 + facing.stepX * along + side.stepX * .193,
                .5 + facing.stepY * along + side.stepY * .193,
                .5 + facing.stepZ * along + side.stepZ * .193)
        }
        override fun isSideActive(state: BlockState, direction: Direction) =
            direction.axis != state.getValue(BlockStateProperties.FACING).axis
    }

    companion object {
        private val WIDTH_CLIPBOARD = BehaviourType<SizeClipboard>("wheel_width")
        private val THICKNESS_CLIPBOARD = BehaviourType<SizeClipboard>("wheel_thickness")
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
