package org.valkyrienskies.clockwork

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.config.ModConfig
import org.valkyrienskies.mod.api.config.VSConfigApi
import org.valkyrienskies.mod.api.config.VSConfigApi.buildForgeConfigSpec
import org.valkyrienskies.mod.api.config.VSConfigApi.update
import org.valkyrienskies.mod.common.config.ConfigType
import org.valkyrienskies.mod.common.hooks.VSGameEvents.ConfigUpdateEntry

object ClockworkConfigUpdater {

    fun update(config: ModConfig) {
        val updatedEntries = mutableSetOf<ConfigUpdateEntry>()

        server_config.update(config, ConfigType.SERVER, updatedEntries)
        client_config.update(config, ConfigType.CLIENT, updatedEntries)
    }

    private val server_config = VSConfigApi.buildVSConfigModel(ClockworkConfig.SERVER)
    val SERVER_SPEC: ForgeConfigSpec = buildForgeConfigSpec(
        configCategory = server_config.root,
        builder = ForgeConfigSpec.Builder()
    ).build()

    private val client_config = VSConfigApi.buildVSConfigModel(ClockworkConfig.CLIENT)
    val CLIENT_SPEC: ForgeConfigSpec = buildForgeConfigSpec(
        configCategory = client_config.root,
        builder = ForgeConfigSpec.Builder()
    ).build()
}