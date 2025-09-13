package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

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
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock.Companion.POWERED
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerMovementBehavior.Companion.isAttachedToShipOrWorld
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.apigame.joints.VSFixedJoint
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOML


class SlickerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {

    var piston: LerpedFloat? = null
    var update = false

    var attachedShipId: Long = -1
    var attachmentConstraintData: VSFixedJoint? = null
    var attachmentConstraintId: Int = -1

    var distance = 0.0

    var shipStuck = false
    var waitForNoPower = false

    var wasAttached = false

    var shouldRenderDoink = false

    var currentDoinkSize = 0.0
    var currentDoinkTransparency = 1.0

    var targetDoinkSize = 0.0
    val targetDoinkTransparency = 0.0

    init {
        piston = LerpedFloat.linear()
        update = false
    }

    private fun removeConstraint(level: ServerLevel?, removeTags: Boolean) {
        val extraData = PlatformUtils.getExtraData(this).getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)
        if (extraData.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)) {
            if (level != null) {
                level.gtpa.removeJoint(extraData.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID))
            }
            if (removeTags) {
                extraData.remove(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)
                extraData.remove(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)
                extraData.remove(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE)
            }
        }
    }

    private fun doTick() {
        if (this.level == null || this.level!!.isClientSide) return
        val slevel = this.level as ServerLevel

        val myDir: Direction = blockState.getValue(DirectionalBlock.FACING)
        val myDirNormal: Vector3d = Vec3.atLowerCornerOf(myDir.normal).toJOML().normalize()

        val shipAttached: Boolean = isAttachedToShipOrWorld(
            false, slevel,
                Vec3.atCenterOf(
                    blockPos
                ).toJOML(), myDirNormal, PlatformUtils.getExtraData(this)// this.extraCustomData
        ) //isAttachedToShipOrWorld(false);

        //val shipAttached = attachedShipId != -1L

        if (isBlockStateExtended() && !shipStuck) {
            //Sticker extended with no ship related thing stuck to it
            waitForNoPower = false
            if (shipAttached) {
                //no sameworld block attached but there is a ship related thing near enough
                if (isAttachedToShipOrWorld(
                        true, slevel,
                            Vec3.atCenterOf(
                                blockPos
                            ).toJOML(), myDirNormal, PlatformUtils.getExtraData(this)
                    )
                ) {
                    shipStuck = true
                    ClockworkSounds.DOINK.playOnServer(slevel, BlockPos.containing(level.toWorldCoordinates(worldPosition)), 0.35f, 0.75f)
                    ClockworkPackets.sendToNear(
                        slevel, blockPos, 128, SlickerAttachmentSyncPacket(this)
                    )
                }
            }
        } else if (!isBlockStateExtended() && shipStuck) {
            //Sticker retracted with ship related thing stuck to it
            if (!level!!.isClientSide) {
                ClockworkSounds.BOING.playOnServer(slevel, BlockPos.containing(level.toWorldCoordinates(worldPosition)), 0.35f, 0.75f)
                removeConstraint(level as ServerLevel?, true)
                shipStuck = false
            }
            waitForNoPower = true
        } else if (isBlockStateExtended() && !PlatformUtils.getExtraData(this).getCompound(ClockworkConstants.Nbt.CONDENSED_DATA).contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID) && !shipStuck && shipAttached && blockState.getValue(
                POWERED
            )
        ) {
            //Sticker extended with nothing attached and is powered but there is a ship thing in range
            waitForNoPower = false

            if (isAttachedToShipOrWorld(
                    true, slevel,
                        Vec3.atCenterOf(
                            blockPos
                        ).toJOML(), myDirNormal, PlatformUtils.getExtraData(this)
                )
            ) {
                ClockworkSounds.DOINK.playOnServer(slevel, BlockPos.containing(level.toWorldCoordinates(worldPosition)), 0.35f, 0.75f)
                shipStuck = true
                ClockworkPackets.sendToNear(
                    slevel, blockPos, 128, SlickerAttachmentSyncPacket(this)
                )
            }
        }
        if (waitForNoPower && !blockState.getValue(POWERED)) {
            if (shipStuck) ClockworkSounds.BOING.playOnServer(slevel, BlockPos.containing(level.toWorldCoordinates(worldPosition)), 0.35f, 0.75f)
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
        }
    }

    fun isAlreadyPowered(reset: Boolean): Boolean {
        val extraData = PlatformUtils.getExtraData(this).getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)
        val result: Boolean = extraData.contains(ClockworkConstants.Nbt.SHIP_STICKER_ALREADY_POWERED)
        if (reset) {
            extraData.remove(ClockworkConstants.Nbt.SHIP_STICKER_ALREADY_POWERED)
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
        if (shipStuck != wasAttached) {
            shouldRenderDoink = true

            targetDoinkSize = if (shipStuck) 2.0 else 0.0
            currentDoinkTransparency = 1.0
            wasAttached = shipStuck
        }
        if (shouldRenderDoink) {

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
            condensedTag.putLong(ClockworkConstants.Nbt.ATTACHED_SHIP, this.attachedShipId)
        }
        if (this.attachmentConstraintData != null) {
            condensedTag.putByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT, mapper.writeValueAsBytes(this.attachmentConstraintData))
        }
        if (this.attachmentConstraintId != -1) {
            condensedTag.putInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID, this.attachmentConstraintId)
        }
        if (this.distance != 0.0) {
            condensedTag.putDouble(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE, this.distance)
        }
        if (this.shipStuck) {
            condensedTag.putBoolean(ClockworkConstants.Nbt.SHIP_STUCK, this.shipStuck)
        }
        tag.put(ClockworkConstants.Nbt.CONDENSED_DATA, condensedTag)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (clientPacket) update = true
        val tag = compound.getCompound(ClockworkConstants.Nbt.CONDENSED_DATA)
        if (tag.contains(ClockworkConstants.Nbt.ATTACHED_SHIP)) {
            this.attachedShipId = compound.getLong(ClockworkConstants.Nbt.ATTACHED_SHIP)
        }
        if (tag.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT)) {
            this.attachmentConstraintData = mapper.readValue(compound.getByteArray(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT), VSFixedJoint::class.java)
        }
        if (tag.contains(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)) {
            this.attachmentConstraintId = compound.getInt(ClockworkConstants.Nbt.ATTACHMENT_CONSTRAINT_ID)
        }
        if (tag.contains(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE)) {
            this.distance = compound.getDouble(ClockworkConstants.Nbt.SHIP_SLICKER_DISTANCE)
        }
        if (tag.contains(ClockworkConstants.Nbt.SHIP_STUCK)) {
            this.shipStuck = compound.getBoolean(ClockworkConstants.Nbt.SHIP_STUCK)
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