package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos


class GyroBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(typeIn, pos, state) {

    var visualSpeed: LerpedFloat = LerpedFloat.linear()
    var angle: Float = 0f
    var coreAngle = 0f
    var previousCoreAngle = 0f

    var targetVec3: Vector3dc = Vector3d(0.0, 1.0, 0.0)
    private val ship: ServerShip? get() = (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    private val control: GyroShipControl? get() = ship?.getAttachment(GyroShipControl::class.java)

    fun getInterpolatedCoreAngle(partialTicks: Float): Float {
        previousCoreAngle = coreAngle
        coreAngle++
        if (coreAngle == 360f) {
            coreAngle = 0f
        }
        return Mth.lerp(partialTicks, coreAngle, coreAngle + 4f)
    }

    override fun tick() {
        super.tick()

        if (level is ServerLevel) {
            control?.ship = ship
            control?.speed = getSpeed()
            control?.targetVector = targetVec3
        }

        val targetSpeed = getSpeed()
        visualSpeed.updateChaseTarget(targetSpeed)
        visualSpeed.tickChaser()
        angle += visualSpeed.value * 3 / 10f
        angle %= 360f
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.putDouble("X", targetVec3.x())
        compound.putDouble("Y", targetVec3.y())
        compound.putDouble("Z", targetVec3.z())
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("X")) {
            targetVec3 = Vector3d(compound.getDouble("X"), compound.getDouble("Y"), compound.getDouble("Z"))
        }
        super.read(compound, clientPacket)

        if (clientPacket) {
            visualSpeed.chase(generatedSpeed.toDouble(), (1 / 64f).toDouble(), LerpedFloat.Chaser.EXP)
        }
    }
}