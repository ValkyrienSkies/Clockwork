package org.valkyrienskies.clockwork.fabric

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.AtmosphereParametersResolver
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object RegisterResourceManagers {
    //idk java so i did this
    fun init() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            object : IdentifiableResourceReloadListener {
                override fun getFabricId() = ResourceLocation(ClockworkMod.MOD_ID, "atmosphere_parameters")

                override fun reload(
                    preparationBarrier: PreparableReloadListener.PreparationBarrier,
                    resourceManager: ResourceManager,
                    preparationsProfiler: ProfilerFiller,
                    reloadProfiler: ProfilerFiller,
                    backgroundExecutor: Executor,
                    gameExecutor: Executor
                ): CompletableFuture<Void?>? {
                    return AtmosphereParametersResolver.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor)
                }
            }
        )
    }
}