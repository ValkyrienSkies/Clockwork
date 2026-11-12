package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity

class RedstoneDuctBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), IConnectable, IThinkable {

    override fun providesThinkable(side: Direction) = side.axis != blockState.getValue(RotatedPillarBlock.AXIS)
    override fun thinkablePortName(side: Direction): Component? = if (providesThinkable(side))
        Component.translatable("thinkable.vs_clockwork.port.gas.${conditional?.type?.name?.lowercase() ?: "none"}") else null
    override fun thinkableValue(side: Direction): Double? {
        if (level?.isClientSide != false || !providesThinkable(side)) return null
        return conditional?.measurement(ClockworkMod.getKelvin(level), getDuctNodePosition())
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) { }

    var conditional: RedstoneDuctConditional? = null

    override fun tick() {
        super.tick()

        val oldPower = blockState.getValue(RedstoneDuctBlock.POWER)

        if (level == null || level!!.isClientSide) return

        val power = getPower()
        if (oldPower != power) {
            level?.setBlockAndUpdate(blockPos, blockState.setValue(RedstoneDuctBlock.POWER, power))
        }
    }


    fun openScreen(player: Player) {
        ScreenOpener.open(RedstoneDuctScreen(this))
    }

    fun getPower(): Int {
        return if (conditional != null && conditional!!.passes(ClockworkMod.getKelvin(level), getDuctNodePosition())) 15 else 0
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (conditional != null)
        tag.put("Conditional", conditional!!.serialize(CompoundTag()))
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        if (tag.contains("Conditional"))
        conditional = RedstoneDuctConditional.deserialize(tag.get("Conditional") as CompoundTag)

        super.write(tag, clientPacket)
    }
}
