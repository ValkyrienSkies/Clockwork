package org.valkyrienskies.clockwork.compat.thinkable

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.api.thinkable.ThinkableRegistry
import java.util.IdentityHashMap
import java.util.function.BiFunction
import java.util.function.Function

class ThinkableProviderRegistry : ThinkableRegistry {
    private val blocks = IdentityHashMap<Block, Function<ThinkableContext, Thinkable?>>()
    private val entities = IdentityHashMap<BlockEntityType<*>, (ThinkableContext) -> Thinkable?>()
    private var frozen = false

    override fun registerBlock(block: Block, provider: Function<ThinkableContext, Thinkable?>) {
        check(!frozen) { "Thinkable registration is closed" }
        check(!blocks.containsKey(block)) { "Duplicate Thinkable provider for block ${BuiltInRegistries.BLOCK.getKey(block)}" }
        blocks[block] = provider
    }

    override fun <T : BlockEntity> registerBlockEntity(type: BlockEntityType<T>, provider: BiFunction<ThinkableContext, T, Thinkable?>) {
        check(!frozen) { "Thinkable registration is closed" }
        check(!entities.containsKey(type)) { "Duplicate Thinkable provider for block entity ${BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)}" }
        entities[type] = { context ->
            @Suppress("UNCHECKED_CAST")
            provider.apply(context, context.blockEntity() as T)
        }
    }

    fun find(context: ThinkableContext): Thinkable? {
        context.blockEntity()?.let { entity -> entities[entity.type]?.invoke(context)?.let { return it } }
        return blocks[context.state().block]?.apply(context)
    }

    fun freeze() { frozen = true }
}
