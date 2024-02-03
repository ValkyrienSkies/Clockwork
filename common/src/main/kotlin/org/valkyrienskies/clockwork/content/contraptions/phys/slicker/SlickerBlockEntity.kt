package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.chassis.StickerBlock
import com.simibubi.create.content.contraptions.glue.SuperGlueItem
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock.Companion.POWERED
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerMovementBehavior.Companion.isAttachedToShipOrWorld
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML


class SlickerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {

    var piston: LerpedFloat? = null
    var update = false

    var attachedShipId: Long = -1
    var attachmentConstraintData: VSAttachmentConstraint? = null
    var orientationConstraintData: VSFixedOrientationConstraint? = null
    var attachmentConstraintId: Int = -1
    var orientationConstraintId: Int = -1

    var distance = 0.0

    var shipStuck = false
    var waitForNoPower = false

    init {
        piston = LerpedFloat.linear()
        update = false
    }

    private fun removeConstraint(level: ServerLevel?, removeTags: Boolean) {
        val extraData = extraCustomData.getCompound("CondensedData")
        if (extraData.contains("AttachmentConstraint") && extraData.contains("OrientationConstraint")) {
            if (level != null) {
                level.shipObjectWorld.removeConstraint(extraData.getInt("AttachmentConstraintId"))
                level.shipObjectWorld.removeConstraint(extraData.getInt("OrientationConstraintId"))
            }
            if (removeTags) {
                extraData.remove("AttachmentConstraintId")
                extraData.remove("OrientationConstraintId")
                extraData.remove("AttachmentConstraint")
                extraData.remove("OrientationConstraint")
                extraData.remove("ShipStickerDistance")
            }
        }
    }

    private fun doTick() {
        if (this.level == null || this.level!!.isClientSide) return
        val slevel = this.level as ServerLevel

        val myDir: Direction = blockState.getValue(DirectionalBlock.FACING)
        val myDirNormal: Vector3d = Vec3.atLowerCornerOf(myDir.getNormal()).toJOML()

        val shipAttached: Boolean = isAttachedToShipOrWorld(
            false, slevel,
                Vec3.atCenterOf(
                    blockPos
                ).toJOML(), myDirNormal, this.extraCustomData
        ) //isAttachedToShipOrWorld(false);

        if (isBlockStateExtended() && !shipStuck) {
            //Sticker extended with no ship related thing stuck to it
            waitForNoPower = false
            if (shipAttached) {
                //no sameworld block attached but there is a ship related thing near enough
                if (isAttachedToShipOrWorld(
                        true, slevel,
                            Vec3.atCenterOf(
                                blockPos
                            ).toJOML(), myDirNormal, extraCustomData
                    )
                ) {
                    shipStuck = true
                    ClockworkPackets.sendToNear(
                        slevel, blockPos, 128, SlickerAttachmentSyncPacket(this)
                    )
                }
            }
        } else if (!isBlockStateExtended() && shipStuck) {
            //Sticker retracted with ship related thing stuck to it
            if (!level!!.isClientSide) {
                removeConstraint(level as ServerLevel?, true)
            }
            waitForNoPower = true
        } else if (isBlockStateExtended() && !extraCustomData.getCompound("CondensedData").contains("AttachmentConstraintId") && !shipStuck && shipAttached && blockState.getValue(
                POWERED
            )
        ) {
            //Sticker extended with nothing attached and is powered but there is a ship thing in range
            waitForNoPower = false
            if (isAttachedToShipOrWorld(
                    true, slevel,
                        Vec3.atCenterOf(
                            blockPos
                        ).toJOML(), myDirNormal, extraCustomData
                )
            ) {
                //StickerParticleUtil().doBluperParticle(level, worldPosition, myDir)
                shipStuck = true
                ClockworkPackets.sendToNear(
                    slevel, blockPos, 128, SlickerAttachmentSyncPacket(this)
                )
            }
        }
        if (waitForNoPower && !blockState.getValue(POWERED)) {
            waitForNoPower = false
            shipStuck = false
            ClockworkPackets.sendToNear(
                slevel, blockPos, 128, SlickerAttachmentSyncPacket(this)
            )
        }
    }

    override fun destroy() {
        if (level != null) {
            if (!level!!.isClientSide) {
                removeConstraint(level as ServerLevel?, true)
            }
        } else {
            throw RuntimeException("ERROR Couldn't try to clean up constraint!")
        }
    }

    fun isAlreadyPowered(reset: Boolean): Boolean {
        val extraData = extraCustomData.getCompound("CondensedData")
        val result: Boolean = extraData.contains("ShipStickerAlreadyPowered")
        if (reset) {
            extraData.remove("ShipStickerAlreadyPowered")
        }
        return result
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun initialize() {
        super.initialize()
        if (!level!!.isClientSide) return
        piston!!.startWithValue((if (isBlockStateExtended()) -2.0/16.0 else 0).toDouble())
    }

    fun isBlockStateExtended(): Boolean {
        val blockState = blockState
        return ClockworkBlocks.SLICKER.has(blockState) && blockState.getValue(SlickerBlock.EXTENDED)
    }

    override fun tick() {
        super.tick()
        if (!level!!.isClientSide) {
            doTick()
            return
        }
        piston!!.tickChaser()
        if (isAttachedToShip() && piston!!.getValue(0f) != piston!!.value && piston!!.value == 1f) {
            SuperGlueItem.spawnParticles(
                level, worldPosition, blockState.getValue(
                    StickerBlock.FACING
                ), true
            )
            playSound(true)
        }
        if (!update) return
        update = false
        val target = if (isBlockStateExtended()) 2.0/16.0 else 0.0
        ClockworkMod.LOGGER.info(isBlockStateExtended().toString())
        if (isAttachedToShip() && target == 0.0 && piston!!.chaseTarget == 1f) playSound(false)
        piston!!.chase(target, .4, LerpedFloat.Chaser.LINEAR)
        //InstancedRenderDispatcher.enqueueUpdate(this)
    }

    fun isAttachedToShip(): Boolean {
        val blockState = blockState
        if (!AllBlocks.STICKER.has(blockState)) return false

        return shipStuck
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        val condensedTag = CompoundTag()
        if (this.attachedShipId != -1L) {
            condensedTag.putLong("AttachedShip", this.attachedShipId)
        }
        if (this.attachmentConstraintData != null) {
            condensedTag.putByteArray("AttachmentConstraint", mapper.writeValueAsBytes(this.attachmentConstraintData))
        }
        if (this.orientationConstraintData != null) {
            condensedTag.putByteArray("OrientationConstraint", mapper.writeValueAsBytes(this.orientationConstraintData))
        }
        if (this.attachmentConstraintId != -1) {
            condensedTag.putInt("AttachmentConstraintId", this.attachmentConstraintId)
        }
        if (this.orientationConstraintId != -1) {
            condensedTag.putInt("OrientationConstraintId", this.orientationConstraintId)
        }
        if (this.distance != 0.0) {
            condensedTag.putDouble("ShipStickerDistance", this.distance)
        }
        if (this.shipStuck) {
            condensedTag.putBoolean("ShipStuck", this.shipStuck)
        }
        tag.put("CondensedData", condensedTag)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (clientPacket) update = true
        val tag = compound.getCompound("CondensedData")
        if (tag.contains("AttachedShip")) {
            this.attachedShipId = compound.getLong("AttachedShip")
        }
        if (tag.contains("AttachmentConstraint")) {
            this.attachmentConstraintData = mapper.readValue(compound.getByteArray("AttachmentConstraint"), VSAttachmentConstraint::class.java)
        }
        if (tag.contains("OrientationConstraint")) {
            this.orientationConstraintData = mapper.readValue(compound.getByteArray("OrientationConstraint"), VSFixedOrientationConstraint::class.java)
        }
        if (tag.contains("ShipStickerDistance")) {
            this.distance = compound.getDouble("ShipStickerDistance")
        }
        if (tag.contains("ShipStuck")) {
            this.shipStuck = compound.getBoolean("ShipStuck")
        }
    }

    fun playSound(attach: Boolean) {
        ClockworkSounds.DOINK.playAt(
            level,
            worldPosition,
            0.35f,
            if (attach) 0.75f else 0.2f,
            false
        )
    }

    companion object {
        val mapper = VSJacksonUtil.defaultMapper
    }
}