package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import dev.architectury.injectables.annotations.ExpectPlatform
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.lang.Lang
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.euler_angle
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer.Companion.getThirdPoint
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotBehaviour
import org.valkyrienskies.core.api.VsCoreApi
import org.valkyrienskies.mod.api.VsApi
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.api.shipWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DeliveryCannonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(type, pos, state), IHaveGoggleInformation {

    //    var inventory: Any? = null
    lateinit var frequencySlotBehaviour: FrequencySlotBehaviour
    lateinit var distributionModeBehaviour: ScrollOptionBehaviour<DistributionMode>

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)

        frequencySlotBehaviour = FrequencySlotBehaviour(this, FrequencySlot())
        behaviours.add(frequencySlotBehaviour)

        distributionModeBehaviour = ScrollOptionBehaviour<DistributionMode>(
            DistributionMode::class.java, ClockworkLang.translateDirect("contraptions.movement_mode"),
            this, FrequencySlot()
        )

        distributionModeBehaviour.requiresWrench()

        behaviours.add(frequencySlotBehaviour)
        behaviours.add(distributionModeBehaviour)


    }

    var currentStack: ItemStack = ItemStack.EMPTY
    var midAirStack    : ItemStack = ItemStack.EMPTY
    var didParticles = false
    var fired = false

    var lastVel = 0.0
    var visitedChutes = HashSet<BlockPos>()

    var cooldown = 0.0
    var gunpowderTicks = 0.0
    var shootingAtChute: BlockPos? = null

    private var xRot = LerpedFloat.angular()
    private var yRot = LerpedFloat.angular()
    private var distance = LerpedFloat.linear()

    val realPos: Vector3d? get()
    { return vsApi.getShipManagingBlock(level, blockPos)?.positionToWorld(blockPos.toJOMLD()) ?: blockPos.toJOMLD() }

    init {
        xRot.setValue(blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble())
    }

    override fun tick() {
        super.tick()



        xRot.tickChaser()
        yRot.tickChaser()
        distance.tickChaser()
        cooldown = max(cooldown, cooldown-1)
        gunpowderTicks = max(gunpowderTicks, gunpowderTicks-1)

        realPos ?: return
        if (level!!.isClientSide) return


        if (shootingAtChute != null) {
            val chute = ActiveChutes.actives[shootingAtChute]
            if (chute == null && ActiveChutes.unloaded[shootingAtChute] == null) shootingAtChute = null
            return
        }


        if (midAirStack.isEmpty && !currentStack.isEmpty) {
            // TODO: CONFIGURE MAX DISTANCE
            val chutes = ActiveChutes.getSortedChuteWithFrequency(realPos!!,100.0,frequencySlotBehaviour.frequency)
            var chute: BlockPos? = null
            if (distributionModeBehaviour.get() == DistributionMode.ALWAYS_CLOSEST)  chute = chutes[0]
            else {
                for (possibleChute in chutes) {
                    if (possibleChute in visitedChutes) continue
                    chute = possibleChute
                    visitedChutes.add(chute)

                    break
                }

                if (chute == null) visitedChutes.clear()
            }

            chute ?: return

            if (abs(xRot.value-xRot.chaseTarget) < 1 && abs(yRot.value-yRot.chaseTarget) < 1) {
                midAirStack = currentStack
                currentStack = ItemStack.EMPTY
                distance.updateChaseTarget(realPos!!.distance(ActiveChutes.actives[chute]!!.realPos).toFloat())
            }
        }


    }

    fun updateAngleChaser(chuteBlockEntity: DeliveryChuteBlockEntity) {

        val vertex = getThirdPoint(realPos!!, chuteBlockEntity.realPos!!)
        val deltaP = realPos!!.sub(vertex)

        xRot.updateChaseTarget(euler_angle(deltaP.z,-deltaP.x).toFloat())

        val otherV: Double
        if (abs(deltaP.z) > abs(deltaP.x)) otherV = deltaP.z
        else otherV =  deltaP.x
        var u_angle = euler_angle(deltaP.y,otherV)
        if (u_angle>90) u_angle=180-u_angle

        yRot.updateChaseTarget(min(90.0,u_angle+20).toFloat())
    }

    companion object {
        @ExpectPlatform
        fun extractFrom(level: Level): ItemStack {
            throw AssertionError()
        }
    }

//
//    val soundRandom = RandomSource.create()
//
//    var currentStack: ItemStack = ItemStack.EMPTY
//
//    var transportStack: ItemStack = ItemStack.EMPTY
//    var progress: Double = 0.0
//    var clientProgress: Double = 0.0
//    var maxProgress: Double = 0.0
//    var chuteLocation: BlockPos = BlockPos.ZERO
//    var realLocation: Vec3 = Vec3.ZERO
//    var distance: Double = 0.0
//    var lastDistance: Double = 1.0
//
//
//    var itemRotation = 0.0
//
//    var didParticles = false
//    var fired = false
//
//    var xRotation = 0.0
//    var yRotation = 0.0
//
//    var xTargetRotation = 0.0
//    var yTargetRotation = 0.0
//
//    var xLastRotation = 0.0
//    var yLastRotation = 0.0
//
//    var clientShotProgress = 0.0
//
//    var clientBarrelOffset = 0.0f
//    var clientCannonRotationOffset = 0.0f
//    var clientAntennaRotationOffset = 0.0f
//
//    val velocityThreshold = 5.0
//    var lastVelocity = Vector3d(0.0,0.0,0.0)
//
//    var cooldown: Int = 0
//    var gunPowderTicks: Int = 0
//
//    var roundRobin = true
//
//    var visitedChutes = HashSet<BlockPos>()
//    var ponder = false
//
//    init {
//        xTargetRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
//        xRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
//        xLastRotation = xTargetRotation
//
//    }
//
//    override fun tick() {
//        super.tick()
//
//
//        if (level!!.isClientSide && !ponder ) return
//        cooldown=max(0,cooldown-1)
//        gunPowderTicks=max(0,gunPowderTicks-1)
//        val mult = if(gunPowderTicks>0) 3 else 1
//
//        val cap = grabCapability(Direction.DOWN) ?: return
//        if (currentStack.isEmpty) currentStack  = ItemHelper.extract(cap, { true}, ItemHelper.ExtractionCountMode.UPTO, 64, false)
//
//        if (!currentStack.isEmpty && transportStack.isEmpty) {
//            val chutes = ActiveChutes.getSortedChuteWithFrequency(getRealPos(),100.0,frequencySlotBehaviour.frequency)
//
//            if (chutes.isNotEmpty()) {
//
//                if (visitedChutes.size == chutes.size) visitedChutes = HashSet()
//                for (chute in chutes) {
//                    if (!roundRobin || chute !in visitedChutes) {
//                        visitedChutes.add(chute)
//                        if (startAiming(chute)) break
//                    }
//
//                }
//
//            }
//        }
//
//
//        if (!transportStack.isEmpty) {
//            getAngle()
//            if (!ponder && !ActiveChutes.hasChute(chuteLocation)) {
//                end(false)
//
//                return
//            }
//
//            if (!ponder && ActiveChutes.getChutes()[chuteLocation]!!.isOnShip()) {
//                if (getChuteVelocity().sub(lastVelocity).length()>velocityThreshold) {
//                    val lerped = getRealPos().lerp(realLocation,progress)
//                    val item = ItemEntity(level!!, lerped.x, getParabolaY(this, lerped), lerped.z, transportStack)
//
//                    item.deltaMovement = getRealPos().lerp(realLocation,(progress+1)/maxProgress ).subtract(lerped)
//                    item.setDefaultPickUpDelay()
//                    level!!.addFreshEntity(item)
//
//                    return end(true)
//                }
//
//                lastVelocity = getChuteVelocity()
//            }
//
//            if (!ponder) realLocation = ActiveChutes.getChuteRealPos(chuteLocation)!!
//            distance = getRealPos().distanceToSqr(realLocation)
//
//
//            if ((obstructionChecker(chuteLocation,realLocation) && abs(xTargetRotation-xRotation) < 1 && abs(yTargetRotation-yRotation)< 0.5) || fired) {
//                if (!fired) {
//                    val pitch = Mth.randomBetween(soundRandom, 0.9f, 1.1f)
//                    level!!.playSound(null, blockPos, ClockworkSounds.THWOOM.mainEvent!!, SoundSource.BLOCKS, 1f,pitch)
//                    fired = true
//                }
//
//                lastDistance = distance
//
//                maxProgress = (15+distance*0.05)/mult
//                progress += 1
//
//
//                if (progress >= maxProgress ) {
//                    val be = level!!.getBlockEntity(chuteLocation) as DeliveryChuteBlockEntity
//                    be.receiveItem(transportStack,false)
//
//                    be.isRecieving = false
//
//                    end(false)
//                }
//            }
//        } else {
//            xTargetRotation = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot().toDouble()
//            yTargetRotation = 0.0
//        }
//
//        xRotation =  turn(xRotation, xTargetRotation, 3.0*mult).first
//        yRotation =  turn(yRotation, yTargetRotation, 2.1*mult).first
//
//
//        if (isVirtual) sync()
//    }
//
//    fun obstructionChecker(chuteBlockLocation: BlockPos, chuteRealLocation: Vec3): Boolean {
//        val vertex = getVertexOfParabola(chuteRealLocation)
//
//        val cannonToVertex = clip(getRealPos().add(0.0,0.5,0.0), vertex)
//        if (cannonToVertex.type != HitResult.Type.MISS) return false // returns if path to parabola vertex is obstructed
//
//        val vertexToChute = clip(vertex, chuteRealLocation)
//        return !(vertexToChute.type == HitResult.Type.MISS || vertexToChute.blockPos != chuteBlockLocation)
//
//    }
//
//    fun clip(from: Vec3, to: Vec3): BlockHitResult {
//        return level!!.clipIncludeShips(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
//    }
//
//    fun startAiming(chute: BlockPos): Boolean {
//
//        if (level!!.getBlockEntity(chute) == null || level!!.getBlockEntity(chute) !is DeliveryChuteBlockEntity) return false
//        val be = level!!.getBlockEntity(chute) as DeliveryChuteBlockEntity
//
//        val attempt = be.receiveItem(currentStack,true)
//        val obs = obstructionChecker(chute, ActiveChutes.getChuteRealPos(chute)!!)
//
//        if (obs && attempt) {
//            transportStack = currentStack.copy()
//            currentStack = ItemStack.EMPTY
//            chuteLocation = chute
//            realLocation = ActiveChutes.getChuteRealPos(chute)!!
//            be.isRecieving = true
//            lastVelocity = getChuteVelocity()
//
//            distance = getRealPos().distanceToSqr(realLocation)
//            return true
//        }
//
//        return false
//    }
//
//    fun addGunpowderTicks(count: Int) {
//        gunPowderTicks += count*6000
//    }
//
//    fun getRealPos(): Vec3 {
//        if (level.getShipManagingPos(blockPos) != null) {
//            val temp = level.getShipManagingPos(blockPos)!!.shipToWorld.transformPosition(Vector3d(blockPos.x.toDouble()+0.5,blockPos.y.toDouble()+0.75,blockPos.z.toDouble()+0.5))
//            return Vec3(temp.x,temp.y,temp.z)
//        } else return Vec3(blockPos.x.toDouble()+0.5,blockPos.y.toDouble()+0.75,blockPos.z.toDouble()+0.5)
//    }
//
//    fun end(has_cooldown: Boolean) {
//        transportStack = ItemStack.EMPTY
//        progress = 0.0
//        maxProgress = 0.0
//        distance = 0.0
//        fired = false
//
//        if (has_cooldown) cooldown = 10
//
//    }
//
//    fun getChuteVelocity(): Vector3d {
//        return if (ActiveChutes.getChutes()[chuteLocation]!!.isOnShip()) ActiveChutes.getChutes()[chuteLocation]!!.getVelocity()!!.get(Vector3d(0.0,0.0,0.0))
//            else Vector3d(0.0,0.0,0.0)
//    }
//
//    fun getAngle() {
//
//        val startVec = getRealPos()
//
//
//        var dif = startVec.subtract(getVertexOfParabola(realLocation))
//
//        val ship = level!!.getShipManagingPos(blockPos)
//        if (ship!=null) {
//            val temp = ship.worldToShip.transformDirection(Vector3d(dif.x,dif.y,dif.z))
//            dif = Vec3(temp.x,temp.y,temp.z)
//        }
//
//        xTargetRotation = euler_angle(dif.z,-dif.x)
//
//        val otherV: Double
//        if (abs(dif.z) > abs(dif.x)) otherV = dif.z
//        else otherV =  dif.x
//        var u_angle = euler_angle(dif.y,otherV)
//        if (u_angle>90) u_angle=180-u_angle
//
//        yTargetRotation = min(90.0,u_angle+20)
//    }
//
//    fun getVertexOfParabola(endVec: Vec3): Vec3 {
//        return getThirdPoint(getRealPos(), endVec)
//    }
//
//    fun sync() {
//        ClockworkPackets.sendToNear(level!!,blockPos,100,DeliveryCannonSyncPacket(currentStack, transportStack, realLocation, maxProgress, xRotation ,yRotation , blockPos, xTargetRotation, yTargetRotation, gunPowderTicks))
//    }
//
//    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
//        super.addBehaviours(behaviours)
//
//        frequencySlotBehaviour = FrequencySlotBehaviour(this, FrequencySlot())
//        behaviours.add(frequencySlotBehaviour)
//
//        distributionModeBehaviour = ScrollOptionBehaviour<DistributionMode>(
//            DistributionMode::class.java, CreateLang.translateDirect("contraptions.movement_mode"),
//            this, FrequencySlot()
//        )
//        distributionModeBehaviour.withCallback { t: Int? -> roundRobin = t==0 }
//        distributionModeBehaviour.requiresWrench()
//        behaviours.add(distributionModeBehaviour)
//
//
//
//        return
//    }
//
//
//    override fun setLevel(level: Level) {
//        super.setLevel(level)
//        capBelow = StorageProvider.createForItems(level, worldPosition.below())
//    }
//
//    @ExpectPlatform
//    fun setLevel(level: Level, pos: BlockPos): StorageProvider<ItemVariant?> {
//        val cache = (level as LevelExtensions).`port_lib$getItemCache`(pos)
//        if (cache is EmptyItemLookupCache) return StorageProvider.create<ItemVariant?>(ItemStorage.SIDED, level, pos)
//        return StorageProvider.create<ItemVariant?>(cache, level)
//    }
//
//    private fun grabCapability(side: Direction): Storage<ItemVariant>? {
//        if (level == null) return null
//        val provider: StorageProvider<ItemVariant> = capBelow
//        return provider[side.opposite]
//    }
//
//
//    override fun write(compound: CompoundTag, clientPacket: Boolean) {
//        super.write(compound, clientPacket)
//        val currentStack = currentStack.copy()
//        val transportStack = transportStack.copy()
//        val progress = progress
//        val location = chuteLocation
//        val xRotation = xRotation
//        val yRotation = yRotation
//
//        compound.put("currentStack",currentStack.save(CompoundTag()))
//        compound.put("transportStack",transportStack.save(CompoundTag()))
//        compound.putDouble("progress",progress)
//        compound.putInt("locationX",location.x)
//        compound.putInt("locationY",location.y)
//        compound.putInt("locationZ",location.z)
//        compound.putDouble("rotationX",xRotation)
//        compound.putDouble("rotationY",yRotation)
//        compound.putInt("gunPowderTicks",gunPowderTicks)
//    }
//
//    override fun read(compound: CompoundTag, clientPacket: Boolean) {
//        super.read(compound, clientPacket)
//
//        currentStack = ItemStack.of(compound.getCompound("currentStack"))
//        transportStack = ItemStack.of(compound.getCompound("transportStack"))
//        progress = compound.getDouble("progress")
//        chuteLocation = BlockPos(compound.getInt("locationX"), compound.getInt("locationY"), compound.getInt("locationZ"))
//        xRotation = compound.getDouble("rotationX")
//        yRotation = compound.getDouble("rotationY")
//        gunPowderTicks = compound.getInt("gunPowderTicks")
//
//        xLastRotation = xRotation
//        yLastRotation = yRotation
//    }
//
//    fun ponderFire(chute: BlockPos) {
//        transportStack = currentStack.copy()
//        currentStack = ItemStack.EMPTY
//        chuteLocation = chute
//        realLocation = Vec3(chuteLocation.x+0.5,chuteLocation.y+0.95,chuteLocation.z+0.5)
//
//        distance = getRealPos().distanceToSqr(realLocation)
//    }
//
//    override fun addToGoggleTooltip(tooltip: MutableList<Component?>, isPlayerSneaking: Boolean): Boolean {
//
//
//        var shouldShow = false
//
//        tooltip.add(Component.literal("     Delivery Cannon Information").withStyle(ChatFormatting.WHITE))
//        if (!currentStack.isEmpty) {
//            ClockworkLang.translate(
//                "tooltip.chute.contains",
//                Component.translatable(currentStack.descriptionId).string,
//                currentStack.count
//            )
//                .style(ChatFormatting.GREEN)
//                .forGoggles(tooltip)
//            shouldShow = true
//        }
//        if (gunPowderTicks>0) {
//
//            tooltip.add(Component.literal((gunPowderTicks/1200).toString() + "m " + (gunPowderTicks/20%60).toString() + "s of gunpowder left")
//                .withStyle(ChatFormatting.GOLD))
//
//            shouldShow = true
//        }
//
//
//        return shouldShow
//    }



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
