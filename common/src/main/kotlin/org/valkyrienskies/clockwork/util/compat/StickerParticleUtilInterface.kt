package org.valkyrienskies.clockwork.util.compat

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.function.Supplier

interface StickerParticleUtilInterface {
    fun doBluperParticle(level: Level?, worldPosition: BlockPos?, facing: Direction?)
    fun runOnClient(func: Supplier<Runnable?>?)
}