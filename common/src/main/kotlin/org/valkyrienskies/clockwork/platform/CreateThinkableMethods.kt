package org.valkyrienskies.clockwork.platform

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

object CreateThinkableMethods {
    @JvmStatic @ExpectPlatform
    fun observedItems(observer: SmartObserverBlockEntity): Double? = throw AssertionError()

    @JvmStatic @ExpectPlatform
    fun observedFluid(observer: SmartObserverBlockEntity): Double? = throw AssertionError()

    @JvmStatic @ExpectPlatform
    fun tankFluid(tank: FluidTankBlockEntity, capacity: Boolean): Double = throw AssertionError()

    @JvmStatic @ExpectPlatform
    fun enchantPower(level: Level, pos: BlockPos): Double? = throw AssertionError()
}
