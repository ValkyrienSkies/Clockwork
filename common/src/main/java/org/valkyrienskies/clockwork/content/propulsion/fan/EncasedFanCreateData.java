package org.valkyrienskies.clockwork.content.propulsion.fan;

import org.joml.Vector3dc;

public record EncasedFanCreateData(Vector3dc fanPos, Vector3dc fanDir, double fanSpeed) {
}
