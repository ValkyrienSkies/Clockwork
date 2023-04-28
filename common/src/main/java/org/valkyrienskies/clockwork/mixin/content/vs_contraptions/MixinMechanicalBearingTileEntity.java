package org.valkyrienskies.clockwork.mixin.content.vs_contraptions;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.clockwork.platform.api.GlueType;
import org.valkyrienskies.clockwork.util.assemble.GlueAssembler;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentOrientationConstraint;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity extends GeneratingKineticTileEntity implements ContraptionController {

    @Shadow
    protected AssemblyException lastException;

    @Shadow
    protected boolean running;

    @Shadow
    protected float angle;

    @Unique
    private boolean alreadyAdded;

    @Unique
    private Integer bearingID = null;

    @Unique // Rn only set serverside, is null client-side
    private Ship ship = null;

    public MixinMechanicalBearingTileEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public boolean isShipContraptionController() {
        return false;
    }

    @Nullable
    @Override
    public Ship getConnectedShip() {
        return ship;
    }

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    void assemble(CallbackInfo ci) {
        if (isShipContraptionController() && level != null && !level.isClientSide) {
            ci.cancel();

            if (!((level.getBlockState(worldPosition)).getBlock() instanceof BearingBlock))
                return;

            Direction direction = getBlockState().getValue(BearingBlock.FACING);
            BlockPos center = worldPosition.relative(direction);
            DenseBlockPosSet selection;

            try {
                selection = GlueAssembler.collectGlued(this.level, center, GlueType.SUPER);
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
            ServerShip shipOn = (ServerShip) VSGameUtilsKt.getShipObjectManagingPos(level, worldPosition);

//            double compliance = 1e-3;
//            double maxForce = 1e6;
//            double maxTorque = 1e6;
//            Vector3dc attachPoint = new Vector3d();
            if (shipOn != null) {
                // make constraint between ships


                //VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(new VSAttachmentOrientationConstraint(ship.getId(), shipOn.getId(), compliance, worldPosition, attachPoint, maxForce, new Quaterniond(), new Quaterniond(), maxTorque));

                //Vector3dc bearingPosition = (new Vector3d(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5)).sub(ship.getTransform().getShipToWorld());
            } else {
                // just make constraint
            }

            this.running = true;
            this.angle = 0;
            sendData();
            updateGeneratedRotation();
        }
    }
}
