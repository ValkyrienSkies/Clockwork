package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.logistics.depot.EjectorBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.item.ItemHelper
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.VecHelper
import io.github.fabricators_of_create.porting_lib.transfer.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.blockToVec
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.euler_angle
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.get_Parabola_Y
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.get_delta
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.turn
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotBehaviour
import org.valkyrienskies.mod.common.getShipManagingPos
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DeliveryCannonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(type, pos,
    state
) {

    lateinit var capBelow: StorageProvider<ItemVariant>
    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour

    val soundRandom = Random()

    var currentStack: ItemStack = ItemStack.EMPTY

    var transportStack: ItemStack = ItemStack.EMPTY
    var progress: Double = 0.0
    var chuteLocation: BlockPos = BlockPos.ZERO
    var realLocation: Vec3 = Vec3.ZERO
    var distance: Double = 0.0
    var lastDistance: Double = 1.0

    var itemLastPos = Vec3.ZERO
    var itemRotation = 0.0

    var didParticles = false
    var fired = false

    var xRotation = 0.0
    var yRotation = 0.0

    var xTargetRotation = 0.0
    var yTargetRotation = 0.0

    var xLastRotation = 0.0
    var yLastRotation = 0.0

    var clientShotProgress = 0.0

    var clientBarrelOffset = 0.0f
    var clientCannonRotationOffset = 0.0f
    var clientAntennaRotationOffset = 0.0f

    val velocityThreshold = 5.0
    var lastVelocity = Vector3d(0.0,0.0,0.0)

    var cooldown: Int = 0

    init {
        xTargetRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
        xRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
        xLastRotation = xTargetRotation
    }

    override fun tick() {
        super.tick()
        cooldown=max(0,cooldown-1)


        if (level!!.isClientSide) return

        val cap = grabCapability(Direction.DOWN) ?: return
        if (currentStack.isEmpty) currentStack  = ItemHelper.extract(cap, { true}, ItemHelper.ExtractionCountMode.UPTO, 64, false)


        val chute = ActiveChutes.getNearestChuteWithFrequency(getRealPos(),100.0,frequencySlotBehaviour.frequency)

        if (chute!=null && !currentStack.isEmpty && transportStack.isEmpty ) {
            if (level!!.getBlockEntity(chute) == null || level!!.getBlockEntity(chute) !is DeliveryChuteBlockEntity) return
            val be = level!!.getBlockEntity(chute) as DeliveryChuteBlockEntity

            val attempt = be.receiveItem(currentStack,true)
            if (attempt && cooldown == 0) {
                transportStack = currentStack.copy()
                currentStack = ItemStack.EMPTY
                chuteLocation = chute
                realLocation = ActiveChutes.getChuteRealPos(chute)!!
                be.isRecieving = true
                lastVelocity = getChuteVelocity()
            }
        }


        if (!transportStack.isEmpty) {
            getAngle()




            if (!ActiveChutes.hasChute(chuteLocation)) {
                end(false)

                return
            }

            if (ActiveChutes.getChutes()[chuteLocation]!!.isOnShip()) {
                if (getChuteVelocity().sub(lastVelocity).length()>velocityThreshold) {
                    val lerped = getRealPos().lerp(realLocation,progress)
                    val item = ItemEntity(level, lerped.x, get_Parabola_Y(this, lerped), lerped.z, transportStack)

                    item.deltaMovement = getRealPos().lerp(realLocation,progress -0.00001*distance + 0.05).subtract(lerped)
                    item.setDefaultPickUpDelay()
                    level!!.addFreshEntity(item)

                    end(true)

                    return
                }


                lastVelocity = getChuteVelocity()
            }

            realLocation = ActiveChutes.getChuteRealPos(chuteLocation)!!
            distance = getRealPos().distanceToSqr(realLocation)




            if ((abs(xTargetRotation-xRotation) < 1 && abs(yTargetRotation-yRotation)< 0.5) || fired) {
                if (!fired) {
                    val pitch = Mth.randomBetween(soundRandom, 0.9f, 1.1f)
                    level!!.playSound(null, blockPos, ClockworkSounds.THWOOM.mainEvent!!, SoundSource.BLOCKS, 1f,pitch)
                    fired = true
                }
                progress = progress * max(distance/lastDistance,1.0)
                lastDistance = distance
                progress += max(-0.00001*distance + 0.05,0.001)
            }

            if (progress >= 1 ) {
                val be = level!!.getBlockEntity(chuteLocation) as DeliveryChuteBlockEntity
                be.receiveItem(transportStack,false)

                be.isRecieving = false

                end(false)
            }
        } else {
            xTargetRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
            yTargetRotation = 0.0
        }

        xRotation =  turn(xRotation, xTargetRotation, 3.0).first
        yRotation =  turn(yRotation, yTargetRotation, 2.1).first



        sync()
    }

    fun getRealPos(): Vec3 {
        if (level.getShipManagingPos(blockPos) != null) {
            val temp = level.getShipManagingPos(blockPos)!!.shipToWorld.transformPosition(Vector3d(blockPos.x.toDouble(),blockPos.y.toDouble(),blockPos.z.toDouble()))
            return Vec3(temp.x,temp.y,temp.z)
        } else return blockToVec(blockPos)
    }

    fun end(has_cooldown: Boolean) {
        transportStack = ItemStack.EMPTY
        progress = 0.0
        distance = 0.0
        fired = false

        if (has_cooldown) cooldown = 10

    }

    fun getChuteVelocity(): Vector3d {
        return if (ActiveChutes.getChutes()[chuteLocation]!!.isOnShip()) ActiveChutes.getChutes()[chuteLocation]!!.getVelocity()!!.get(Vector3d(0.0,0.0,0.0))
            else Vector3d(0.0,0.0,0.0)
    }

    fun getAngle() {

        val startVec = getRealPos()
        val endVec = realLocation

        val delta = get_delta(this)
        val y = get_Parabola_Y(this, startVec.lerp(endVec,delta))

        var dif = startVec.subtract(startVec.lerp(endVec,delta).x,y,startVec.lerp(endVec,delta).z)

        val ship = level!!.getShipManagingPos(blockPos)
        if (ship!=null) {
            val temp = ship.worldToShip.transformDirection(Vector3d(dif.x,dif.y,dif.z))
            dif = Vec3(temp.x,temp.y,temp.z)
        }

        xTargetRotation = euler_angle(dif.z,-dif.x)

        val otherV: Double
        if (abs(dif.z) > abs(dif.x)) otherV = dif.z
        else otherV =  dif.x
        var u_angle = euler_angle(dif.y,otherV)
        if (u_angle>90) u_angle=180-u_angle

        yTargetRotation = min(90.0,u_angle+20)



    }

    fun sync() {
        ClockworkPackets.sendToNear(level!!,blockPos,100,DeliveryCannonSyncPacket(transportStack, realLocation, progress, xRotation ,yRotation , blockPos, xTargetRotation, yTargetRotation))
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        frequencySlotBehaviour = FrequencySlotBehaviour(this, FrequencySlot())

        behaviours.add(frequencySlotBehaviour)
        super.addBehaviours(behaviours)
        return
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        capBelow = StorageProvider.createForItems(level, worldPosition.below())
    }

    private fun grabCapability(side: Direction): Storage<ItemVariant>? {
        if (level == null) return null
        val provider: StorageProvider<ItemVariant> = capBelow
        return provider[side.opposite]
    }


    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        val currentStack = currentStack.copy()
        val transportStack = transportStack.copy()
        val progress = progress
        val location = chuteLocation
        val xRotation = xRotation
        val yRotation = yRotation

        compound.put("currentStack",currentStack.save(CompoundTag()))
        compound.put("transportStack",transportStack.save(CompoundTag()))
        compound.putDouble("progress",progress)
        compound.putInt("locationX",location.x)
        compound.putInt("locationY",location.y)
        compound.putInt("locationZ",location.z)
        compound.putDouble("rotationX",xRotation)
        compound.putDouble("rotationY",yRotation)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)

        currentStack = ItemStack.of(compound.getCompound("currentStack"))
        transportStack = ItemStack.of(compound.getCompound("transportStack"))
        progress = compound.getDouble("progress")
        chuteLocation = BlockPos(compound.getInt("locationX"), compound.getInt("locationY"), compound.getInt("locationZ"))
        xRotation = compound.getDouble("rotationX")
        yRotation = compound.getDouble("rotationY")
    }


    public class FrequencySlot : ValueBoxTransform.Sided() {
        override fun getLocalOffset(state: BlockState): Vec3 {
            return if (direction != Direction.UP) super.getLocalOffset(state) else Vec3(.5, 10.5 / 16f, .5).add(
                VecHelper.rotate(
                    VecHelper.voxelSpace(0.0, 0.0, -5.0), angle(state).toDouble(), Direction.Axis.Y
                )
            )
        }

        override fun rotate(state: BlockState, ms: PoseStack) {
            if (direction != Direction.UP) {
                super.rotate(state, ms)
                return
            }
            TransformStack.cast(ms)
                .rotateY(angle(state).toDouble())
                .rotateX(90.0)
        }

        private fun angle(state: BlockState): Float {
            return if (AllBlocks.WEIGHTED_EJECTOR.has(state)) AngleHelper.horizontalAngle(state.getValue(EjectorBlock.HORIZONTAL_FACING)) else 0f
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


}