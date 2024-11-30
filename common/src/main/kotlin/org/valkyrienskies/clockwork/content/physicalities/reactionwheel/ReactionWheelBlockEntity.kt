package org.valkyrienskies.clockwork.content.physicalities.reactionwheel

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.valkyrienskies.clockwork.content.forces.ReactionWheelController
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelCreateData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelUpdateData
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign

class ReactionWheelBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos,
    state
), IForceApplierBE<ReactionWheelUpdateData, ReactionWheelData, ReactionWheelCreateData, ReactionWheelController> {

    val clientSpeed: LerpedFloat = LerpedFloat.linear()

    var realSpeed: Double = 0.0
    var targetSpeed: Double = 0.0

    val pendingMomentumConsumptionQueue = ConcurrentLinkedQueue<Double>()

    var reachedTarget = false

    override var physID: Int = -1

    override fun newCreateData(): ReactionWheelCreateData {
        return ReactionWheelCreateData.fromBlockEntity(this)
    }

    override fun newUpdateData(): ReactionWheelUpdateData {
        return ReactionWheelUpdateData(realSpeed)
    }

    override fun getRenderBoundingBox(): AABB {
        return super.createRenderBoundingBox().inflate(2.0)
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putDouble("ServerSpeed", realSpeed)
        if (physID > -1) {
            compound.putInt("PhysID", physID)
        }
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (clientPacket) {
            clientSpeed.chase(compound.getDouble("ServerSpeed"), 1.0/64.0, LerpedFloat.Chaser.EXP)
        }
        if (compound.contains("PhysID")) {
            physID = compound.getInt("PhysID")
        }
    }

    fun calculateTargetSpeed(): Double {
        if (pendingMomentumConsumptionQueue.isNotEmpty()) {
            targetSpeed = pendingMomentumConsumptionQueue.poll()
        }

        return if (getSpeed() != 0f) {
            Mth.clamp(targetSpeed + (getSpeed().toDouble() / 20.0), -512.0, 512.0)
        } else {
            targetSpeed
        }
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) {
            clientSpeed.tickChaser()
            return
        }

        if (level == null) {
            return
        }

        assert (level is ServerLevel)

        targetSpeed = calculateTargetSpeed()

        realSpeed = Mth.lerp(0.05, realSpeed, targetSpeed)

        if (realSpeed != targetSpeed || !reachedTarget) {
            reachedTarget = Mth.equal(realSpeed, targetSpeed)
            sendData()

        }

        val isOnShip = (level!! as ServerLevel).getShipObjectManagingPos(worldPosition) != null

        if (isOnShip) {
            val ship = (level!! as ServerLevel).getShipObjectManagingPos(worldPosition)!!
            val attachment = ReactionWheelController.getOrCreate(ship)!!

            tickData(attachment, !reachedTarget)
        }
    }

    override fun remove() {
        removeApplier(ReactionWheelController::class.java, level!!, worldPosition)
        super.remove()
    }

    override fun destroy() {
        removeApplier(ReactionWheelController::class.java, level!!, worldPosition)
        super.destroy()
    }

}