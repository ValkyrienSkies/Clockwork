package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.Iterate
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import java.awt.Point
import kotlin.math.max


class GyroBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(typeIn, pos, state) {

    var redstonePowerXN: Int = 0
    var redstonePowerZN: Int = 0
    var redstonePowerXP: Int = 0
    var redstonePowerZP: Int = 0

    var visualSpeed: LerpedFloat = LerpedFloat.linear()
    var angle: Float = 0f
    var coreAngle = 0f
    var previousCoreAngle = 0f

    var targetQuat: Quaterniond = Quaterniond(0.0,1.0,0.0,0.0)
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

        if (level == null) {
            return
        }

        if (level is ServerLevel) {
            control?.ship = ship
            control?.speed = getSpeed()
            control?.pointTowards(targetQuat, 1.0f)
        }

        updatePower(level!!, blockPos)

        targetQuat.x = (redstonePowerXP / 15.0) - (redstonePowerXN / 15.0)
        targetQuat.z = (redstonePowerZP / 15.0) - (redstonePowerZN / 15.0)
        val targetSpeed = getSpeed()
        visualSpeed.updateChaseTarget(targetSpeed)
        visualSpeed.tickChaser()
        angle += visualSpeed.value * 3 / 10f
        angle %= 360f
    }

    private fun updatePower(worldIn: Level, pos: BlockPos) {
        var powerZP = worldIn.getSignal(pos.relative(Direction.SOUTH), Direction.SOUTH) //EAST
        var powerZN = worldIn.getSignal(pos.relative(Direction.NORTH), Direction.NORTH) //WEST

        var powerXP = worldIn.getSignal(pos.relative(Direction.EAST), Direction.EAST)
        var powerXN = worldIn.getSignal(pos.relative(Direction.WEST), Direction.WEST)

        this.redstonePowerXP = powerXP
        this.redstonePowerZP = powerZP
        this.redstonePowerXN = powerXN
        this.redstonePowerZN = powerZN
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.putDouble("X", targetQuat.x())
        compound.putDouble("Y", targetQuat.y())
        compound.putDouble("Z", targetQuat.z())
        compound.putDouble("W", targetQuat.w())

        compound.putInt("PowerXP", redstonePowerXP)
        compound.putInt("PowerZP", redstonePowerZP)
        compound.putInt("PowerXN", redstonePowerXN)
        compound.putInt("PowerZN", redstonePowerZN)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("X")) {
            targetQuat = Quaterniond(compound.getDouble("X"), compound.getDouble("Y"), compound.getDouble("Z"), compound.getDouble("Z"))
        }
        if (compound.contains("PowerXP")) {
            redstonePowerXP = compound.getInt("PowerXP")
            redstonePowerZP = compound.getInt("PowerZP")
            redstonePowerXN = compound.getInt("PowerXN")
            redstonePowerZN = compound.getInt("PowerZN")
        }
        super.read(compound, clientPacket)

        if (clientPacket) {
            visualSpeed.chase(generatedSpeed.toDouble(), (1 / 64f).toDouble(), LerpedFloat.Chaser.EXP)
        }
    }
}