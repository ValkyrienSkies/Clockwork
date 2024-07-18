package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs

class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState): KineticBlockEntity(typeIn, pos, state) {

    lateinit var pumpDirection: ScrollOptionBehaviour<PumpDirectionMode>

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        behaviours.add(
            ScrollOptionBehaviour<PumpDirectionMode>(
                PumpDirectionMode::class.java,
                ClockworkLang.translateDirect("logistics.pump_direction"), this, PumpDirectionBoxTransform()
            ).also {
                pumpDirection = it
            })
        super.addBehavioursDeferred(behaviours)
    }

    override fun onLoad() {
        super.onLoad()
        ClockworkMod.getKelvin().markLoaded(this.blockPos.toJOMLD())
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        ClockworkMod.getKelvin().markUnloaded(this.blockPos.toJOMLD())
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("currentTemperature")) {
            ClockworkMod.getKelvin().nodeInfo[this.blockPos.toJOMLD()]?.currentTemperature = tag.getDouble("currentTemperature")
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putDouble("currentTemperature", ClockworkMod.getKelvin().getTemperatureAt(this.blockPos.toJOMLD()))
        super.write(tag, clientPacket)
    }

    override fun initialize() {
        super.initialize()

        if (this.level?.isClientSide == true) {
            return
        }

        ClockworkMod.getKelvin().addNode(this.blockPos.toJOMLD(),
            DuctNetworkUtils.createPipeNode(this.blockPos.toJOMLD(), ClockworkMod.getKelvin())
        )
        for (dir in Direction.values()) {
            val block = this.level!!.getBlockState(this.blockPos).block
            val otherBlock = this.level!!.getBlockState(this.blockPos.relative(dir)).block
            if (block is IDuct && otherBlock is IDuct && block.canConnectTo(this.blockPos, this.blockPos.relative(dir), this.level!!) && otherBlock.canConnectTo(this.blockPos.relative(dir), this.blockPos, this.level!!)) {
                ClockworkMod.getKelvin().addEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD(),
                    DuctNetworkUtils.createPipeEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD())
                )
            }
        }
    }

    override fun remove() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.remove()
    }

    override fun destroy() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.destroy()
    }

    override fun tick() {
        super.tick()

        if (this.level?.isClientSide == true) {
            return
        }

        if (this.getSpeed() == 0f) {
            return
        }

        val pumpPressure = (abs(this.getSpeed()).toDouble() / 256.0) * maxPumpPressure

        if (ClockworkMod.getKelvin().nodes[this.blockPos.toJOMLD()]?.behavior == NodeBehaviorType.PUMP) {
            (ClockworkMod.getKelvin().nodes[this.blockPos.toJOMLD()] as PumpDuctNode).pumpPressure = pumpPressure
        }


    }

    companion object {
        const val maxPumpPressure: Double = 1023440.0
    }

    enum class PumpDirectionMode(icon: AllIcons): INamedIconOptions {
        FORWARD(AllIcons.I_MTD_LEFT),
        BACKWARD(AllIcons.I_MTD_RIGHT);

        private var translationKey: String
        private var icon: AllIcons

        init {
            this.icon = icon
            translationKey = "pump.direction." + Lang.asId(name)
        }

        override fun getIcon(): AllIcons? {
            return icon
        }

        override fun getTranslationKey(): String? {
            return translationKey
        }
    }

    private class PumpDirectionBoxTransform(): ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3 {
            return Vec3(0.5, 0.5, 0.5)
        }

    }
}