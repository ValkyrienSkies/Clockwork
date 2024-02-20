package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.ClockworkMod;

import static net.minecraft.world.item.Items.BUCKET;

public class FabricClockworkFluids {


    public static void register() {}

//    public static FluidBuilder<SimpleFlowableFluid.Flowing, CreateRegistrate> frostingFluid(String name) {
//        return ClockWorkMod.REGISTRATE.fluid(name, Create.asResource("fluid/frosting_still"), Create.asResource("fluid/frosting_flow"));
//    }

}