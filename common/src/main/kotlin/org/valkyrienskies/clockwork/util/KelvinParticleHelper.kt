package org.valkyrienskies.clockwork.util

import net.minecraft.client.multiplayer.ClientLevel
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType

object KelvinParticleHelper {
    fun spawnParticleWithRatio(level: ClientLevel, ductNodePos: DuctNodePos, pos: Vector3dc, speed: Vector3dc) {

        val network = ClockworkModClient.getKelvin()
        val gasMasses = network.getGasMassAt(ductNodePos)

        if (gasMasses.isEmpty()) return

        val sum = gasMasses.values.sum()
        var cumulative = 0.0
        var particleGas = gasMasses.keys.first()
        for ((gas, value) in gasMasses) {
            cumulative += value
            if (level.random.nextDouble() <= cumulative/sum) {
                particleGas = gas
                break
            }
        }

        network.createGasParticle(level, particleGas, ductNodePos, pos.x(), pos.y(), pos.z(), speed.x(), speed.y(), speed.z())
    }
}