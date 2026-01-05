package org.valkyrienskies.clockwork.util.render

import net.minecraft.client.Minecraft
import java.util.concurrent.CopyOnWriteArrayList

object LightningManager {
    private val active = CopyOnWriteArrayList<LightningBolt>()

    fun spawn(
        startProvider: () -> net.minecraft.world.phys.Vec3,
        endProvider: () -> net.minecraft.world.phys.Vec3,
        seed: Long = System.nanoTime(),
        lifeTicks: Int = 6
    ) {
        val level = Minecraft.getInstance().level ?: return
        active += LightningBolt(
            startProvider = startProvider,
            endProvider = endProvider,
            seed = seed,
            birthGameTime = level.gameTime,
            lifeTicks = lifeTicks
        )
    }

    fun instances(now: Long): List<LightningBolt> {
        active.removeIf { !it.alive(now) }
        return active
    }
}
