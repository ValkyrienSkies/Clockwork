package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

object ClockworkTags {

    fun <T> optionalTag(
        registry: Registry<T>,
        id: ResourceLocation?
    ): TagKey<T> {
        return TagKey.create(registry.key(), id)
    }

    fun init() {
        AllBlockTags.init()
    }

    enum class NameSpace constructor(
        val id: String,
        val optionalDefault: Boolean = true,
        val alwaysDatagenDefault: Boolean = false
    ) {
        MOD(ClockworkMod.MOD_ID, false, true),
        FORGE("c")
    }

    enum class AllBlockTags constructor(
        namespace: NameSpace,
        path: String?,
        optional: Boolean = namespace.optionalDefault,
        alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
    ) {
        BALLOON_BLOCK;

        val tag: TagKey<Block>
        val alwaysDatagen: Boolean

        constructor(
            namespace: NameSpace = NameSpace.MOD,
            optional: Boolean = namespace.optionalDefault,
            alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
        ) : this(namespace, null, optional, alwaysDatagen)

        init {
            val id = ResourceLocation(
                namespace.id,
                path ?: ClockworkLang.asId(name)
            )
            tag = optionalTag(BuiltInRegistries.BLOCK, id)
            this.alwaysDatagen = alwaysDatagen
        }

        @Suppress("deprecation")
        fun matches(block: Block): Boolean {
            return block.builtInRegistryHolder()
                .`is`(tag)
        }

        fun matches(state: BlockState): Boolean {
            return state.`is`(tag)
        }

        companion object {
            fun init() {}
        }
    }
}