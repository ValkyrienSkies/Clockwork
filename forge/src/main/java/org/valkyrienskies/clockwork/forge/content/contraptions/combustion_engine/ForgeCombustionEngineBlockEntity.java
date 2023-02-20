package org.valkyrienskies.clockwork.forge.content.contraptions.combustion_engine;
//
//import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
//import com.simibubi.create.foundation.fluid.SmartFluidTank;
//import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.NbtUtils;
//import net.minecraft.network.chat.Component;
//import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.material.Fluid;
//import net.minecraftforge.common.capabilities.Capability;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.fluids.FluidAttributes;
//import net.minecraftforge.fluids.FluidStack;
//import net.minecraftforge.fluids.IFluidTank;
//import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
//import net.minecraftforge.fluids.capability.IFluidHandler;
//import net.minecraftforge.fluids.capability.templates.FluidTank;
//import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlock;
//import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlockEntity;
//import org.valkyrienskies.clockwork.data.ClockWorkTags;
//import org.valkyrienskies.clockwork.util.blocktype.FuelBoosterType;
//import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.time.Clock;
//import java.util.List;
//
//import static com.simibubi.create.content.contraptions.fluids.tank.FluidTankTileEntity.getCapacityMultiplier;
//
//public class ForgeCombustionEngineBlockEntity extends CombustionEngineBlockEntity implements IHaveGoggleInformation {
//
////    protected LazyOptional<IFluidHandler> fluidCapability;
//    protected boolean forceFluidLevelUpdate;
////    protected FluidTank tankInventory;
//    protected BlockPos lastKnownPos;
//
//    private float generatedSpeed;
//
//    private static final int SYNC_RATE = 8;
//    protected int syncCooldown;
//    protected boolean queuedSync;
//
//    private boolean active = false;
//
//    //
//
//    public ForgeCombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
//        super(typeIn, pos, state);
////        tankInventory = createInventory();
////        fluidCapability = LazyOptional.of(() -> tankInventory);
////        forceFluidLevelUpdate = true;
////        refreshCapability();
//    }
//
////    protected SmartFluidTank createInventory() {
////        return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
////    }
//
//    @Override
//    public void tick() {
//        super.tick();
//
//        float prevGeneratedSpeed = generatedSpeed;
//        generatedSpeed = switch (getFuelQuality()) {
//            case NONE -> 0;
//            case STALE -> 16;
//            case PLAIN -> 32;
//            case SWEET, EXTRA -> 64;
//            case GOURMET -> 128;
//        };
//        if (prevGeneratedSpeed != generatedSpeed) {
//            reActivateSource = true;
//        }
//        if (reActivateSource) {
//            updateGeneratedRotation();
//            reActivateSource = false;
//        }
//        if (syncCooldown > 0) {
//            syncCooldown--;
//            if (syncCooldown == 0 && queuedSync)
//                sendData();
//        }
//
//        if (hasValidFuelType()) {
//            active = true;
//            if (getRemainingFuel() >= 2) {
//                tank.getPrimaryHandler().drain(getDrainRate(), IFluidHandler.FluidAction.EXECUTE);
//            } else {
//                tank.getPrimaryHandler().drain(getRemainingFuel(), IFluidHandler.FluidAction.EXECUTE);
//                tank.getPrimaryHandler().getFluid().shrink(amount);
//                active = false;
//            }
//
//        }
//
//        if (lastKnownPos == null)
//            lastKnownPos = getBlockPos();
//        else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
//            onPositionChanged();
//        }
//
//    }
//
////    @Override
////    public float calculateAddedStressCapacity() {
////        return Math.abs(getGeneratedSpeed()) * 64;
////    }
//
////    @Override
////    public float getGeneratedSpeed() {
////        return convertToDirection(generatedSpeed, getBlockState().getValue(CombustionEngineBlock.FACING));
////    }
//
//    @Override
//    public void initialize() {
//        super.initialize();
//        sendData();
//        if (level.isClientSide)
//            invalidateRenderBoundingBox();
//    }
//
//    private FluidStack getCurrentFluidInTank() {
//        return tank.getPrimaryHandler()
//                .getFluid();
//    }
//
//    private void onPositionChanged() {
//        lastKnownPos = worldPosition;
//    }
//
//    protected void onFluidStackChanged(FluidStack newFluidStack) {
//        if (!hasLevel())
//            return;
//
//        FluidAttributes attributes = newFluidStack.getFluid()
//                .getAttributes();
//
//        if (!level.isClientSide) {
//            setChanged();
//            sendData();
//        }
//
//    }
//
//    @Override
//    public LiquidFuelType getFuelQuality() {
//        if (!hasValidFuelType()) {
//            return LiquidFuelType.NONE;
//        }
//        Fluid fuel = getCurrentFluidInTank().getFluid();
//        if (fuel.is(ClockWorkTags.AllFluidTags.STALE.tag)) {
//            return LiquidFuelType.STALE;
//        } else if (fuel.is(ClockWorkTags.AllFluidTags.PLAIN.tag)) {
//            return LiquidFuelType.PLAIN;
//        } else if (fuel.is(ClockWorkTags.AllFluidTags.SWEET.tag)) {
//            return LiquidFuelType.SWEET;
//        } else if (fuel.is(ClockWorkTags.AllFluidTags.GOURMET.tag)) {
//            return LiquidFuelType.GOURMET;
//        } else {
//            return LiquidFuelType.EXTRA;
//        }
//    }
//
//    public void sendDataImmediately() {
//        syncCooldown = 0;
//        queuedSync = false;
//        sendData();
//    }
//
//    @Override
//    public void sendData() {
//        if (syncCooldown > 0) {
//            queuedSync = true;
//            return;
//        }
//        super.sendData();
//        queuedSync = false;
//        syncCooldown = SYNC_RATE;
//    }
//
////    private void refreshCapability() {
////        LazyOptional<IFluidHandler> oldCap = fluidCapability;
////        fluidCapability = LazyOptional.of(() -> new FluidTank(0));
////        oldCap.invalidate();
////    }
//
//    @Override
//    protected void read(CompoundTag compound, boolean clientPacket) {
//        super.read(compound, clientPacket);
//        tank.getPrimaryHandler().readFromNBT(compound.getCompound("Tank"));
//        if (clientPacket)
//            forceFluidLevelUpdate = true;
//
//        lastKnownPos = null;
//
//        if (compound.contains("LastKnownPos"))
//            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
//
//        lastCapacityProvided = 0;
//
//        if (compound.contains("LastCapacityProvided"))
//            lastCapacityProvided = compound.getFloat("LastCapacityProvided");
//
//        if (!clientPacket)
//            return;
//
//
//    }
//
//    public float getFillState() {
//        return (float) tank.getPrimaryHandler().getFluidAmount() / tank.getPrimaryHandler().getCapacity();
//    }
//
//    @Override
//    public void write(CompoundTag compound, boolean clientPacket) {
//        super.write(compound, clientPacket);
//        compound.put("Tank", tank.getPrimaryHandler().writeToNBT(new CompoundTag()));
//        if (lastKnownPos != null)
//            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
//        compound.putFloat("LastCapacityProvided", lastCapacityProvided);
//
//        forceFluidLevelUpdate = false;
//    }
//
////    @Nonnull
////    @Override
////    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
////        if (!fluidCapability.isPresent())
////            refreshCapability();
////        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
////            return fluidCapability.cast();
////        return super.getCapability(cap, side);
////    }
//
//    @Override
//    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
//        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && side != Direction.DOWN)
//            return tank.getCapability()
//                    .cast();
//        return super.getCapability(cap, side);
//    }
//
//    @Override
//    public void invalidate() {
//        super.invalidate();
//    }
//
//    public IFluidTank getTankInventory() {
//        return tank.getPrimaryHandler();
//    }
//
//    @Override
//    public int getRemainingFuel() {
//        if (!hasValidFuelType()) {
//            return 0;
//        }
//
//        return tank.getPrimaryHandler().getFluidAmount();
//    }
//
//    @Override
//    public int getDrainRate() {
//        //todo: boosters
//        return 2;
//    }
//
//    @Override
//    public FuelBoosterType getFuelBooster() {
//        //todo: boosters
//        return null;
//    }
//
//
//    @Override
//    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
//        return containedFluidTooltip(tooltip, isPlayerSneaking,
//                getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY));
//    }
//}
