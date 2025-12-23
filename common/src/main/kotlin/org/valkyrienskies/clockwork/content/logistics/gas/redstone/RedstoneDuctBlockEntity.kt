package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType

class RedstoneDuctBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

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
        return if (conditional != null && conditional!!.passes(ClockworkMod.getKelvin(), getDuctNodePosition())) 15 else 0
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