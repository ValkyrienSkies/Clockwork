package org.valkyrienskies.clockwork

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema

object ClockworkConfig {
    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()

    class Client

    class Server {
        // Blacklist of blocks that don't get added for ship building
        @JsonSchema(description = "Blacklist of blocks that don't get assembled")
        var blockBlacklist = setOf(
            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:end_portal",
            "minecraft:end_gateway",
            "minecraft:portal",
            "minecraft:air",
            "minecraft:water",
            "minecraft:flowing_water",
            "minecraft:lava",
            "minecraft:flowing_lava"
        )
    }
}