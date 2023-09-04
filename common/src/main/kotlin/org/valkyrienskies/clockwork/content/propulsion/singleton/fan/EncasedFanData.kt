package org.valkyrienskies.clockwork.content.propulsion.singleton.fan

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3dc

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EncasedFanData(val fanPos: Vector3dc, val fanDir: Vector3dc, var fanSpeed: Double)