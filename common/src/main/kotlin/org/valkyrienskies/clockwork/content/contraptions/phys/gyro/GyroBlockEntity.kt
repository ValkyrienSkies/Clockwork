package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlock
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d


class GyroBlockEntity(typeIn: BlockEntityType<GyroBlockEntity>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(typeIn, pos, state) {

    var visualSpeed: LerpedFloat = LerpedFloat.linear()
    var angle: Float = 0f

    var targetVec3: Vec3 = Vec3(0.0,1.0,0.0)
    private val ship: ServerShip? get() = (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    private val control: GyroShipControl? get() = ship?.getAttachment(GyroShipControl::class.java)

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
        val targetVec3 = targetVec3
        compound.putVector3d("TargetVector", targetVec3.toJOML())
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (compound.contains("TargetVector")) {
            targetVec3 = compound.getVector3d("TargetVector")!!.toMinecraft()
        }

        if (clientPacket) {
            visualSpeed.chase(generatedSpeed.toDouble(), (1 / 64f).toDouble(), LerpedFloat.Chaser.EXP)
        }
    }
}