package org.valkyrienskies.clockwork.content.contraptions.propeller

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.BearingContraption
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD

class PropellerBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState, val brass: Boolean = false) : KineticBlockEntity(type, pos, state), IBearingBlockEntity, IForceApplierBE<PropUpdateData, PropData, PropCreateData, PropellerController> {

    var sailPositions: MutableList<Vector3ic> = ArrayList()
    override var physID: Int = -1

    var propellerContraption: ControlledContraptionEntity? = null

    var targetOmega = 0.0
    var currentOmega = 0.0
    var previousOmega = 0.0

    var angle = 0.0
    var previousAngle = 0.0

    var active = false
    var running = false

    var stopping = false
    var disassemblyProgress = 0.0

    var assembleNextTick = false
    var assembleCooldown = 0

    var clientAngle = 0.0

    var lastException: AssemblyException? = null

    lateinit var rotationDirection: ScrollOptionBehaviour<RotationDirection>

    override fun newCreateData(): PropCreateData {
        return PropCreateData(worldPosition.toJOML(), blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD(), angle, currentOmega, sailPositions, isInverted(), active, brass)
    }

    override fun newUpdateData(): PropUpdateData {
        return PropUpdateData(currentOmega, angle, isInverted(), active)
    }

    fun shutDown() {
        if (level!!.isClientSide || assembleCooldown > 0) return
        this.assembleCooldown = 10
        this.active = false
        this.stopping = true
        this.disassemblyProgress = Math.toDegrees(currentOmega)
        sendData()
    }

    fun getSails() {
        sailPositions = ArrayList()
        if (propellerContraption != null) {
            val blocks = propellerContraption!!.contraption.blocks
            for ((key, value) in blocks) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(value.state)) {
                    sailPositions.add(key.toJOML())
                }
            }
        }
    }

    override fun tick() {
        super.tick()
        if (level == null) return
        if (level!!.isClientSide) {
            return
        } else {
            if (assembleNextTick && assembleCooldown == 0) {
                assembleNextTick = false
                assembleCooldown = 10
                assemble()
            }
            if (assembleCooldown > 0) {
                assembleCooldown--
            }

            if (!running) return

            previousAngle = angle
            previousOmega = currentOmega
            if (!stopping) {
                val stalled = propellerContraption?.isStalled ?: true
                active = !overStressed && !stalled
                updateSpinDir(currentOmega < 0)
                val lastTargetOmega = targetOmega
                targetOmega = rpmToOmega(this.getSpeed() * 2f)

                if (lastTargetOmega != targetOmega) {
                    sendData()
                }

                currentOmega = Mth.lerp(0.1, currentOmega, targetOmega)
            } else {
                active = false
                disassemblyProgress--
                if (disassemblyProgress <= 0) {
                    disassemble()
                    stopping = false
                } else {
                    val stoppingPoint = angle + Math.toDegrees(currentOmega) * disassemblyProgress * 0.5
                    val optimalStoppingPoint = 90.0 * Math.round(stoppingPoint / 90.0)
                    val Q = (optimalStoppingPoint - stoppingPoint) / disassemblyProgress
                    currentOmega = (currentOmega + 6.0 * Q / disassemblyProgress) * (1.0 - 1.0 / disassemblyProgress)
                }
            }

            val newAngle = angle + Math.toDegrees(currentOmega)
            angle = newAngle % 360.0

            if (level!!.getShipObjectManagingPos(blockPos) != null && physID != -1) {
                val shipOn = (level as ServerLevel).getShipObjectManagingPos(blockPos)!!
                val attachment = PropellerController.getOrCreate(shipOn)!!
                tickData(attachment, true)
            }
        }
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

    fun assemble() {
        if (level!!.getBlockState(worldPosition)
                .block !is PropellerBearingBlock
        ) return
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
        propellerContraption = ControlledContraptionEntity.create(level, this, contraption)
        propellerContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        propellerContraption!!.rotationAxis = direction.axis
        propellerContraption?.let { level!!.addFreshEntity(it) }
        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition)
        running = true
        angle = 0.0
        currentOmega = 0.0

        targetOmega = rpmToOmega(this.getSpeed() * 2f)

        if (brass) {
            getSails()
        }

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

    fun disassemble() {
        if (!running && propellerContraption == null) return

        targetOmega = 0.0
        currentOmega = 0.0
        angle = 0.0
        stopping = false
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
        return contraption == propellerContraption
    }

    override fun attach(contraption: ControlledContraptionEntity?) {
        val blockState = blockState
        if (contraption!!.contraption !is BearingContraption) return
        if (!blockState.hasProperty(BearingBlock.FACING)) return

        this.propellerContraption = contraption
        setChanged()
        val anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING))
        propellerContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        if (!level!!.isClientSide) {
            this.running = true
            sendData()
        }
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
        if (isVirtual) return Mth.lerp((partialTicks + .5f).toDouble(), previousAngle, angle).toFloat()
        if (propellerContraption == null || !running) pT = 0f

        var renderedOmega = if (!isInverted()) Mth.lerp(pT.toDouble(), currentOmega, targetOmega) else Mth.lerp(pT.toDouble(), -currentOmega, -targetOmega)
        return Mth.lerp(pT.toDouble(), angle, angle + Math.toDegrees(renderedOmega)).toFloat()
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

        if (compound.contains("Stopping")) {
            stopping = compound.getBoolean("Stopping")
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
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {

        compound.putBoolean("Stopping", stopping)
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
}
