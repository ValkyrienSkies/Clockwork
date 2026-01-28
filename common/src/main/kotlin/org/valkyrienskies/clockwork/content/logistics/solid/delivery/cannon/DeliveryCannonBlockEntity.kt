package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.lang.Lang
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotBehaviour
import org.valkyrienskies.clockwork.platform.SolidDeliveryMethods
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.api.toFloat
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.world.clipIncludeShips
import kotlin.math.*

class DeliveryCannonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHaveGoggleInformation {

    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour
    lateinit var distributionModeBehaviour: ScrollOptionBehaviour<DistributionMode>

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        frequencySlotBehaviour = FrequencySlotBehaviour(this, FrequencySlot())
        behaviours.add(frequencySlotBehaviour)

        distributionModeBehaviour = ScrollOptionBehaviour<DistributionMode>(
            DistributionMode::class.java, ClockworkLang.translateDirect("delivery_cannon.distribution_mode"),
            this, FrequencySlot()
        )

        distributionModeBehaviour.requiresWrench()

        behaviours.add(frequencySlotBehaviour)
        behaviours.add(distributionModeBehaviour)


    }

    var currentStack: ItemStack = ItemStack.EMPTY
    var midAirStack    : ItemStack = ItemStack.EMPTY



    var lastVel = 0.0
    var visitedChutes = HashSet<BlockPos>()

    var cooldown = 0.0
    var gunpowderTicks = 0.0
    var shootingAtChute: BlockPos? = null

    var xRot = LerpedFloat.angular()
    var yRot = LerpedFloat.angular()
    var distance = LerpedFloat.linear()

    val soundRandom = RandomSource.create()
    // for Client
    var clientShotProgress = 0.0
    var clientBarrelOffset = 0.0f
    var clientCannonRotationOffset = 0.0f
    var clientAntennaRotationOffset = 0.0f
    var clientItemRotation = 0.0
    var fired = false

    val gunpowderedCoefficient: Double get()
    { return if (gunpowderTicks > 0) 1.0 else 0.0 }

    val realPos: Vector3d get()
    { return ClockworkUtils.getRealPos(level, blockPos) }

    val isRoundRobin: Boolean get()
    {return distributionModeBehaviour.get() == DistributionMode.ROUND_ROBIN}

    val defaultXrot: Double get()
    {return blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()}

    init {

        xRot.chase(defaultXrot, 1.0, LerpedFloat.Chaser.LINEAR)
        yRot.chase(0.0, 1.0, LerpedFloat.Chaser.LINEAR)
        distance.chase(0.0, 0.5, LerpedFloat.Chaser.LINEAR)
    }

    override fun lazyTick() {
       sendData()
    }

    override fun tick() {
        super.tick()

        xRot.updateChaseSpeed(1.0 + gunpowderedCoefficient * 3)
        yRot.updateChaseSpeed(1.0 + gunpowderedCoefficient * 3)
        distance.updateChaseSpeed(0.5 + gunpowderedCoefficient * 2)


        xRot.tickChaser()
        yRot.tickChaser()
        distance.tickChaser()
        gunpowderTicks = max(gunpowderTicks, gunpowderTicks-1)

        if (level!!.isClientSide) return

        if (shootingAtChute != null) {
            val chute = ActiveChutes.actives[shootingAtChute]
            if (chute != null) distance.updateChaseTarget(chute.realPos.distance(realPos).toFloat())
            else if (ActiveChutes.unloaded[shootingAtChute] == null) reset()

            if (abs(distance.value - distance.chaseTarget) < 0.5 && chute != null) {
                chute.busy = false
                chute.receiveItem(midAirStack)

                reset()
            }
            return
        }


        cooldown = max(cooldown, cooldown-1)

        if (midAirStack.isEmpty && !currentStack.isEmpty) {
            // TODO: CONFIGURE MAX DISTANCE
            val chutes = ActiveChutes.getSortedChuteWithFrequency(realPos,100.0,frequencySlotBehaviour.frequency)
            if (chutes.isEmpty()) return

            var chute: BlockPos? = null
            var chuteBe: DeliveryChuteBlockEntity? = null

            for (possibleChute in chutes) {
                if (isRoundRobin && possibleChute in visitedChutes) continue
                chute = possibleChute
                chuteBe = ActiveChutes.actives[chute] ?: continue
                if (chuteBe.busy || !chuteBe.receiveItem(currentStack, true) || isObstructed(chuteBe)) {
                    chuteBe = null
                    continue
                }
                break
            }

            chuteBe ?: return visitedChutes.clear()


            updateAngleChaser(chuteBe)
            if (abs(xRot.value-xRot.chaseTarget) < 1 && abs(yRot.value-yRot.chaseTarget) < 1) {
                if (isRoundRobin) visitedChutes.add(chuteBe.blockPos)
                shootingAtChute = chute
                midAirStack = currentStack
                currentStack = ItemStack.EMPTY
                distance.updateChaseTarget(realPos.distance(ActiveChutes.actives[chute]!!.realPos).toFloat())

                chuteBe.busy = true

                val pitch = Mth.randomBetween(soundRandom, 0.9f, 1.1f)
                level!!.playSound(null, blockPos, ClockworkSounds.THWOOM.mainEvent!!, SoundSource.BLOCKS, 1f,pitch)
                fired = true

                sendData()
            }
        } else if (currentStack.isEmpty) currentStack = SolidDeliveryMethods.extractFrom(level!!, this)


    }

    fun reset() {
        xRot.updateChaseTarget(defaultXrot.toFloat())
        yRot.updateChaseTarget(0f)
        distance.setValue(0.0)
        distance.updateChaseTarget(0f)

        shootingAtChute = null
        midAirStack = ItemStack.EMPTY
        sendData()
    }

    fun updateAngleChaser(chuteBlockEntity: DeliveryChuteBlockEntity) {

        val vertex = getThirdPoint(realPos, chuteBlockEntity.realPos)
        val deltaP = realPos.sub(vertex)
        val ship = level!!.getShipManagingPos(blockPos)
        if (ship != null) {
            val temp = ship.worldToShip.transformDirection(Vector3d(deltaP.x,deltaP.y,deltaP.z), Vector3d())
            deltaP.x = temp.x
            deltaP.y = temp.y
            deltaP.z = temp.z
        }
        val xTargetRot = euler_angle(deltaP.z,-deltaP.x)


        val otherV: Double
        if (abs(deltaP.z) > abs(deltaP.x)) otherV = deltaP.z
        else otherV =  deltaP.x
        var u_angle = euler_angle(deltaP.y,otherV)
        if (u_angle>90) u_angle=180-u_angle
        val yTargetRot = u_angle


        xRot.updateChaseTarget(xTargetRot.toFloat())
        yRot.updateChaseTarget(min(90.0f,yTargetRot.toFloat()))
        sendData()
    }

    fun isObstructed(chute: DeliveryChuteBlockEntity): Boolean {
        return false
        val vertex = getThirdPoint(realPos, chute.realPos).toMinecraft()

        val cannonToVertex = clip(realPos.add(0.0,0.5,0.0).toMinecraft(), vertex)
        //println("CTV: ${cannonToVertex.type} ${cannonToVertex.blockPos}")
        if (cannonToVertex.type != HitResult.Type.MISS) return true

        val vertexToChute = clip(vertex, chute.realPos.toMinecraft())
        //println("VTC: ${vertexToChute.type} ${vertexToChute.blockPos} ${chute.blockPos}")
        return vertexToChute.type == HitResult.Type.MISS || vertexToChute.blockPos != chute.blockPos

    }


    fun clip(from: Vec3, to: Vec3): BlockHitResult {
        return level!!.clipIncludeShips(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
    }

    fun addGunpowderTicks(count: Int) {
        gunpowderTicks += count*6000
    }

    fun getParabolaY(vec: Vector3d): Double {
        val startPos = realPos
        val endPos = ClockworkUtils.getRealPos(level!!, shootingAtChute!!)
        val middlePos = getThirdPoint(startPos, endPos)

        var sX = startPos.x
        var eX = endPos.x
        var vX = middlePos.x
        var iX = vec.x

        // Picks an axis to use for the parabola.
        if (abs(endPos.x-startPos.x) < abs(endPos.z-startPos.z)) {

            sX = startPos.z
            eX = endPos.z
            vX = middlePos.z
            iX = vec.z
        }


        return parabola(sX,startPos.y,eX,endPos.y,vX,middlePos.y, iX)

    }

    companion object {


        fun parabola(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double,  z:Double): Double {
            val denom = (x1 - x2) * (x1 - x3) * (x2 - x3)

            val A = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom
            val B = (x3 * x3 * (y1 - y2) + x2 * x2 * (y3 - y1) + x1 * x1 * (y2 - y3)) / denom
            val C = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom


            return A*z.pow(2) + B*z + C
        }

        fun getThirdPoint(start: Vector3d, end: Vector3d): Vector3d {
            return Vector3d((start.x + end.x)/2, (start.y + end.y)/2+5, (start.z + end.z)/2)
        }


        fun euler_angle(x: Double,y: Double): Double {
            val rad = atan(y/x)   // arcus tangent in radians
            var deg = rad*180/Math.PI  // converted to degrees
            if (x<0) deg += 180        // fixed mirrored angle of arctan
            val eul = (270+deg)%360    // folded to [0,360) domain
            return eul
        }


    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        // The CompoundTag? ?: return is sus I will admit
        xRot.readNBT(tag.get("xRot") as CompoundTag? ?: return, clientPacket)
        yRot.readNBT(tag.get("yRot") as CompoundTag? ?: return, clientPacket)
        distance.readNBT(tag.get("distance") as CompoundTag? ?:return, clientPacket)

        currentStack = ItemStack.of(tag)
        midAirStack = ItemStack.of(tag)

        if (tag.contains("shootingAtChute")) shootingAtChute = NbtUtils.readBlockPos(tag.get("shootingAtChute") as? CompoundTag? ?: return)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.put("xRot", xRot.writeNBT())
        tag.put("yRot", yRot.writeNBT())
        tag.put("distance", distance.writeNBT())

        currentStack.save(tag)
        midAirStack.save(tag)

        if (shootingAtChute != null) tag.put("shootingAtChute",NbtUtils.writeBlockPos(shootingAtChute!!))
        super.write(tag, clientPacket)

    }



    class FrequencySlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(level, pos, state) else Vec3(.5, 10.5 / 16f, .5).add(
                VecHelper.rotate(
                    VecHelper.voxelSpace(0.0, 0.0, -5.0), angle(state).toDouble(), Direction.Axis.Y
                )
            )
        }

        override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
            if (direction != Direction.UP) {
                super.rotate(level, pos, state, ms)
                return
            }
            TransformStack.of(ms)
                .rotateYDegrees(angle(state))
                .rotateXDegrees(90.0f)
        }

        private fun angle(state: BlockState): Float {
            return if (ClockworkBlocks.DELIVERY_CANNON.has(state)) AngleHelper.horizontalAngle(state.getValue(HorizontalDirectionalBlock.FACING)) else 0f
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            return direction != Direction.UP && direction != Direction.DOWN  // This is atrocious, but it's 5 am and I don't care
                    && ((state.getValue(HorizontalDirectionalBlock.FACING)==Direction.NORTH || state.getValue(HorizontalDirectionalBlock.FACING)==Direction.SOUTH) && (direction == Direction.NORTH || direction == Direction.SOUTH)
                    || ((state.getValue(HorizontalDirectionalBlock.FACING)==Direction.WEST || state.getValue(HorizontalDirectionalBlock.FACING)==Direction.EAST) && (direction == Direction.WEST || direction == Direction.EAST)))
        }

        override fun getSouthLocation(): Vec3 {
            return if (direction == Direction.UP) Vec3.ZERO else VecHelper.voxelSpace(8.0, 3.0, 15.5)
        }
    }


    enum class DistributionMode(private val icon: AllIcons) : INamedIconOptions {
        ROUND_ROBIN(AllIcons.I_TUNNEL_ROUND_ROBIN),
        ALWAYS_CLOSEST(AllIcons.I_TUNNEL_PREFER_NEAREST),
        ;

        private val translationKey = "contraptions.movement_mode." + Lang.asId(name)

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }

}
