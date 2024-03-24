package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.integration.cc.ComputerAttachmentHandler
import org.valkyrienskies.clockwork.platform.PlatformUtils.isModLoaded
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.MinecraftUtil.between
import java.util.function.Consumer

class SequencedSeatBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    SplitShaftBlockEntity(typeIn, pos, state) {
    var forwardRules: SequencedSeatRuleList = SequencedSeatRuleList.defaultList(Rotation.NONE)
        private set
    var backwardRules: SequencedSeatRuleList = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_180)
        private set
    var leftRules: SequencedSeatRuleList = SequencedSeatRuleList.defaultList(Rotation.COUNTERCLOCKWISE_90)
        private set
    var rightRules: SequencedSeatRuleList = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_90)
        private set
    private var pressedKeys = setOf<InputKey>()
    public val degreesAwayFromBase = FloatArray(4)
    private val lastModifier = FloatArray(4)

    val computerHandler: ComputerAttachmentHandler = ComputerAttachmentHandler()
    @OptIn(ExperimentalStdlibApi::class)
    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        for (i in 0..3) {
            val dir = Direction.entries[i + 2]
            val modifier = getRotationSpeedModifier(dir)
            degreesAwayFromBase[i] += convertToAngular(modifier * speed)
            if (modifier != lastModifier[i]) {
                detachKinetics()
                attachKinetics()
            }
            lastModifier[i] = modifier
            if (degreesAwayFromBase[i] > 360) degreesAwayFromBase[i] -= 360f
            if (degreesAwayFromBase[i] < 0) degreesAwayFromBase[i] += 360f
        }
    }

    override fun getRotationSpeedModifier(face: Direction): Float {
        if (isVirtual || !hasSource()) return 1f
        if (sourceFacing != Direction.DOWN) return 0f
        return if (face == sourceFacing) 1f else getList(face).currentModifier(this, face)
    }

    fun getList(face: Direction?): SequencedSeatRuleList {
        val forward = blockState.getValue<Direction>(BlockStateProperties.HORIZONTAL_FACING)
        val rotation = between(forward, face!!)
        return getList(rotation)
    }

    fun getList(rotation: Rotation): SequencedSeatRuleList {
        return when (rotation) {
            Rotation.NONE -> forwardRules
            Rotation.CLOCKWISE_90 -> rightRules
            Rotation.CLOCKWISE_180 -> backwardRules
            Rotation.COUNTERCLOCKWISE_90 -> leftRules
        }
    }

    fun pressedKeys(): Set<InputKey> {
        return pressedKeys
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.put(ClockworkConstants.Nbt.FORWARD_RULES, forwardRules.serializeNBT())
        compound.put(ClockworkConstants.Nbt.BACKWARD_RULES, backwardRules.serializeNBT())
        compound.put(ClockworkConstants.Nbt.LEFT_RULES, leftRules.serializeNBT())
        compound.put(ClockworkConstants.Nbt.RIGHT_RULES, rightRules.serializeNBT())
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        forwardRules.deserializeNBT(compound.getList(ClockworkConstants.Nbt.FORWARD_RULES, CompoundTag.TAG_COMPOUND.toInt()))
        backwardRules.deserializeNBT(compound.getList(ClockworkConstants.Nbt.BACKWARD_RULES, CompoundTag.TAG_COMPOUND.toInt()))
        leftRules.deserializeNBT(compound.getList(ClockworkConstants.Nbt.LEFT_RULES, CompoundTag.TAG_COMPOUND.toInt()))
        rightRules.deserializeNBT(compound.getList(ClockworkConstants.Nbt.RIGHT_RULES, CompoundTag.TAG_COMPOUND.toInt()))
    }

    fun updateRules(
        forwardRules: SequencedSeatRuleList,
        backwardRules: SequencedSeatRuleList,
        leftRules: SequencedSeatRuleList,
        rightRules: SequencedSeatRuleList
    ) {
        this.forwardRules = forwardRules
        this.backwardRules = backwardRules
        this.leftRules = leftRules
        this.rightRules = rightRules
        sendData()
        setChanged()
        detachKinetics()
        attachKinetics()
    }

    fun updateInput(pressedKeys: Set<InputKey>) {
        if (this.pressedKeys == pressedKeys) return
        if (!level!!.isClientSide) if (isModLoaded("computercraft")) {
            val event: MutableList<String> = ArrayList()
            this.pressedKeys.forEach(Consumer { key: InputKey -> event.add(key.name) })
            computerHandler.sendEvent("command_seat_keys", event)
        }
        this.pressedKeys = pressedKeys
    }

    fun getDegreesAwayFromBase(direction: Direction): Float {
        return degreesAwayFromBase[direction.ordinal - 2]
    }
}
