package org.valkyrienskies.clockwork.content.contraptions.fan;

import org.joml.Vector3dc;

public class EncasedFanData {
    public final Vector3dc fanPos;
    public final Vector3dc fanDir;

    public double fanSpeed;

    public EncasedFanData(Vector3dc fanPos, Vector3dc fanDir, double fanSpeed) {
        this.fanPos = fanPos;
        this.fanDir = fanDir;
        this.fanSpeed = fanSpeed;
    }
}
