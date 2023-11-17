package org.valkyrienskies.clockwork.data

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import org.valkyrienskies.clockwork.ClockworkMod

object ClockworkTags {
    fun <T> optionalTag(
        registry: Registry<T>,
        id: ResourceLocation?
    ): TagKey<T> {
        return TagKey.create(registry.key(), id)
    }

    fun <T> forgeTag(registry: Registry<T>, path: String?): TagKey<T> {
        return optionalTag(registry, ResourceLocation("c", path))
    }

    fun forgeBlockTag(path: String?): TagKey<Block> {
        return forgeTag(BuiltInRegistries.BLOCK, path)
    }

    fun forgeItemTag(path: String?): TagKey<Item> {
        return forgeTag(BuiltInRegistries.ITEM, path)
    }

    fun forgeFluidTag(path: String?): TagKey<Fluid> {
        return forgeTag(BuiltInRegistries.FLUID, path)
    }

    //    public static class BlockCW {
    //        public static final TagKey<Block>
    //        BALLOON_BLOCK = createAndGenerateBlockTag(ClockWorkMod.asResource("balloon_block"));
    //
    //
    //
    //        public static TagKey<Block> createAndGenerateBlockTag(ResourceLocation loc) {
    //            TagKey<Block> tag = BlockTags.create(loc);
    //            REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, prov -> prov.tag(tag));
    //            return tag;
    //        }
    //
    //        public static void addBlocksToBlockTag(TagKey<Block> tag, Block... blocks) {
    //            REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, prov -> {
    //                prov.tag(tag).add(blocks);
    //            });
    //        }
    //
    //        public static void addBlocksToBlockTag(TagKey<Block> tag, Supplier<List<? extends Block>> blocks) {
    //            REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, prov -> {
    //                TagsProvider.TagAppender<Block> app = prov.tag(tag);
    //                for (Block b : blocks.get()) {
    //                    app.add(b);
    //                }
    //            });
    //        }
    //
    //        public static void addTagsToBlockTag(TagKey<Block> tag, Supplier<List<TagKey<Block>>> tags) {
    //            REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, prov -> {
    //                TagsProvider.TagAppender<Block> app = prov.tag(tag);
    //                tags.get().forEach(app::addTag);
    //            });
    //        }
    //
    //        public static void sectionRegister() {}
    //    }
    fun init() {
        AllBlockTags.init()
        //        AllItemTags.init();
//        AllFluidTags.init();
    }

    enum class NameSpace @JvmOverloads constructor(
        val id: String,
        val optionalDefault: Boolean = true,
        val alwaysDatagenDefault: Boolean = false
    ) {
        MOD(ClockworkMod.MOD_ID, false, true),
        FORGE("c")
    }

    enum class AllFluidTags @JvmOverloads constructor(
        namespace: NameSpace,
        path: String?,
        optional: Boolean = namespace.optionalDefault,
        alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
    ) {
        STALE(NameSpace.MOD, "fuel/stale"),
        PLAIN(NameSpace.MOD, "fuel/plain"),
        SWEET(NameSpace.MOD, "fuel/sweet"),
        GOURMET(NameSpace.MOD, "fuel/gourmet"),
        EXTRA(NameSpace.MOD, "fuel/extra");

        val tag: TagKey<Fluid>
        val alwaysDatagen: Boolean

        @JvmOverloads
        constructor(
            namespace: NameSpace = NameSpace.MOD,
            optional: Boolean = namespace.optionalDefault,
            alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
        ) : this(namespace, null, optional, alwaysDatagen)

        init {
            val id = ResourceLocation(
                namespace.id,
                path ?: Lang.asId(name)
            )
            tag = optionalTag(BuiltInRegistries.FLUID, id)
            this.alwaysDatagen = alwaysDatagen
        }

        @Suppress("deprecation")
        fun matches(fluid: Fluid?): Boolean {
            return fluid?.`is`(tag) ?: false
        }

        fun matches(state: FluidState?): Boolean {
            return state?.`is`(tag) ?: false
        }

        companion object {
            fun isValidFuel(fuel: Fluid?): Boolean {
                if (STALE.matches(fuel)) return true
                if (PLAIN.matches(fuel)) return true
                if (SWEET.matches(fuel)) return true
                return if (EXTRA.matches(fuel)) true else GOURMET.matches(fuel)
            }

            private fun init() {}
        }
    }

    enum class AllBlockTags @JvmOverloads constructor(
        namespace: NameSpace,
        path: String?,
        optional: Boolean = namespace.optionalDefault,
        alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
    ) {
        BALLOON_BLOCK;

        val tag: TagKey<Block>
        val alwaysDatagen: Boolean

        @JvmOverloads
        constructor(
            namespace: NameSpace = NameSpace.MOD,
            optional: Boolean = namespace.optionalDefault,
            alwaysDatagen: Boolean = namespace.alwaysDatagenDefault
        ) : this(namespace, null, optional, alwaysDatagen)

        init {
            val id = ResourceLocation(
                namespace.id,
                path ?: Lang.asId(name)
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
