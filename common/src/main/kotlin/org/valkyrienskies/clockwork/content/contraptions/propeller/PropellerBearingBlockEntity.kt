package org.valkyrienskies.clockwork.content.contraptions.propeller

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.createmod.catnip.lang.Lang
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.core.Direction.AxisDirection.POSITIVE
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.copter.CopterBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.clockwork.util.sound.PropellerSoundInstance
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

open class PropellerBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState, val brass: Boolean = false) : KineticBlockEntity(type, pos, state), IBearingBlockEntity, IForceApplierBE<PropUpdateData, PropData, PropCreateData, PropellerController> {

    var sailPositions: MutableList<Vector3ic> = ArrayList()
    var blades: MutableList<BladeData> = ArrayList()
    override var physID: Int = -1

    var propellerContraption: ControlledContraptionEntity? = null

    @Environment(value = EnvType.CLIENT)
    var soundInstance: PropellerSoundInstance? = null

    var targetOmega = 0.0
    var currentOmega = 0.0
    var previousOmega = 0.0

    var angle = 0.0
    var previousAngle = 0.0

    var active = false
    @Volatile
    var running = false

    var starting = false
    var startingProgress = 0.0

    @Volatile
    var stopping = false
    var disassemblyProgress = 0.0
    var totalDisassemblyTime = 0.0

    var assembleNextTick = false
    var assembleCooldown = 0

    var clientAngleDiff = 0.0f

    var lastException: AssemblyException? = null

    lateinit var rotationDirection: ScrollOptionBehaviour<RotationDirection>

    var powerOne = 0
    var powerTwo = 0
    var lastPowerOne = 0
    var lastPowerTwo = 0

    override fun newCreateData(): PropCreateData {
        return PropCreateData(worldPosition.toJOML(), blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD(), angle, currentOmega, ArrayList(sailPositions), isInverted(), active, brass && blades.isEmpty(), ArrayList(blades))
    }

    override fun newUpdateData(): PropUpdateData {
        return PropUpdateData(currentOmega, angle, isInverted(), active, ArrayList(blades), lastStressApplied = lastStressApplied)
    }

    override fun invalidate() {
        if (level != null && level!!.isClientSide) clientInvalidate()
        super.invalidate()
    }

    fun clientInvalidate() {
        soundInstance?.stopNow()
        soundInstance = null
    }

    fun shutDown() {
        if (assembleCooldown > 0) return
        this.assembleCooldown = 10
        this.active = false
        this.stopping = true
        this.disassemblyProgress = currentOmega.absoluteValue
        this.totalDisassemblyTime = disassemblyProgress
        setChanged()
        if (!level!!.isClientSide) ClockworkSounds.PROPELLER_STOP.playOnServer(level, worldPosition, 0.75f)
        sendData()
    }

    fun getSails() {
        sailPositions = ArrayList()
        if (propellerContraption != null) {
            val blocks = propellerContraption!!.contraption.blocks
            for ((key, value) in blocks) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(value.state)) {
                    //println("Found sail at ${key}")
                    sailPositions.add(key.toJOML())
                }
            }
        }
    }

    fun getBlades() {
        if (propellerContraption != null) {
            val blocks = propellerContraption!!.contraption.blocks
            for ((key, value) in blocks) {
                if (value.state.`is`(ClockworkBlocks.BLADE_CONTROLLER.get())) {
                    val shouldUpdate = true
                    if (shouldUpdate && value.nbt != null) {
                        value.nbt!!.putBoolean("ShouldUpdatePhys", false)
                        blades = BladeData.fromTag(value.nbt!!)
                    }
                }
            }
        }
    }

    fun setNewBladeAngle(bladeAngle: Double) {
        if (propellerContraption != null) {
            val blocks = propellerContraption!!.contraption.blocks
            for ((key, value) in blocks) {
                if (value.state.`is`(ClockworkBlocks.BLADE_CONTROLLER.get())) {
                    value.nbt?.putDouble("BladeAngle", bladeAngle)
                    break
                }
            }
        }
    }

    override fun tickAudio() {
        if (level == null || !level!!.isClientSide) return
        if (this.active && !this.stopping && this.getAngularSpeed() > 2.0) {
            val pitch = Mth.clamp((backFromAngular(this.currentOmega.absoluteValue).toFloat() / 256f) + .45f, .85f, 1f)
            if (!this.brass) {
                val scape = ClockworkSoundScapes.AmbienceGroup.RICKETY
                ClockworkSoundScapes.play(scape, this.worldPosition, pitch)
            }
            val sound = if (this.brass) ClockworkSounds.PROPELLER.mainEvent else ClockworkSounds.JUNK_PROPELLER.mainEvent
            if (sound != null) {
                if (this.soundInstance == null || this.soundInstance!!.isStopped) {
                    this.soundInstance = PropellerSoundInstance(this, level!!.random)
                    Minecraft.getInstance().soundManager.play(this.soundInstance!!)
                }
            }
        }
    }

    private fun backFromAngular(angular: Double): Double {
        return angular / 3.0 * 10.0
    }

    override fun tick() {
        super.tick()
        if (level == null) return
        if (level!!.isClientSide) {
            clientAngleDiff /= 2f
        } else {
            if (assembleNextTick && assembleCooldown == 0) {
                assembleNextTick = false
                assembleCooldown = 10
                assemble()
            }
            if (assembleCooldown > 0) {
                assembleCooldown--
            }
        }
        if (!running) return

        if (starting) startingProgress++

        if (startingProgress >= targetOmega) {
            starting = false
            startingProgress = 0.0
        }

        lastPowerOne = powerOne
        lastPowerTwo = powerTwo
        val power = getPower()
        powerOne = power.first
        powerTwo = power.second

        previousAngle = angle
        previousOmega = currentOmega
        if (!stopping) {
            val stalled = propellerContraption?.isStalled ?: true
            active = !overStressed && !stalled
            updateSpinDir(currentOmega < 0)
            val lastTargetOmega = targetOmega
            targetOmega = convertToAngular(this.getSpeed()).toDouble() * if (isInverted()) -1.0 else 1.0

            if (lastTargetOmega != targetOmega) {
                sendData()
            }
            currentOmega += Mth.clamp((targetOmega - currentOmega) / 10.0 / (if (starting) (targetOmega.absoluteValue + 1.0 - min(startingProgress.toDouble(), targetOmega.absoluteValue)) else 1.0), convertToAngular(-32f).toDouble(), convertToAngular(32f).toDouble())
        } else {
            active = false
            disassemblyProgress--
            if (disassemblyProgress <= 0) {
                if (!level!!.isClientSide) {
                    propellerContraption?.contraption?.stop(level!!)
                    disassemble()
                }
                stopping = false
            } else {
                val stoppingPoint = angle + currentOmega * disassemblyProgress * 0.5
                val optimalStoppingPoint = 90.0 * Math.round(stoppingPoint / 90.0)
                val Q = (optimalStoppingPoint - stoppingPoint) / disassemblyProgress
                currentOmega = (currentOmega + 6.0 * Q / disassemblyProgress) * (1.0 - 1.0 / disassemblyProgress)
            }
        }

        if (!level!!.isClientSide && level!!.getShipObjectManagingPos(blockPos) != null) {
            val shipOn = (level as ServerLevel).getShipObjectManagingPos(blockPos)!!
            val attachment = PropellerController.getOrCreate(shipOn)!!
            getBlades()
            tickData(attachment, true)
        }

        if (propellerContraption != null && running) {
            var angularSpeed = getAngularSpeed().toFloat()
            val newAngle = (angle + angularSpeed).toFloat()
            angle = (newAngle % 360).toDouble()
            applyPowerEffect()

        }

        applyRotation()
    }
    // reminder: override this for copter bearing since their redstone controls something different
    open fun applyPowerEffect() {
        if (!this.brass || blades.isEmpty()) return

        val powerEffect = Mth.clamp((powerOne + powerTwo).toFloat() / 30f, -1f, 1f)
        val angleChange = 2f * powerEffect

        val currentAngle = blades[0].angle
        val newAngle = (currentAngle + angleChange) % 360f

        setNewBladeAngle(newAngle.toDouble())
        //update our blades
        blades.clear()
        getBlades()
    }

    fun getAngularSpeed(): Double {
        var speed = currentOmega
        if (level!!.isClientSide) {
            speed *= ServerSpeedProvider.get()
            speed += clientAngleDiff / 3.0
        }
        return speed
    }


    override fun remove() {
        if (!level!!.isClientSide) {
            disassemble()
        }
        super.remove()
    }

    fun updateSpinDir(negativeSpeed: Boolean) {
        if ((!isInverted() && negativeSpeed) || (isInverted() && !negativeSpeed)) {
            if (blockState.getValue(PropellerBearingBlock.SPIN_DIRECTION) == PropellerBearingBlock.SpinDirection.PUSH) return

            level!!.setBlockAndUpdate(worldPosition, blockState.setValue(PropellerBearingBlock.SPIN_DIRECTION, PropellerBearingBlock.SpinDirection.PUSH))
        } else if ((!isInverted() && !negativeSpeed) || (isInverted() && negativeSpeed)) {
            if (blockState.getValue(PropellerBearingBlock.SPIN_DIRECTION) == PropellerBearingBlock.SpinDirection.PULL) return

            level!!.setBlockAndUpdate(worldPosition, blockState.setValue(PropellerBearingBlock.SPIN_DIRECTION, PropellerBearingBlock.SpinDirection.PULL))
        }
    }

    override fun tickData(attachment: PropellerController, shouldUpdate: Boolean) {
        if (running && propellerContraption != null) super.tickData(attachment, shouldUpdate) else removeApplier(PropellerController::class.java, level, worldPosition)
    }

    open fun assemble() {
        if (level!!.getBlockState(worldPosition)
                .block !is PropellerBearingBlock
            && level!!.getBlockState(worldPosition).block !is CopterBearingBlock) return
        val direction = blockState.getValue(BlockStateProperties.FACING)
        val contraption: PropellerContraption
        try {
            contraption = PropellerContraption.assembleProp(level!!, worldPosition, direction, brass)!!
            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            sendData()
            return
        }
        if (contraption.blocks.isEmpty()) return
        val anchor = worldPosition.relative(direction)
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        propellerContraption = createContraptionEntity(contraption)
        propellerContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        propellerContraption!!.rotationAxis = direction.axis
        propellerContraption?.let { level!!.addFreshEntity(it) }
        ClockworkSounds.PROPELLER_START.playOnServer(level, worldPosition)
        running = true
        starting = true
        stopping = false
        totalDisassemblyTime = 0.0
        startingProgress = 0.0
        angle = 0.0
        currentOmega = 0.0

        targetOmega = convertToAngular(this.getSpeed()).toDouble() * if (isInverted()) -1.0 else 1.0

        getBlades()
        if (brass && blades.isEmpty()) {
            getSails()
        }

        val stressImpact = calculateStressApplied()
        orCreateNetwork?.updateStressFor(this, stressImpact)

        if (!level!!.isClientSide) {
            val ship = (level as ServerLevel).getShipObjectManagingPos(
                blockPos
            )
            if (ship != null) {
                tickData(PropellerController.getOrCreate(ship)!!, true)
            }
        }
        sendData()
    }

    open fun createContraptionEntity(contraption: PropellerContraption): ControlledContraptionEntity {
        return ControlledContraptionEntity.create(level, this, contraption)
    }

    open fun disassemble() {
        if (!running && propellerContraption == null) return

        targetOmega = 0.0
        currentOmega = 0.0
        angle = 0.0
        stopping = false
        starting = false
        startingProgress = 0.0
        disassemblyProgress = 0.0
        if (propellerContraption != null) {
            propellerContraption!!.disassemble()
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
        }
        propellerContraption = null
        running = false
        if (physID != -1) {
            removeApplier(PropellerController::class.java, level, worldPosition)
        }

        // Remove stress impact
        val stressImpact = calculateStressApplied()
        orCreateNetwork?.updateStressFor(this, stressImpact)

        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour?>) {
        super.addBehaviours(behaviours)
        rotationDirection = ScrollOptionBehaviour(
            RotationDirection::class.java,
            ClockworkLang.translateDirect("contraptions.propeller.rotation_direction"), this,
            movementModeSlot
        )
        behaviours.add(rotationDirection)
    }

    enum class RotationDirection(private val icon: AllIcons) : INamedIconOptions {
        NORMAL(AllIcons.I_REFRESH),
        INVERTED(AllIcons.I_ROTATE_CCW);

        private val translationKey: String = "propeller.rotation_direction." + Lang.asId(name)

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }

    fun isInverted(): Boolean {
        return rotationDirection.get() == RotationDirection.INVERTED
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean {
        return if (contraption.contraption !is PropellerContraption) false else propellerContraption === contraption
    }

    override fun attach(contraption: ControlledContraptionEntity?) {
        val blockState = blockState
        if (contraption!!.contraption !is PropellerContraption) return
        if (!blockState.hasProperty(BearingBlock.FACING)) return

        this.propellerContraption = contraption
        setChanged()
        val anchor = worldPosition.relative(blockState.getValue(BlockStateProperties.FACING))
        propellerContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        if (!level!!.isClientSide) {
            this.running = true
            sendData()
        }
    }

    fun applyRotation() {
        if (propellerContraption == null) return
        propellerContraption!!.setAngle(angle.toFloat())
        val blockState = blockState
        if (blockState.hasProperty<Direction>(BlockStateProperties.FACING)) propellerContraption!!.setRotationAxis(
            blockState.getValue<Direction>(
                BlockStateProperties.FACING
            )
                .axis
        )
    }

    override fun onStall() {
        if (!level!!.isClientSide) {
            targetOmega = 0.0
            sendData()
        }
    }

    override fun isValid(): Boolean {
        return !this.isRemoved
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var pT = partialTicks
        if (isVirtual) {
            return Mth.lerp((partialTicks + .5f).toDouble(), previousAngle, angle).toFloat()
        }
        if (propellerContraption == null || !running) pT = 0f

        val renderedOmega = if (!isInverted()) Mth.lerp(pT.toDouble(), getAngularSpeed(), targetOmega) else Mth.lerp(pT.toDouble(), -getAngularSpeed(), -targetOmega)
        return Mth.lerp(pT.toDouble(), angle, angle + getAngularSpeed()).toFloat()
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun setAngle(forcedAngle: Float) {
        previousAngle = angle
        angle = forcedAngle.toDouble()
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)

        val angleBefore = angle
        if (compound.contains("Stopping")) {
            if (clientPacket && !stopping && compound.getBoolean("Stopping")) {
                starting = false
                startingProgress = 0.0
                disassemblyProgress = currentOmega.absoluteValue
                totalDisassemblyTime = disassemblyProgress
            }
            stopping = compound.getBoolean("Stopping")
        }
        if (compound.contains("Starting")) {
            if (clientPacket && !starting && compound.getBoolean("Starting")) {
                stopping = false
                disassemblyProgress = 0.0
                startingProgress = 0.0
            }
            starting = compound.getBoolean("Starting")
        }
        if (compound.contains("PhysID")) {
            physID = compound.getInt("PhysID")
        }
        if (compound.contains("Running")) {
            running = compound.getBoolean("Running")
        }
        if (compound.contains("Active")) {
            active = compound.getBoolean("Active") && !stopping
        }
        if (compound.contains("TargetOmega")) {
            targetOmega = compound.getDouble("TargetOmega")
        }
        if (compound.contains("CurrentOmega")) {
            currentOmega = compound.getDouble("CurrentOmega")
        }
        if (compound.contains("Angle")) {
            angle = compound.getDouble("Angle")
        }
        if (compound.contains("RotationDirection")) {
            rotationDirection.setValue(compound.getInt("RotationDirection"))
        }

//        if (clientPacket && ()) {
//            clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle.toDouble())
//            angle = angleBefore
//        }
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {

        compound.putBoolean("Stopping", stopping)
        compound.putBoolean("Starting", starting)
        compound.putInt("PhysID", physID)
        compound.putBoolean("Running", running)
        compound.putBoolean("Active", active)
        compound.putDouble("TargetOmega", targetOmega)
        compound.putDouble("CurrentOmega", currentOmega)
        compound.putDouble("Angle", angle)
        compound.putInt("RotationDirection", rotationDirection.getValue())

        super.write(compound, clientPacket)
    }

    override fun lazyTick() {
        super.lazyTick()
        if (running && propellerContraption != null) {
            sendData()
        }
    }

    companion object {
        fun rpmToOmega(rpm: Float): Double {
            return (rpm * (2.0 * Math.PI)) / 60.0
        }

        fun omegaToRPM(omega: Double): Float {
            return ((omega * 60.0) / (2.0 * Math.PI)).toFloat()
        }
    }

    fun getPowerDirections(): Pair<Axis, Axis> {
        val perpendicularAxes = (Axis.values().filter {
            it != (blockState as DirectionalAxisKineticBlock).getRotationAxis(
                blockState
            )
        })
        return perpendicularAxes[0] to perpendicularAxes[1]
    }

    protected fun getPower(): Pair<Int, Int> {
        val perpendicularAxes = (Axis.values().filter {
            it != (blockState.block as DirectionalAxisKineticBlock).getRotationAxis(
                blockState
            )
        })

        val powerOne = run {
            val positiveDir = Direction.get(POSITIVE, perpendicularAxes[0])
            val negativeDir = positiveDir.opposite

            var power = 0
            power += level!!.getSignal(blockPos.relative(positiveDir), positiveDir)
            power -= level!!.getSignal(blockPos.relative(negativeDir), negativeDir)
            power
        }
        val powerTwo = run {
            val positiveDir = Direction.get(POSITIVE, perpendicularAxes[1])
            val negativeDir = positiveDir.opposite

            var power = 0
            power += level!!.getSignal(blockPos.relative(positiveDir), positiveDir)
            power -= level!!.getSignal(blockPos.relative(negativeDir), negativeDir)
            power
        }

        return powerOne to powerTwo
    }

    override fun calculateStressApplied(): Float {
        if(level!!.isClientSide) return lastStressApplied
        var stressImpact = 0.0
        val axis : Vector3dc = blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD()
        if (propellerContraption != null) {
            if(sailPositions.isNotEmpty()) {
                // Add stress impact from propeller contraption.
                // Stress impact of propeller contraption is computed by:
                // sum((sail relative pos from bearing).cross(bearing axis))
                for (sail in sailPositions) {
                    stressImpact += axis.cross(sail.x().toDouble(), sail.y().toDouble(), sail.z().toDouble(), Vector3d()).length()
                }
            } else if (this.theoreticalSpeed != 0f) {
                // Add stress impact from propeller blades.
                for (blade in blades) {
                    // TODO: Single point for deriving blade width from blade.wide
                    // TODO: multiply internal RPM of propeller by 8.0
                    stressImpact += (PropellerController.calculateBladePower(0.0,
                        this.theoreticalSpeed.toDouble() * 8.0 / 60.0,
                        blade.length, blade.angle, if (blade.wide) 0.375 else 0.25
                    ) / 100.0).roundToInt() / this.theoreticalSpeed
                }
            }
        }
        lastStressApplied = stressImpact.toFloat()
        return stressImpact.toFloat()
    }
}
