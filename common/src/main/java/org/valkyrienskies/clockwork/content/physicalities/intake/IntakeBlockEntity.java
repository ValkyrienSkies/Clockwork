package org.valkyrienskies.clockwork.content.physicalities.intake;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.IntakeController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class IntakeBlockEntity extends KineticBlockEntity {

    private boolean alreadyCreated = false;
    private Integer intakeID = null;
    public IntakeBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        return speed * 20;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        LoadedServerShip ship = null;
        if (VSGameUtilsKt.getShipObjectManagingPos(level, worldPosition) != null) {
            if (!level.isClientSide) {
                ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());;
            }
        }
        if (ship != null && !alreadyCreated) {
            Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
            final IntakeCreateData data = new IntakeCreateData(speed, pos);
            alreadyCreated = true;
            intakeID = IntakeController.getOrCreate(ship).addIntake(data);
        }
        if (ship != null && alreadyCreated && intakeID != null) {
            final IntakeUpdateData data = new IntakeUpdateData(speed);
            IntakeController.getOrCreate(ship).updateIntake(intakeID, data);
        }
        if (this.isRemoved()) {
            if (ship != null && intakeID != null) {
                IntakeController.getOrCreate(ship).removeIntake(intakeID);
                intakeID = null;
                alreadyCreated = false;
            }
        }
    }
}
