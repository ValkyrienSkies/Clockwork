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

    var redstonePower: Point = Point(0,0)

    var visualSpeed: LerpedFloat = LerpedFloat.linear()
    var angle: Float = 0f
    var coreAngle = 0f
    var previousCoreAngle = 0f

    var targetQuat: Quaterniond = Quaterniond(0.0,1.0,0.0,0.0)
    private val ship: ServerShip? get() = (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    val control: GyroShipControl? get() = ship?.getAttachment(GyroShipControl::class.java)

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

        targetQuat.x = (redstonePower.x / 15.0) / 2
        targetQuat.z = (redstonePower.y / 15.0) / 2
        val targetSpeed = getSpeed()
        visualSpeed.updateChaseTarget(targetSpeed)
        visualSpeed.tickChaser()
        angle += visualSpeed.value * 3 / 10f
        angle %= 360f
    }

    /**
     * Updates the power of the current block based on neighboring blocks' signals.
     * Calculates the difference in power between the east and west directions (X-axis)
     * and between the south and north directions (Z-axis) to determine the redstone power.
     *
     * @param worldIn The level (world) in which the block resides.
     * @param pos The position of the current block.
     */
    private fun updatePower(worldIn: Level, pos: BlockPos) {
        val powerZP = worldIn.getSignal(pos.relative(Direction.SOUTH), Direction.SOUTH)
        val powerZN = worldIn.getSignal(pos.relative(Direction.NORTH), Direction.NORTH)

        val powerXP = worldIn.getSignal(pos.relative(Direction.EAST), Direction.EAST)
        val powerXN = worldIn.getSignal(pos.relative(Direction.WEST), Direction.WEST)

        this.redstonePower = Point(powerXP - powerXN,  powerZP - powerZN)
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.putDouble("X", targetQuat.x())
        compound.putDouble("Y", targetQuat.y())
        compound.putDouble("Z", targetQuat.z())
        compound.putDouble("W", targetQuat.w())

        compound.putInt("PowerX", redstonePower.x)
        compound.putInt("PowerZ", redstonePower.y)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (compound.contains("X")) {
            targetQuat = Quaterniond(compound.getDouble("X"), compound.getDouble("Y"), compound.getDouble("Z"), compound.getDouble("Z"))
        }
        if (compound.contains("PowerX")) {
            redstonePower = Point(compound.getInt("PowerX"), compound.getInt("PowerXZ"))
        }
        super.read(compound, clientPacket)

        if (clientPacket) {
            visualSpeed.chase(generatedSpeed.toDouble(), (1 / 64f).toDouble(), LerpedFloat.Chaser.EXP)
        }
    }
}