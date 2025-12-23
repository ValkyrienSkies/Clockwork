package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.foundation.data.CreateBlockEntityBuilder
import com.tterrag.registrate.AbstractRegistrate
import com.tterrag.registrate.builders.BlockEntityBuilder
import com.tterrag.registrate.builders.BuilderCallback
import com.tterrag.registrate.fabric.EnvExecutor
import com.tterrag.registrate.util.nullness.NonNullConsumer
import com.tterrag.registrate.util.nullness.NonNullSupplier
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
import net.fabricmc.api.EnvType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * This class is also a warcrime but whatever
 */
class ClockworkBlockEntityBuilder<T : BlockEntity, P>(owner: AbstractRegistrate<*>, parent: P, name: String,
                                                      callback: BuilderCallback, factory: BlockEntityFactory<T>
) : CreateBlockEntityBuilder<T, P>(
    owner, parent, name, callback, factory
    ) {

    private var cwVisualFactory: NonNullSupplier<ClockworkSimpleBlockEntityVisualFactory<T>>? = null
    private lateinit var cwRenderNormally: Predicate<T>

    fun clockworkVisual(
        visualFactory: NonNullSupplier<ClockworkSimpleBlockEntityVisualFactory<T>>?
    ): ClockworkBlockEntityBuilder<T, P> {
        return clockworkVisual(visualFactory, true)
    }

    fun clockworkVisual(
        visualFactory: NonNullSupplier<ClockworkSimpleBlockEntityVisualFactory<T>>?,
        renderNormally: Boolean
    ): ClockworkBlockEntityBuilder<T, P> {
        return clockworkVisual(visualFactory, Predicate { be: T? -> renderNormally })
    }

    fun clockworkVisual(
        visualFactory: NonNullSupplier<ClockworkSimpleBlockEntityVisualFactory<T>>?,
        renderNormally: Predicate<T>
    ): ClockworkBlockEntityBuilder<T, P> {
        if (this.cwVisualFactory == null) {
            ClockworkRegistrate.onClient { Runnable { this.registerVisualizerClockwork() } }
        }

        this.cwVisualFactory = visualFactory
        this.cwRenderNormally = renderNormally

        return this
    }

    protected fun registerVisualizerClockwork() {
        this.onRegister(NonNullConsumer { entry: BlockEntityType<T> ->
            Objects.requireNonNull<NonNullSupplier<ClockworkSimpleBlockEntityVisualFactory<T>>>(this.cwVisualFactory)
            val renderNormally: Predicate<T> = this.cwRenderNormally
            SimpleBlockEntityVisualizer.builder<T>(this.getEntry())
                .factory(this.cwVisualFactory!!.get()::create)
                .skipVanillaRender(Predicate { be: T -> !renderNormally.test(be) })
                .apply()
        })
    }

    companion object {
        fun <T : BlockEntity, P> create(
            owner: AbstractRegistrate<*>, parent: P,
            name: String, callback: BuilderCallback, factory: BlockEntityFactory<T>
        ): BlockEntityBuilder<T, P> {
            return ClockworkBlockEntityBuilder<T, P>(owner, parent, name, callback, factory)
        }
    }
}