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
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
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

        var data: CompoundTag = context.blockEntityData.getCompound("CondensedData")

        if (!data.isEmpty && data.contains("AttachmentConstraint") && data.contains("OrientationConstraint")) {
            if (!isStopped) {
                doUpdateConstraint(context, null, null)
            }
        } else {
            if (context.state.getValue(SlickerBlock.EXTENDED)) {
                context.blockEntityData.put("CondensedData", CompoundTag())
                isAttachedToShipOrWorld(true, context.world as ServerLevel, context.localPos.toJOMLD(), context.rotation.apply(Vec3.atLowerCornerOf(context.state.getValue(
                    DirectionalBlock.FACING).normal)).toJOML(), context.blockEntityData.getCompound("CondensedData"))
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
        val extraData: CompoundTag = context.blockEntityData.getCompound("CondensedData")
        var distance = DISTANCE_BUFFER
        if (extraData.contains("ShipStickerDistance")) distance = extraData.getDouble("ShipStickerDistance")
        val myDir = context.state.getValue(DirectionalBlock.FACING)
        val myDirNormal: Vec3 = (Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5)).toMinecraft()
        if (!getAssembleNextTick(context)) {
            if (context.state.getValue(BlockStateProperties.POWERED)) extraData.putBoolean(
                "ShipStickerAlreadyPowered",
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
                tempQuat.mul(mapper.readValue(extraData.getByteArray("OrientationConstraint"), VSFixedOrientationConstraint::class.java).localRot0, quaterniond)
            }
        }
        if (!extraData.isEmpty) {
            if (extraData.contains("AttachmentConstraintId") && extraData.contains("OrientationConstraintId")) {
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

        val extraData: CompoundTag = context.blockEntityData.getCompound("CondensedData")

        if (extraData.contains("AttachmentConstraintId") && extraData.contains("OrientationConstraintId")) {
            var ship2Pos: Vector3d? = null
            var ship2Rot: Quaterniond? = null

            val attachConstraintData = extraData.getByteArray("AttachmentConstraint")
            val attachConstraint = mapper.readValue(attachConstraintData, VSAttachmentConstraint::class.java)
            val orientationConstraintData = extraData.getByteArray("OrientationConstraint")
            val orientationConstraint = mapper.readValue(orientationConstraintData, VSFixedOrientationConstraint::class.java)

            distance = attachConstraint.fixedDistance
            ship1 = context.world.shipObjectWorld.allShips.getById(attachConstraint.shipId0)
            ship2 = context.world.shipObjectWorld.allShips.getById(attachConstraint.shipId1)
            ship2Pos = Vector3d(attachConstraint.localPos1)
            ship2Rot = Quaterniond(orientationConstraint.localRot1)

            if (ship1 == null && ship2 == null) return

            val myDir = context.state.getValue(DirectionalBlock.FACING)
            var myDirNormal: Vec3 = if (ship1 != null && ship2 != null) {
                Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5).toMinecraft()
            } else {
                Vec3.atLowerCornerOf(myDir.normal).toJOML().mul(.5).toMinecraft()
            }

            if (realShip1Pos == null) {
                realShip1Pos = Vector3d(attachConstraint.localPos0)
                if (context.contraption is StabilizedContraption) {
                    realShip1Pos.add(Vec3.atLowerCornerOf((context.contraption as StabilizedContraption).facing.normal).toJOML().mul(0.125))
                }
                realShip1Pos.add(context.motion.toJOML())

                if (distance < DISTANCE_BUFFER) {
                    realShip1Pos.add(context.rotation.apply(Vec3.atLowerCornerOf(myDir.normal)).toJOML().mul(distance / -1 + DISTANCE_BUFFER))
                }
            }
            if (realShip1Rot == null) {
                realShip1Rot = Quaterniond(orientationConstraint.localRot0)
                val rotationState = context.contraption.entity.rotationState
                if (rotationState != null) {
                    realShip1Rot = Quaterniond().setFromNormalized(rotationState.asMatrix().asMatrix4f.toJOML()).mul(realShip1Rot)
                }
            }

            val constraintPair: Pair<VSAttachmentConstraint, VSFixedOrientationConstraint>? = makeConstraint(realShip1Pos, Vector3d(realShip1Pos), ship1, ship2, context.world as ServerLevel, realShip1Rot, ship2Rot, ship2Pos)

            if (constraintPair != null) {
                val attachConstraint2 = constraintPair.first
                val orientationConstraint2 = constraintPair.second
                (context.world as ServerLevel).shipObjectWorld.updateConstraint(extraData.getInt("AttachmentConstraintId"), attachConstraint2)
                (context.world as ServerLevel).shipObjectWorld.updateConstraint(extraData.getInt("OrientationConstraintId"), orientationConstraint2)
                extraData.putInt("AttachmentConstraintId", extraData.getInt("AttachmentConstraintId"))
                extraData.putByteArray("AttachmentConstraint", mapper.writeValueAsBytes(attachConstraint2))
                extraData.putInt("OrientationConstraintId", extraData.getInt("OrientationConstraintId"))
                extraData.putByteArray("OrientationConstraint", mapper.writeValueAsBytes(orientationConstraint2))
                extraData.putDouble("ShipStickerDistance", distance)

                ClockworkMod.LOGGER.info("Updated constraint from ship ${attachConstraint2.shipId0} to ${attachConstraint2.shipId1} using points ${attachConstraint2.localPos0} && ${attachConstraint2.localPos1}")
            }
        }
    }

    companion object {
        val mapper = VSJacksonUtil.defaultMapper
        const val DISTANCE_BUFFER = 1.05

        fun isAttachedToShipOrWorld(
            attach: Boolean,
            level: ServerLevel,
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
                val transformedSearchPos = Vector3d(searchPos)
                if (ships.hasNext()) {
                    do {
                        shipItr = ships.next()
                        if (shipItr === ship) continue
                        shipItr.worldToShip.transformPosition(transformedSearchPos)
                        val blockPos: BlockPos = BlockPos(transformedSearchPos.toMinecraft())
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

            removeConstraint(level, false, tag)

            val adjustedDirNormal = Vector3d(myDirNormal).mul(distance, Vector3d())

            val ship1Pos = Vector3d(myPos).add(adjustedDirNormal, Vector3d())
            var ship2ConstraintPos = Vector3d(ship1Pos)
            if (distance < DISTANCE_BUFFER) {
                ship1Pos.add(adjustedDirNormal.mul(distance / -1.0 + DISTANCE_BUFFER, ship1Pos))
            }
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

            if (tag.contains("AttachmentConstraintId") && tag.contains("AttachmentConstraint") && tag.contains("OrientationConstraintId") && tag.contains("OrientationConstraint")) {
                val attachConstraintData = tag.getByteArray("AttachmentConstraint")
                val attachConstraint = mapper.readValue(attachConstraintData, VSAttachmentConstraint::class.java)
                val orientationConstraintData = tag.getByteArray("OrientationConstraint")
                val orientationConstraint = mapper.readValue(orientationConstraintData, VSFixedOrientationConstraint::class.java)
                adjustedDistance = attachConstraint.fixedDistance
                realShip1 = level.shipObjectWorld.allShips.getById(attachConstraint.shipId0)
                realShip2 = level.shipObjectWorld.allShips.getById(attachConstraint.shipId1)
                realShip1Pos = Vector3d(attachConstraint.localPos0)
                realShip2Pos = Vector3d(attachConstraint.localPos1)
                realShip1Rot = Quaterniond(orientationConstraint.localRot0)
                realShip2Rot = Quaterniond(orientationConstraint.localRot1)
            }

            val constraintPair: Pair<VSAttachmentConstraint, VSFixedOrientationConstraint>? = makeConstraint(realShip1Pos, ship2ConstraintPos, realShip1, realShip2, level, realShip1Rot, realShip2Rot, realShip2Pos)

            if (constraintPair != null) {
                val attachConstraint = constraintPair.first
                val orientationConstraint = constraintPair.second
                val attachConstraintId = level.shipObjectWorld.createNewConstraint(attachConstraint)
                val orientationConstraintId = level.shipObjectWorld.createNewConstraint(orientationConstraint)
                tag.putInt("AttachmentConstraintId", attachConstraintId ?: -1)
                tag.putByteArray("AttachmentConstraint", mapper.writeValueAsBytes(attachConstraint))
                tag.putInt("OrientationConstraintId", orientationConstraintId ?: -1)
                tag.putByteArray("OrientationConstraint", mapper.writeValueAsBytes(orientationConstraint))
                tag.putDouble("ShipStickerDistance", adjustedDistance)

                ClockworkMod.LOGGER.info("Attached to ship ${attachConstraint.shipId1} using points ${attachConstraint.localPos0} && ${attachConstraint.localPos1}")
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
        ): Pair<VSAttachmentConstraint, VSFixedOrientationConstraint>? {
            var realShip2ConstraintPos: Vector3d = ship2ConstraintPos
            var realShip1Rot: Quaterniond? = ship1Rot
            var realShip2Rot: Quaterniond? = ship2Rot
            if (ship1 == null && ship2 == null) return null
            val ship1Id: Long
            val ship2Id: Long
            val groundId: Long = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
            if (ship1Rot == null && ship1 == null) realShip1Rot = Quaterniond()
            if (ship2Rot == null && ship2 == null) realShip1Rot = Quaterniond()
            var mass = 100.0
            if (ship1 != null) {
                ship1.shipToWorld.transformPosition(realShip2ConstraintPos)
                ship1Id = ship1.id
                if (realShip1Rot == null) realShip1Rot = ship1.transform.shipToWorldRotation as Quaterniond
                mass = level.shipObjectWorld.allShips.getById(ship1Id)!!.inertiaData.mass
            } else ship1Id = groundId
            if (ship2 != null) {
                ship2.worldToShip.transformPosition(realShip2ConstraintPos)
                ship2Id = ship2.id
                if (realShip2Rot == null) realShip2Rot = ship2.transform.shipToWorldRotation as Quaterniond
                //ship2Rot.add(ship2.getTransform().getShipToWorldRotation());
                mass = level.shipObjectWorld.allShips.getById(ship2Id)!!.inertiaData.mass
            } else ship2Id = groundId
            if (ship2Pos != null) realShip2ConstraintPos = ship2Pos
            val ship1Rotation: Quaterniondc = realShip1Rot?: Quaterniond()
            val ship2Rotation: Quaterniondc = realShip2Rot?: Quaterniond()

            val attachConstraint = VSAttachmentConstraint(
                ship1Id,
                ship2Id,
                1e-9 / mass,
                ship1ConstraintPos,
                realShip2ConstraintPos,
                1e10,
                ship1ConstraintPos.distance(ship2ConstraintPos)
            )

            val orientationConstraint = VSFixedOrientationConstraint(
                ship1Id,
                ship2Id,
                1e-9 / mass,
                ship1Rotation,
                ship2Rotation,
                1e10
            )

            return (attachConstraint to orientationConstraint)
        }


        fun removeConstraint(level: ServerLevel, removeTags: Boolean, compoundTag: CompoundTag) {
            if (compoundTag.contains("AttachmentConstraintId")) {
                level.shipObjectWorld.removeConstraint(compoundTag.getInt("AttachmentConstraintId"))

                if (removeTags) {
                    compoundTag.remove("AttachmentConstraintId")
                    compoundTag.remove("AttachmentConstraint")
                }
            }
            if (compoundTag.contains("OrientationConstraintId")) {
                level.shipObjectWorld.removeConstraint(compoundTag.getInt("OrientationConstraintId"))

                if (removeTags) {
                    compoundTag.remove("OrientationConstraintId")
                    compoundTag.remove("OrientationConstraint")
                }
            }
        }
    }
}