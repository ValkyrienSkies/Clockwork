package org.valkyrienskies.clockwork.util.fluid

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.NBTHelper
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.material.Fluid
import org.apache.commons.lang3.mutable.MutableInt
import org.valkyrienskies.clockwork.mixin.accessors.FluidAccessor
import org.valkyrienskies.clockwork.platform.PlatformUtils.cwFluidTank
import java.util.function.Consumer

abstract class CWFluidTankBehaviour protected constructor(
        private val behaviourType: BehaviourType<CWFluidTankBehaviour>,
        te: SmartBlockEntity?,
        tanks: Int,
        tankCapacity: Long,
        enforceVariety: Boolean
) : BlockEntityBehaviour(te) {
    protected var syncCooldown = 0
    protected var queuedSync = false
    var tanks: Array<TankSegment?>
        protected set
    protected var extractionAllowed = true
    protected var insertionAllowed = true
    protected var fluidUpdateCallback: Runnable

    init {
        this.tanks = arrayOfNulls(tanks)
        for (i in 0 until tanks) {
            this.tanks[i] = TankSegment(tankCapacity)
        }
        fluidUpdateCallback = Runnable {}
    }

    protected abstract fun makeFluidTank(capacity: Long, updateCallback: Consumer<Fluid?>?): CWFluidTank
    fun whenFluidUpdates(fluidUpdateCallback: Runnable): CWFluidTankBehaviour {
        this.fluidUpdateCallback = fluidUpdateCallback
        return this
    }

    fun allowInsertion(): CWFluidTankBehaviour {
        insertionAllowed = true
        return this
    }

    fun allowExtraction(): CWFluidTankBehaviour {
        extractionAllowed = true
        return this
    }

    fun forbidInsertion(): CWFluidTankBehaviour {
        insertionAllowed = false
        return this
    }

    fun forbidExtraction(): CWFluidTankBehaviour {
        extractionAllowed = false
        return this
    }

    override fun initialize() {
        super.initialize()
        if (world.isClientSide) return
        forEach { ts: TankSegment? ->
            ts!!.fluidLevel.forceNextSync()
            ts.onFluidChanged()
        }
    }

    override fun tick() {
        super.tick()
        if (syncCooldown > 0) {
            syncCooldown--
            if (syncCooldown == 0 && queuedSync) updateFluids()
        }
        forEach { te: TankSegment? ->
            val fluidLevel =
                te!!.fluidLevel
            if (fluidLevel != null) fluidLevel.tickChaser()
        }
    }

    fun sendDataImmediately() {
        syncCooldown = 0
        queuedSync = false
        updateFluids()
    }

    fun sendDataLazily() {
        if (syncCooldown > 0) {
            queuedSync = true
            return
        }
        updateFluids()
        queuedSync = false
        syncCooldown = SYNC_RATE
    }

    protected fun updateFluids() {
        fluidUpdateCallback.run()
        blockEntity.sendData()
        blockEntity.setChanged()
    }

    val primaryHandler: CWFluidTank
        get() = primaryTank!!.tank
    val primaryTank: TankSegment?
        get() = tanks[0]
    val isEmpty: Boolean
        get() {
            for (tankSegment in tanks) if (!tankSegment!!.tank.isEmpty) return false
            return true
        }

    fun forEach(action: Consumer<TankSegment?>) {
        for (tankSegment in tanks) action.accept(tankSegment)
    }

    override fun write(nbt: CompoundTag, clientPacket: Boolean) {
        super.write(nbt, clientPacket)
        val tanksNBT = ListTag()
        forEach { ts: TankSegment? ->
            tanksNBT.add(
                ts!!.writeNBT()
            )
        }
        nbt.put(type.name + "Tanks", tanksNBT)
    }

    override fun read(nbt: CompoundTag, clientPacket: Boolean) {
        super.read(nbt, clientPacket)
        val index = MutableInt(0)
        NBTHelper.iterateCompoundList<Any>(
            nbt.getList(type.name + "Tanks", Tag.TAG_COMPOUND.toInt())
        ) { c: CompoundTag ->
            if (index.toInt() >= tanks.size) return@iterateCompoundList
            tanks[index.toInt()]!!.readNBT(c, clientPacket)
            index.increment()
        }
    }

    inner class TankSegment(capacity: Long) {
        var tank: CWFluidTank
            protected set
        var fluidLevel: LerpedFloat
            protected set
        var renderedFluid: Fluid? = null
            protected set

        init {
            tank = makeFluidTank(
                capacity
            ) { f: Fluid? -> onFluidChanged() }
            fluidLevel = LerpedFloat.linear()
                .startWithValue(0.0)
                .chase(0.0, .25, LerpedFloat.Chaser.EXP)
        }

        fun onFluidChanged() {
            if (!blockEntity.hasLevel()) return
            fluidLevel.chase(
                (tank.currentAmount / tank.totalCapacity.toFloat()).toDouble(),
                .25,
                LerpedFloat.Chaser.EXP
            )
            if (!world.isClientSide) sendDataLazily()
            if (blockEntity.isVirtual && !tank.isEmpty) renderedFluid = tank.fluidType
        }

        fun getTotalUnits(partialTicks: Float): Float {
            return fluidLevel.getValue(partialTicks) * tank.totalCapacity
        }

        fun writeNBT(): CompoundTag {
            val compound = CompoundTag()
            compound.put("TankContent", tank.store(CompoundTag()))
            compound.put("Level", fluidLevel.writeNBT())
            return compound
        }

        fun readNBT(compound: CompoundTag, clientPacket: Boolean) {
            tank.read(compound.getCompound("TankContent"))
            fluidLevel.readNBT(compound.getCompound("Level"), clientPacket)
            if (!tank.isEmpty) renderedFluid = tank.fluidType
        }

        fun isEmpty(partialTicks: Float): Boolean {
            val renderedFluid = renderedFluid
            if ((renderedFluid as FluidAccessor).getIfEmpty()) return true
            val units = getTotalUnits(partialTicks)
            return if (units < 1) true else false
        }
    }

    override fun getType(): BehaviourType<*> {
        return behaviourType
    }

    companion object {
        val TYPE = BehaviourType<CWFluidTankBehaviour>()
        val INPUT = BehaviourType<CWFluidTankBehaviour>("Input")
        val OUTPUT = BehaviourType<CWFluidTankBehaviour>("Output")
        private const val SYNC_RATE = 8
        fun single(te: SmartBlockEntity?, capacity: Long): CWFluidTankBehaviour {
            return cwFluidTank(
                TYPE,
                te!!, 1, capacity, false
            )
        }
    }
}
