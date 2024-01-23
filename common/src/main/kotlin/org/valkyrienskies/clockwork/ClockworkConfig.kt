package org.valkyrienskies.clockwork

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema

object ClockworkConfig {

    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()

    class Client {
        @JsonSchema(description = "Enable debug rendering")
        var debugRender = false
    }

    class Server {
        @JsonSchema(description = "Enable verbose debug logging")
        var debugMode = false

        @JsonSchema(description = "Kelvin tick rate (in Ticks / Second)")
        var kelvinTickRate = 400

        @JsonSchema(description = "Kelvin sub steps (per Tick)")
        var kelvinSubSteps = 10
    }

}