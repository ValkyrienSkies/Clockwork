package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.forces.ReactionWheelController
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.data.ReactionWheelCreateData
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.data.ReactionWheelUpdateData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class ReactionWheelBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    KineticBlockEntity(type, pos, state) {
    var angle = 0f
    var rotspeed = 0f
    var active = false
    var wasActive = false
    var alreadyAdded = false
    var spinup = false
    var spindown = false
    var spinupProg = 0f
    var spindownProg = 0f
    var activeControlSpeed = 0f
    var activeControlMode = false
    var shouldRemove = false
    var rwID: Int? = null
    fun setShouldRemove() {
        shouldRemove = true
    }

    override fun createRenderBoundingBox(): AABB {
        return super.createRenderBoundingBox().inflate(4.0)
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putFloat("angle", angle)
        compound.putFloat("rotspeed", rotspeed)
        compound.putBoolean("active", active)
        compound.putBoolean("wasActive", wasActive)
        compound.putBoolean("alreadyAdded", alreadyAdded)
        if (rwID != null) {
            compound.putInt("rwID", rwID!!)
        }
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        angle = compound.getFloat("angle")
        rotspeed = compound.getFloat("rotspeed")
        active = compound.getBoolean("active")
        wasActive = compound.getBoolean("wasActive")
        alreadyAdded = compound.getBoolean("alreadyAdded")
        if (compound.contains("rwID")) {
            rwID = compound.getInt("rwID")
        }
        super.read(compound, clientPacket)
    }

    override fun remove() {
        if (level != null) {
            if (!level!!.isClientSide) {
                val ship: ServerShip? = (level as ServerLevel).getShipObjectManagingPos(worldPosition)
                if (ship != null) {
                    val controller = ReactionWheelController.getOrCreate(ship)
                    controller!!.removeReactionWheel(rwID!!)
                }
            }
        }
        super.remove()
    }

    override fun tick() {
        super.tick()
        activeControlMode = true
        if (active) {
            if (!wasActive && !activeControlMode) {
                spindown = false
                spinup = true
                spinupProg = speed - rotspeed
            }
            if (spinup) {
                modSpinupSpeed()
            } else {
                modSpeed()
            }
        } else {
            if (wasActive) {
                spinup = false
                spindown = true
                spindownProg = rotspeed
            }
            if (spindown) {
                modSpindownSpeed()
            } else {
                rotspeed = 0f
            }
        }
        angle += rotspeed * 3 / 10f
        angle %= 360f
        var ship: LoadedServerShip? = null
        if (!level!!.isClientSide) {
            if (level.getShipObjectManagingPos(blockPos) != null) {
                ship = (level as ServerLevel).getShipObjectManagingPos(blockPos)
            }
        }
        wasActive = active
        if (ship != null) {
            if (!alreadyAdded && rwID == null || ReactionWheelController.getOrCreate(ship)!!.checkReactionWheel(rwID)) {
                val pos: Vector3dc = worldPosition.toJOMLD()
                val axis: Vector3dc = when (blockState.getValue(BlockStateProperties.AXIS)) {
                    Direction.Axis.X -> Vector3d(1.0, 0.0, 0.0)
                    Direction.Axis.Y -> Vector3d(0.0, 1.0, 0.0)
                    Direction.Axis.Z -> Vector3d(0.0, 0.0, 1.0)
                }
                val data = ReactionWheelCreateData(pos, axis, rotspeed, spinup, spindown, active, speed)
                rwID = ReactionWheelController.getOrCreate(ship)!!.addReactionWheel(data)
                alreadyAdded = true
            }
            if (alreadyAdded && rwID != null) {
                if (ReactionWheelController.getOrCreate(ship) != null) {
                    val data = ReactionWheelUpdateData(rotspeed, speed)
                    ReactionWheelController.getOrCreate(ship)!!.updateReactionWheel(rwID!!, data)
                    //                active = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
//                    case X -> Math.abs(ship.getOmega().x()) >= 10;
//                    case Y -> Math.abs(ship.getOmega().y()) >= 10;
//                    case Z -> Math.abs(ship.getOmega().z()) >= 10;
//                };
                    //FOR TESTING
                    val axis: Vector3dc = when (blockState.getValue(BlockStateProperties.AXIS)) {
                        Direction.Axis.X -> Vector3d(1.0, 0.0, 0.0)
                        Direction.Axis.Y -> Vector3d(0.0, 1.0, 0.0)
                        Direction.Axis.Z -> Vector3d(0.0, 0.0, 1.0)
                    }
                    active = true
                    if (active && activeControlMode) {
                        if (java.lang.Float.isNaN(rotspeed)) {
                            rotspeed = 0f
                        }
                        activeControlSpeed = computeTargetSpeed(ship, rotspeed, axis).toFloat()
                        if (Math.abs(activeControlSpeed) > Math.abs(speed)) {
                            activeControlSpeed = speed
                        }
                    }
                }
            }
            if (this.isRemoved || shouldRemove) {
                if (rwID != null) {
                    ReactionWheelController.getOrCreate(ship)!!.removeReactionWheel(rwID!!)
                    rwID = null
                    alreadyAdded = false
                }
            }
        }
    }

    private fun computeTargetSpeed(
        ship: LoadedServerShip,
        rspeed: Float,
        axis: Vector3dc
    ): Double {
        val wheelSpeed = rspeed.toDouble()
        val wheelMass = 18000.0
        val wheelInertiaD =
            0.5 * wheelMass * (Math.pow(0.25, 2.0) + Math.pow(0.75, 2.0))
        val wheelOmegaD = wheelSpeed * (2 * Math.PI / 20)
        val wheelOmega: Vector3dc = Vector3d(axis).mul(wheelOmegaD)
        val wheelInertia: Vector3dc = Vector3d(axis).mul(wheelInertiaD)
        val shipOmega = ship.omega
        val shipInertia = ship.inertiaData.momentOfInertiaTensor
        val wheelL = wheelOmega.mul(wheelInertia, Vector3d())
        val shipL = shipOmega.mul(shipInertia, Vector3d())
        val Lt: Vector3dc = wheelL.add(shipL, Vector3d())
        val targetWheelL = Lt.div(wheelInertia.length(), Vector3d())

//        Vector3d targetWheelOmega = targetWheelL.div(wheelInertiaD, new Vector3d());
        return targetWheelL.length() / (2 * Math.PI / 20)
    }

    private fun modSpeed() {
        var targetSpeed = speed
        if (activeControlMode) {
            targetSpeed = activeControlSpeed
        }
        if (rotspeed == targetSpeed) {
            return
        }
        //        if ((int) getSourceSpeed() == 0 && (int) speed == 0) {
//            speed = 0;
//            return;
//        }
        val diff = targetSpeed - rotspeed
        rotspeed = rotspeed + Mth.clamp(diff / 10, -32f, 32f)
        //        float delta = Mth.clamp(, lastSpeed, 10)
        //rotspeed = (float) Mth.lerp(delta, lastSpeed, targetSpeed);
    }

    private fun modSpindownSpeed() {
        spindownProg--
        if (spindownProg <= 0) {
            spindown = false
            return
        }
        val stoppingPoint = angle + rotspeed * spindownProg * 0.5f
        val optimalStoppingPoint = 90f * Math.round(stoppingPoint / 90f)
        val Q = (optimalStoppingPoint - stoppingPoint) / spindownProg
        rotspeed = (rotspeed + 6f * Q / spindownProg) * (1f - 1f / spindownProg)
    }

    private fun modSpinupSpeed() {
        if (level!!.isClientSide) {
            return
        }
        spinupProg--
        if (Math.abs(rotspeed) >= Math.abs(speed)) {
            spinup = false
            if (Math.abs(rotspeed) > Math.abs(speed)) {
                rotspeed = speed
            }
            return
        }

//            float time = 1f - (spinup / 20f);
//            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
        val startingPoint = angle + speed * spinupProg * 0.5f
        val Q = startingPoint / spinupProg
        rotspeed = (rotspeed + 6f * Q / spinupProg) * (1f - 1f / spinupProg)
    }
}