package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.contraptions.TranslatingContraption
import com.simibubi.create.content.contraptions.bearing.BearingContraption
import com.simibubi.create.content.contraptions.bearing.StabilizedContraption
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.contraptions.chassis.StickerBlock
import com.simibubi.create.content.contraptions.gantry.GantryContraption
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity
import com.simibubi.create.content.contraptions.piston.PistonContraption
import com.simibubi.create.content.contraptions.pulley.PulleyContraption
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.SupportType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.mixin.accessors.IMixinPistonContraption
import org.valkyrienskies.clockwork.mixinduck.MixinAbstractContraptionEntityDuck
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.apigame.joints.VSFixedJoint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.mixin.mod_compat.create.blockentity.IMixinMechanicalBearingTileEntity
import org.valkyrienskies.mod.mixinducks.mod_compat.create.IMixinControlledContraptionEntity


class SlickerMovementBehavior : MovementBehaviour {

    var isStopped = true

    override fun renderAsNormalBlockEntity(): Boolean {
        return true
    }

    override fun tick(context: MovementContext) {
        if (context.world == null || context.world.isClientSide) return

        var data: CompoundTag = context.blockEntityData.getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)

        if (!data.isEmpty && data.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)) {
            if (!isStopped) {
                doUpdateConstraint(context, null, null)
            }
        } else {
            if (context.state.getValue(SlickerBlock.EXTENDED)) {
                context.blockEntityData.put(ClockworkConstants.Nbt.CONDENSED_DATA, CompoundTag())
                isAttachedToShipOrWorld(true, context.world as ServerLevel, context.localPos.toJOMLD(), context.rotation.apply(Vec3.atLowerCornerOf(context.state.getValue(
                    DirectionalBlock.FACING).normal)).toJOML(), context.blockEntityData.getCompound(ClockworkConstants.Nbt.CONDENSED_DATA))
            }
        }
    }

    fun getFacingAxis(context: MovementContext): Direction.Axis? {
        var axis: Direction.Axis? = null
        if (context.contraption is PistonContraption) axis =
            (context.contraption as IMixinPistonContraption).orientation.axis
        if (context.contraption is PulleyContraption) axis = Direction.Axis.Y
        if (context.contraption is GantryContraption) axis = (context.contraption as GantryContraption).facing.axis

        return axis
    }

    override fun onSpeedChanged(context: MovementContext, oldMotion: Vec3, motion: Vec3) {
        val axis: Direction.Axis? = getFacingAxis(context)
        isStopped = if (axis != null) {
            val axisMotion = Math.abs(VecHelper.getCoordinate(motion, axis))
            axisMotion < 0.001
        } else {
            motion == Vec3.ZERO
        }
        val a = Vector3d(1.0, 45.0, 1.0)
    }

    private fun getAssembleNextTick(context: MovementContext): Boolean {
        var result = false
        if (context.contraption.entity is ControlledContraptionEntity) {
            if (context.contraption is TranslatingContraption) {
                result =
                    ((context.contraption.entity as IMixinControlledContraptionEntity).grabController() as LinearActuatorBlockEntity).assembleNextTick
            }
            if (context.contraption is BearingContraption || context.contraption is StabilizedContraption) {
                result =
                    ((context.contraption.entity as IMixinControlledContraptionEntity).grabController() as IMixinMechanicalBearingTileEntity).isAssembleNextTick
            }
        }
        return result
    }

    override fun startMoving(context: MovementContext?) {
        isStopped = false
    }

    override fun stopMoving(context: MovementContext) {
        isStopped = true
        var position: Vector3d? = null
        var quaterniond: Quaterniond? = null
        val extraData: CompoundTag = context.blockEntityData.getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)
        var distance = DISTANCE_BUFFER
        if (extraData.contains(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE)) distance = extraData.getDouble(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE)
        val myDir = context.state.getValue(DirectionalBlock.FACING)
        val myDirNormal: Vec3 = (Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5)).toMinecraft()
        if (!getAssembleNextTick(context)) {
            if (context.state.getValue(BlockStateProperties.POWERED)) extraData.putBoolean(
                ClockworkConstants.Nbt.SHIP_STICKER_ALREADY_POWERED,
                true
            )
            val structureTransform: StructureTransform =
                (context.contraption.entity as MixinAbstractContraptionEntityDuck).structureTransform
            position =
                Vec3.atCenterOf(structureTransform.apply(context.localPos))
                    .add(structureTransform.applyWithoutOffsetUncentered(myDirNormal)).toJOML()

            if (distance < DISTANCE_BUFFER) {
                position.add(
                        structureTransform.applyWithoutOffsetUncentered(
                                Vec3.atLowerCornerOf(
                                    myDir.normal
                            ).scale(distance / -1 + DISTANCE_BUFFER)
                        ).toJOML()
                )
            }
            if (context.contraption is BearingContraption) {
                val tempQuat: Quaterniond = Vec3.atLowerCornerOf(structureTransform.applyWithoutOffset(context.localPos)).toJOML().rotationTo(
                            Vec3.atLowerCornerOf(context.localPos).toJOML(), Quaterniond())
                quaterniond = Quaterniond()
                tempQuat.mul(mapper.readValue(extraData.getByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT), VSFixedJoint::class.java).pose0.rot, quaterniond)
            }
        }
        if (!extraData.isEmpty) {
            if (extraData.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)) {
                doUpdateConstraint(context, position, quaterniond)
            }
        }
    }


    private fun doUpdateConstraint(context: MovementContext, ship1Pos: Vector3dc?, ship1Rot: Quaterniond?) {

        if (context.world.isClientSide) return

        var ship1: Ship? = null
        var ship2: Ship? = null
        var distance = DISTANCE_BUFFER

        var realShip1Pos: Vector3d? = ship1Pos as Vector3d?
        var realShip1Rot: Quaterniond? = ship1Rot

        val extraData: CompoundTag = context.blockEntityData.getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)

        if (extraData.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)) {
            var ship2Pos: Vector3d? = null
            var ship2Rot: Quaterniond? = null

            val attachConstraintData = extraData.getByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)
            val attachConstraint = mapper.readValue(attachConstraintData, VSFixedJoint::class.java)

            distance = 1.0
            ship1 = attachConstraint.shipId0?.let { context.world.shipObjectWorld.loadedShips.getById(it) }
            ship2 = attachConstraint.shipId1?.let { context.world.shipObjectWorld.loadedShips.getById(it) }
            ship2Pos = Vector3d(attachConstraint.pose1.pos)
            ship2Rot = Quaterniond(attachConstraint.pose1.rot)

            if (ship1 == null && ship2 == null) return

            val myDir = context.state.getValue(DirectionalBlock.FACING)
            var myDirNormal: Vec3 = if (ship1 != null && ship2 != null) {
                Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5).toMinecraft()
            } else {
                Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5).toMinecraft()
            }

            if (realShip1Pos == null) {
                realShip1Pos = Vector3d(attachConstraint.pose0.pos)
                if (context.contraption is StabilizedContraption) {
                    realShip1Pos.add(Vec3.atLowerCornerOf((context.contraption as StabilizedContraption).facing.normal).toJOML().mul(0.125))
                }
                realShip1Pos.add(context.motion.toJOML())

//                if (distance < DISTANCE_BUFFER) {
//                    realShip1Pos.add(context.rotation.apply(Vec3.atLowerCornerOf(myDir.normal)).toJOML())
//                }
            }
            if (realShip1Rot == null) {
                realShip1Rot = Quaterniond(attachConstraint.pose0.rot)
                val rotationState = context.contraption.entity.rotationState
                if (rotationState != null) {
                    realShip1Rot = Quaterniond().setFromNormalized(rotationState.asMatrix().asMatrix4f.toJOML()).mul(realShip1Rot)
                }
            }

            val constraintPair: VSFixedJoint? = makeConstraint(realShip1Pos, Vector3d(realShip1Pos), ship1, ship2, context.world as ServerLevel, realShip1Rot, ship2Rot, ship2Pos)

            if (constraintPair != null) {
                val attachConstraint2 = constraintPair
                (context.world as ServerLevel).shipObjectWorld.updateConstraint(extraData.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID), attachConstraint2)
                extraData.putInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID, extraData.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID))
                extraData.putByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT, mapper.writeValueAsBytes(attachConstraint2))
                extraData.putDouble(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE, distance)

                ClockworkMod.LOGGER.info("Updated constraint from ship ${attachConstraint2.shipId0} to ${attachConstraint2.shipId1} using points ${attachConstraint2.pose0.pos} && ${attachConstraint2.pose1.pos}")
            }
        }
    }

    companion object {
        val mapper = VSJacksonUtil.defaultMapper
        const val DISTANCE_BUFFER = 1.05

        fun isAttachedToShipOrWorld(
            attach: Boolean,
            level: ServerLevel?,
            myPosCentered: Vector3dc,
            myDirNormal: Vector3dc,
            compoundTag: CompoundTag
        ): Boolean {
            var result = false
            if (level == null) return false
            val ship: ServerShip? = level.getShipManagingPos(myPosCentered)
            var ship2: Ship? = null
            val tempDirNormal: Vector3d = myDirNormal.mul(.75, Vector3d())
            val searchPos: Vector3d = Vector3d(myPosCentered).add(tempDirNormal)
            ship?.shipToWorld?.transformPosition(searchPos, searchPos)
            var searchBlockPos: BlockPos = BlockPos(searchPos.toMinecraft())
            val worldBlockState: BlockState = level.getBlockState(searchBlockPos)
            var distance = 0.0
            if (!worldBlockState.isAir) {
                distance = Vector3d.distance(
                    myPosCentered.x(),
                    myPosCentered.y(),
                    myPosCentered.z(),
                    searchBlockPos.x.toDouble(),
                    searchBlockPos.y.toDouble(),
                    searchBlockPos.z.toDouble()
                )
                result = true
            } else {
                val bounds = 0.5
                val searchAABB = AABB(
                    searchPos.x - bounds, searchPos.y - bounds, searchPos.z - bounds,
                    searchPos.x + bounds, searchPos.y + bounds, searchPos.z + bounds
                )
                val ships: Iterator<Ship> = level.getShipsIntersecting(searchAABB).iterator()
                var shipItr: Ship
                val transformedSearchPos: Vector3dc = Vector3d(searchPos)
                if (ships.hasNext()) {
                    do {
                        shipItr = ships.next()
                        if (shipItr === ship) continue
                        val transformedPos = shipItr.worldToShip.transformPosition(transformedSearchPos, Vector3d())
                        val blockPos: BlockPos = BlockPos(transformedPos.toMinecraft())
                        if (level.isBlockInShipyard(blockPos)) {
                            val blockState: BlockState = level.getBlockState(blockPos)
                            if (!blockState.isAir && blockState.isFaceSturdy(
                                    level,
                                    blockPos,
                                    Direction.UP,
                                    SupportType.RIGID
                                )
                            ) {
                                searchBlockPos = BlockPos(
                                    shipItr.shipToWorld.transformPosition(
                                        blockPos.x.toDouble(),
                                        blockPos.y.toDouble(),
                                        blockPos.z.toDouble(),
                                        Vector3d()
                                    ).toMinecraft()
                                )
                                distance = Vector3d.distance(
                                    myPosCentered.x(),
                                    myPosCentered.y(),
                                    myPosCentered.z(),
                                    searchBlockPos.x.toDouble(),
                                    searchBlockPos.y.toDouble(),
                                    searchBlockPos.z.toDouble()
                                )
                                result = true
                                ship2 = shipItr
                            }
                        }
                    } while (ships.hasNext() && !result)
                }
            }
            if (result && !level.isClientSide && attach) doAttach(
                level as ServerLevel,
                ship,
                ship2,
                myPosCentered,
                myDirNormal,
                compoundTag,
                distance
            )
            return result
        }

        fun doAttach(level: ServerLevel, ship1: Ship?, ship2: Ship?, myPos: Vector3dc, myDirNormal: Vector3dc, tag: CompoundTag, distance: Double) {
            if (ship1 == null && ship2 == null) return

            //removeConstraint(level, false, tag)

            val adjustedDirNormal = Vector3d(myDirNormal).normalize().mul(1.0, Vector3d())

            val ship1Pos = Vector3d(myPos).add(adjustedDirNormal, Vector3d())
            val ship2ConstraintPos = Vector3d(ship1Pos)
//            if (distance < DISTANCE_BUFFER) {
//                ship1Pos.add(adjustedDirNormal.mul(distance / -1.0 + DISTANCE_BUFFER, ship1Pos))
//            }
            val ship2Pos: Vector3d? = null
            val ship1Rot: Quaterniond? = null
            val ship2Rot: Quaterniond? = null

            var adjustedDistance = distance
            var realShip1 = ship1
            var realShip2 = ship2
            var realShip1Pos = ship1Pos
            var realShip2Pos = ship2Pos
            var realShip1Rot = ship1Rot
            var realShip2Rot = ship2Rot

            if (tag.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID) && 
                tag.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)
                ) {

                val attachConstraintId = tag.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)
                val attachConstraintData = tag.getByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)
                val attachConstraint = mapper.readValue(attachConstraintData, VSFixedJoint::class.java)

                if ((level.shipObjectWorld.updateConstraint(attachConstraintId, attachConstraint))) return

                adjustedDistance = 1.0
                realShip1 = attachConstraint.shipId0?.let { level.shipObjectWorld.loadedShips.getById(it) }
                realShip2 = attachConstraint.shipId1?.let { level.shipObjectWorld.loadedShips.getById(it) }
                realShip1Pos = Vector3d(attachConstraint.pose0.pos)
                realShip2Pos = Vector3d(attachConstraint.pose1.pos)
                realShip1Rot = Quaterniond(attachConstraint.pose0.rot)
                realShip2Rot = Quaterniond(attachConstraint.pose1.rot)
            }

            val constraintPair: VSFixedJoint? = makeConstraint(realShip1Pos, ship2ConstraintPos, realShip1, realShip2, level, realShip1Rot, realShip2Rot, realShip2Pos)

            if (constraintPair != null) {
                val attachConstraint = constraintPair
                val attachConstraintId = level.shipObjectWorld.createNewConstraint(attachConstraint)

                tag.putInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID, attachConstraintId ?: -1)
                tag.putByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT, mapper.writeValueAsBytes(attachConstraint))

                tag.putDouble(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE, adjustedDistance)

                ClockworkMod.LOGGER.info("Attached to ship ${attachConstraint.shipId1} using points ${attachConstraint.pose0.pos} && ${attachConstraint.pose1.pos}")
            }
        }

        private fun makeConstraint(
            ship1ConstraintPos: Vector3d,
            ship2ConstraintPos: Vector3d,
            ship1: Ship?,
            ship2: Ship?,
            level: ServerLevel,
            ship1Rot: Quaterniond?,
            ship2Rot: Quaterniond?,
            ship2Pos: Vector3d?
        ): VSFixedJoint? {
            var realShip2ConstraintPos: Vector3dc = ship2ConstraintPos
            var realShip1Rot: Quaterniond? = ship1Rot
            var realShip2Rot: Quaterniond? = ship2Rot
            if (ship1 == null && ship2 == null) return null
            val ship1Id: Long
            val ship2Id: Long
            val groundId: Long = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
            if (ship1Rot == null && ship1 == null) realShip1Rot = Quaterniond()
            if (ship2Rot == null && ship2 == null) realShip2Rot = Quaterniond()
            var mass = 100.0
            if (ship1 != null) {
                //realShip2ConstraintPos = ship1.shipToWorld.transformPosition(realShip2ConstraintPos, Vector3d())
                ship1Id = ship1.id
                if (realShip1Rot == null) realShip1Rot = ship1.transform.shipToWorldRotation as Quaterniond
                mass = level.shipObjectWorld.loadedShips.getById(ship1Id)!!.inertiaData.mass
            } else ship1Id = groundId
            if (ship2 != null) {
                realShip2ConstraintPos = ship2.worldToShip.transformPosition(level.toWorldCoordinates(Vector3d(ship1ConstraintPos)), Vector3d())
                ship2Id = ship2.id
                if (realShip2Rot == null) realShip2Rot = ship2.transform.shipToWorldRotation as Quaterniond
                //ship2Rot.add(ship2.getTransform().getShipToWorldRotation());
                mass = level.shipObjectWorld.loadedShips.getById(ship2Id)!!.inertiaData.mass
            } else ship2Id = groundId
            if (ship2Pos != null) realShip2ConstraintPos = ship2Pos
            val ship1Rotation: Quaterniondc = realShip1Rot?: Quaterniond()
            val ship2Rotation: Quaterniondc = realShip2Rot?: Quaterniond()

            val attachConstraint = VSFixedJoint(
                ship1Id,
                pose0 = VSJointPose(ship1ConstraintPos, ship1Rotation),
                ship2Id,
                pose1 = VSJointPose(realShip2ConstraintPos, ship2Rotation),
                VSJointMaxForceTorque(1.0E10F, 1.0E10F)
            )

            return (attachConstraint)
        }


        fun removeConstraint(level: ServerLevel, removeTags: Boolean, compoundTag: CompoundTag) {
            if (compoundTag.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)) {
                level.shipObjectWorld.removeConstraint(compoundTag.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID))

                if (removeTags) {
                    compoundTag.remove(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)
                    compoundTag.remove(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)
                }
            }
        }
    }
}