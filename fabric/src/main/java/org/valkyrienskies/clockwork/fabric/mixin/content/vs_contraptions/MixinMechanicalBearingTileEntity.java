package org.valkyrienskies.clockwork.fabric.mixin.content.vs_contraptions;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.tileEntity.SyncedTileEntity;
import com.simibubi.create.foundation.utility.UniqueLinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.fabric.mixinducks.CWIControlContraption;
import org.valkyrienskies.clockwork.fabric.util.assemble.GlueAssembler;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity extends GeneratingKineticTileEntity implements CWIControlContraption  {

    @Shadow protected AssemblyException lastException;

    @Shadow protected boolean running;

    @Shadow protected float angle;

    @Unique // Rn only set serverside, is null client-side
    private Ship ship = null;

    public MixinMechanicalBearingTileEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public boolean isShipContraptionController() {
        return true;
    }

    @Nullable
    @Override
    public Ship getConnectedShip() {
        return ship;
    }

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true, remap = false)
    void assemble(CallbackInfo ci) {
        if (isShipContraptionController() && level != null && !level.isClientSide) {
            ci.cancel();

            if (!((level.getBlockState(worldPosition)).getBlock() instanceof BearingBlock))
                return;

            Direction direction = getBlockState().getValue(BearingBlock.FACING);
            BlockPos center = worldPosition.relative(direction);
            DenseBlockPosSet selection;

            try {
                selection = GlueAssembler.collectGlued(this.level, center);
                this.lastException = null;
            } catch (AssemblyException e) {
                lastException = e;
                this.sendData();
                return;
            }

            if (selection == null) return;

            ship = ShipAssemblyKt.createNewShipWithBlocks(center, selection, (ServerLevel) level);

            AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

            // TODO setup constraints

            this.running = true;
            this.angle = 0;
            sendData();
            updateGeneratedRotation();
        }
    }
}
