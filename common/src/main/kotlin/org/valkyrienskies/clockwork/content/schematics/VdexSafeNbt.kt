package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.foundation.utility.BlockHelper
import net.createmod.catnip.math.VecHelper
import net.createmod.catnip.nbt.NBTProcessors
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.decoration.Painting
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalLinearBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.copter.CopterBearingBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.clampotron.ClampotronBlockEntity
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.hoseport.HosePortBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing.SpinoffBearingBlockEntity
import org.valkyrienskies.clockwork.util.ClockworkCopyableBlock

internal object VdexSafeNbt {
    fun block(state: BlockState, pos: BlockPos, original: CompoundTag?): CompoundTag? {
        if (original == null) return null
        val be = (state.block as? EntityBlock)?.newBlockEntity(pos, state) ?: return null
        require(!be.onlyOpCanSetNbt()) { "Cannot build ${state.block.name.string} in survival" }
        be.load(original.copy())
        val safe = BlockHelper.prepareBlockEntityData(state, be)?.copy() ?: return null
        preserveClockwork(be, original, safe)
        safe.remove("ClockworkVdex")
        return safe.takeUnless { it.isEmpty }
    }

    fun preserveClockwork(be: BlockEntity, original: CompoundTag, safe: CompoundTag) {
        fun keep(vararg keys: String) = copy(original, safe, *keys)
        if (be.blockState.block is ClockworkCopyableBlock) keep("ClockworkReferences")
        when (be) {
            is PhysBearingBlockEntity -> keep("jointId", "originalFacing", "targetAngle", "sequencedAngleLimit",
                "sequencedAngleProgress", "aligning", "partnerPosx", "partnerPosy", "partnerPosz", "partnerShipId",
                "PartnerAnchorx", "PartnerAnchory", "PartnerAnchorz", "NeutralRotX", "NeutralRotY", "NeutralRotZ", "NeutralRotW")
            is GimbalBearingBlockEntity -> keep("Running", "connectedShip", "JointID", "OriginalDirection", "MaxAngleDeg", "Mode",
                "BearingAxisLX", "BearingAxisLY", "BearingAxisLZ", "BearingPosSX", "BearingPosSY", "BearingPosSZ",
                "AssyHostRotX", "AssyHostRotY", "AssyHostRotZ", "AssyHostRotW", "NeutralRotX", "NeutralRotY", "NeutralRotZ", "NeutralRotW",
                "AttachedBlock", "CopyRotX", "CopyRotY", "CopyRotZ", "CopyRotW")
            is SpinoffBearingBlockEntity -> keep("partnerX", "partnerY", "partnerZ", "jointId", "partnerShipId", "isLeader")
            is UniversalShaftBlockEntity -> keep("otherPosXr", "otherPosYr", "otherPosZr", "otherPosXa", "otherPosYa", "otherPosZa",
                "main", "otherShipId", "ClockworkSourcePos", "ClockworkSourceShip")
            is PhysicalPistonBlockEntity -> keep("ShaftOrigin", "ShaftShip", "Head", "Tail")
            is PhysicalGantryBlockEntity -> keep("RailOrigin", "RailDirection", "RailShip")
            is ExtendonBlockEntity, is HosePortBlockEntity -> keep("ConnectedPosX", "ConnectedPosY", "ConnectedPosZ", "IsMain",
                "DistanceJointId", "SphericalJointId")
            is PropellerBearingBlockEntity -> keep("Running", "Active", "Angle", "RotationDirection", "TargetOmega")
            is FlapBearingBlockEntity -> keep("BearingAngle", "TargetAngle", "IsRunning", "IsLocked")
            is BladeControllerBlockEntity -> keep("BladeAngle")
            is DeliveryCannonBlockEntity -> keep("xRot", "yRot", "distance", "shootingAtChute", "Frequency")
            is DeliveryChuteBlockEntity -> keep("Frequency")
            is DuctBlockEntity -> {
                keep("ClockworkEdges")
                Direction.entries.forEach { keep("DuctEdgeType${it.name}") }
            }
            is DuctTankBlockEntity -> keep("Height", "Width", "Controller")
            is RedstoneDuctBlockEntity -> keep("Conditional")
            is ValveDuctBlockEntity -> keep("Pointer")
            is CreativeGeneratorBlockEntity -> keep("GasValues", "Temperature")
            is CombobulatorBlockEntity -> keep("Program")
            is RedstoneResistorBlockEntity -> keep("ThinkableRatio")
            is ColorBlockEntity -> keep("Clockwork\$color")
            is ClampotronBlockEntity -> keep("Enabled")
        }
        if (be is CopterBearingBlockEntity) keep("DesiredOffsetFromPower")
        if (be is PhysicalLinearBlockEntity) keep("LockedOffset")
    }

    fun entity(original: CompoundTag, level: ServerLevel): CompoundTag {
        val type = EntityType.byString(original.getString("id")).orElseThrow { IllegalArgumentException("Unknown blueprint entity") }
        val entity = type.create(level) ?: error("Cannot create blueprint entity")
        require(!entity.onlyOpCanSetNbt()) { "Cannot build this entity in survival" }
        val safe = CompoundTag()
        copy(original, safe, "id", "Pos", "Rotation", "CustomName", "CustomNameVisible")
        when (entity) {
            is ItemFrame -> {
                copy(original, safe, "Facing", "TileX", "TileY", "TileZ", "ItemRotation", "Invisible")
                safe.put("Item", item(original.getCompound("Item")))
            }
            is ArmorStand -> {
                copy(original, safe, "Pose", "Small", "ShowArms", "NoBasePlate", "Invisible", "NoGravity")
                for (key in listOf("HandItems", "ArmorItems")) safe.put(key, ListTag().also { items ->
                    original.getList(key, 10).forEach { items.add(item(it as CompoundTag)) }
                })
            }
            is Painting -> copy(original, safe, "Facing", "TileX", "TileY", "TileZ", "variant")
            is ControlledContraptionEntity -> {
                copy(original, safe, "Controller", "ControllerRelative", "Axis", "Angle")
                val source = original.getCompound("Contraption")
                val typeId = source.getString("Type")
                require(typeId.substringBefore(':') in setOf("create", "vs_clockwork")) { "Unsupported survival contraption $typeId" }
                val contraption = CompoundTag()
                copy(source, contraption, "Type", "Blocks", "Seats", "CapturedMultiblocks", "Interactors", "Superglue", "Anchor", "BoundsFront",
                    "Sails", "Facing", "Offset", "facing", "offset", "HandType", "InitialLength", "ExtensionLength", "Orientation", "InitialOffset")
                contraption.put("Actors", ListTag().also { actors ->
                    source.getList("Actors", 10).forEach { actor ->
                        actors.add(CompoundTag().also {
                            copy(actor as CompoundTag, it, "Pos")
                            it.put("Motion", VecHelper.writeNBT(Vec3.ZERO))
                            it.put("RelativeMotion", VecHelper.writeNBT(Vec3.ZERO))
                            it.putBoolean("FirstMovement", true)
                        })
                    }
                })
                safe.put("Contraption", contraption)
            }
            else -> error("Cannot safely build blueprint entity ${original.getString("id")} in survival")
        }
        return safe
    }

    fun item(original: CompoundTag): CompoundTag = NBTProcessors.withUnsafeNBTDiscarded(ItemStack.of(original)).save(CompoundTag())

    private fun copy(source: CompoundTag, target: CompoundTag, vararg keys: String) {
        for (key in keys) source.get(key)?.let { target.put(key, it.copy()) }
    }
}
