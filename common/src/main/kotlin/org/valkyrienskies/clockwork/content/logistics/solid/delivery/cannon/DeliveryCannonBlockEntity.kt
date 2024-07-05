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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.FrequencySlotBehaviour
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.blockToVec
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.euler_angle
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.get_Parabola_Y
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.get_delta
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DeliveryCannonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(type, pos,
    state
) {

    lateinit var capBelow: StorageProvider<ItemVariant>
    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour

    var currentStack: ItemStack = ItemStack.EMPTY

    var transportStack: ItemStack = ItemStack.EMPTY
    var progress: Double = 0.0
    var location = BlockPos.ZERO
    var distance: Double = 0.0


    var last = Vec3.ZERO
    var rotate = 0.0

    var xRotation = 0.0
    var yRotation = 0.0

    var xTargetRotation = 0.0
    var yTargetRotation = 0.0

    var xLastRotation = 0.0
    var yLastRotation = 0.0

    val turnSpeed = 1.5
    var shootingTicks = 0

    init {
        xRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
        xLastRotation = xTargetRotation
    }

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return


        val cap = grabCapability(Direction.DOWN) ?: return
        if (currentStack.isEmpty) currentStack  = ItemHelper.extract(cap, { true}, ItemHelper.ExtractionCountMode.UPTO, 64, false)


        val chute = ActiveChutes.getNearestChuteWithFrequency(blockPos,100.0,frequencySlotBehaviour.frequency)

        if (chute!=null && !currentStack.isEmpty && transportStack.isEmpty && shootingTicks == 0 ) {
            val be = level!!.getBlockEntity(chute) as DeliveryChuteBlockEntity
            val attempt = be.receiveItem(currentStack,true)
            if (attempt) {

                transportStack = currentStack.copy()
                currentStack = ItemStack.EMPTY
                location = chute
                distance = blockPos.distSqr(chute)
                be.isRecieving = true

            }
        }


        if (!transportStack.isEmpty) {
            getAngle()




            if (!ActiveChutes.hasChute(location)) {
                transportStack = ItemStack.EMPTY
                progress = 0.0

                return
            }

            if (xRotation==xTargetRotation && yRotation == yTargetRotation) {
                progress += max(-0.00001*distance + 0.05,0.001)
                if (shootingTicks<=6) shootingTicks+=1
            }

            if (progress >= 1 ) {
                val be = level!!.getBlockEntity(location) as DeliveryChuteBlockEntity
                be.receiveItem(transportStack,false)
                transportStack = ItemStack.EMPTY
                progress = 0.0
                be.isRecieving = false
                shootingTicks=0
            }
        } else {
            xTargetRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
            yTargetRotation = 0.0
        }

        xRotation +=  max(min(xTargetRotation-xRotation,turnSpeed),-turnSpeed)
        yRotation +=  max(min(yTargetRotation-yRotation,turnSpeed),-turnSpeed)

        sync()
    }

    fun getAngle() {

        val startVec = blockToVec(blockPos)
        val endVec = blockToVec(location)

        val delta = get_delta(this)
        val y = get_Parabola_Y(this, startVec.lerp(endVec,delta))

        val dif = startVec.subtract(startVec.lerp(endVec,delta).x,y,startVec.lerp(endVec,delta).z)
        xTargetRotation = euler_angle(dif.z,-dif.x)




        val otherV: Double
        if (abs(dif.z) > abs(dif.x)) otherV = dif.z
        else otherV =  dif.x
        var u_angle = euler_angle(dif.y,otherV)
        if (u_angle>90) u_angle=180-u_angle

        yTargetRotation = min(90.0,u_angle+20)
    }

    fun sync() {
        ClockworkPackets.sendToNear(level!!,blockPos,100,DeliveryCannonSyncPacket(transportStack,location, progress, xRotation ,yRotation, shootingTicks , blockPos))
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
        val be = provider.findBlockEntity()
        return provider[side.opposite]
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