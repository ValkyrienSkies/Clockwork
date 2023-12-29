package org.valkyrienskies.clockwork.forge.integration.cc;


// TODO: Fix this
/*
import dan200.computercraft.api.detail.DetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.BalloonerPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ForgeBalloonerPeripheral extends BalloonerPeripheral {
    public ForgeBalloonerPeripheral(BalloonerBlockEntity be) {
        super(be);
    }

    // These methods are from FluidMethods in CC:T

    @LuaFunction(
            mainThread = true
    )
    public static Map<Integer, Map<String, ?>> tanks(IFluidHandler fluids) {
        Map<Integer, Map<String, ?>> result = new HashMap();
        int size = fluids.getTanks();

        for(int i = 0; i < size; ++i) {
            FluidStack stack = fluids.getFluidInTank(i);
            if (!stack.isEmpty()) {
                result.put(i + 1, DetailRegistries.FLUID_STACK.getBasicDetails(stack));
            }
        }

        return result;
    }

    @LuaFunction(
            mainThread = true
    )
    public static int pushFluid(IFluidHandler from, IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        Fluid fluid = fluidName.isPresent() ? (Fluid) getRegistryEntry((String)fluidName.get(), "fluid", ForgeRegistries.FLUIDS) : null;
        IPeripheral location = computer.getAvailablePeripheral(toName);
        if (location == null) {
            throw new LuaException("Target '" + toName + "' does not exist");
        } else {
            IFluidHandler to = extractHandler(location.getTarget());
            if (to == null) {
                throw new LuaException("Target '" + toName + "' is not an tank");
            } else {
                int actualLimit = (Integer)limit.orElse(Integer.MAX_VALUE);
                if (actualLimit <= 0) {
                    throw new LuaException("Limit must be > 0");
                } else {
                    return fluid == null ? moveFluid(from, actualLimit, to) : moveFluid(from, new FluidStack(fluid, actualLimit), to);
                }
            }
        }
    }

    @LuaFunction(
            mainThread = true
    )
    public static int pullFluid(IFluidHandler to, IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        Fluid fluid = fluidName.isPresent() ? (Fluid)getRegistryEntry((String)fluidName.get(), "fluid", ForgeRegistries.FLUIDS) : null;
        IPeripheral location = computer.getAvailablePeripheral(fromName);
        if (location == null) {
            throw new LuaException("Target '" + fromName + "' does not exist");
        } else {
            IFluidHandler from = extractHandler(location.getTarget());
            if (from == null) {
                throw new LuaException("Target '" + fromName + "' is not an tank");
            } else {
                int actualLimit = (Integer)limit.orElse(Integer.MAX_VALUE);
                if (actualLimit <= 0) {
                    throw new LuaException("Limit must be > 0");
                } else {
                    return fluid == null ? moveFluid(from, actualLimit, to) : moveFluid(from, new FluidStack(fluid, actualLimit), to);
                }
            }
        }
    }

    @Nullable
    private static IFluidHandler extractHandler(@Nullable Object object) {
        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) {
                return null;
            }
        }

        if (object instanceof ICapabilityProvider provider) {
            LazyOptional<IFluidHandler> cap = provider.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
            if (cap.isPresent()) {
                return (IFluidHandler)cap.orElseThrow(NullPointerException::new);
            }
        }

        if (object instanceof IFluidHandler handler) {
            return handler;
        } else {
            return null;
        }
    }

    private static int moveFluid(IFluidHandler from, int limit, IFluidHandler to) {
        return moveFluid(from, from.drain(limit, IFluidHandler.FluidAction.SIMULATE), limit, to);
    }

    private static int moveFluid(IFluidHandler from, FluidStack fluid, IFluidHandler to) {
        return moveFluid(from, from.drain(fluid, IFluidHandler.FluidAction.SIMULATE), fluid.getAmount(), to);
    }

    private static int moveFluid(IFluidHandler from, FluidStack extracted, int limit, IFluidHandler to) {
        if (extracted != null && extracted.getAmount() > 0) {
            extracted = extracted.copy();
            extracted.setAmount(Math.min(extracted.getAmount(), limit));
            int inserted = to.fill(extracted.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (inserted <= 0) {
                return 0;
            } else {
                extracted.setAmount(inserted);
                from.drain(extracted, IFluidHandler.FluidAction.EXECUTE);
                return inserted;
            }
        } else {
            return 0;
        }
    }

    // From ArgumentHelper in CC:T

    public static void assertBetween(double value, double min, double max, String message) throws LuaException {
        if (value < min || value > max || Double.isNaN(value)) {
            throw new LuaException(String.format(message, "between " + min + " and " + max));
        }
    }

    public static void assertBetween(int value, int min, int max, String message) throws LuaException {
        if (value < min || value > max) {
            throw new LuaException(String.format(message, "between " + min + " and " + max));
        }
    }

    @Nonnull
    public static <T extends IForgeRegistryEntry<T>> T getRegistryEntry(String name, String typeName, IForgeRegistry<T> registry) throws LuaException {
        ResourceLocation id;
        try {
            id = new ResourceLocation(name);
        } catch (ResourceLocationException var5) {
            id = null;
        }

        IForgeRegistryEntry value;
        if (id != null && registry.containsKey(id) && (value = registry.getValue(id)) != null) {
            return (T) value;
        } else {
            throw new LuaException(String.format("Unknown %s '%s'", typeName, name));
        }
    }
}

 */