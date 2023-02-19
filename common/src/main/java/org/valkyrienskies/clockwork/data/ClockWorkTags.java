package org.valkyrienskies.clockwork.data;


import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.valkyrienskies.clockwork.ClockWorkMod;


import java.util.List;
import java.util.function.Supplier;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;
import static org.valkyrienskies.clockwork.data.ClockWorkTags.NameSpace.MOD;

public class ClockWorkTags {

    public static <T> TagKey<T> optionalTag(Registry<T> registry,
                                            ResourceLocation id) {
        return TagKey.create(registry.key(), id);
    }

    public static <T> TagKey<T> forgeTag(Registry<T> registry, String path) {
        return optionalTag(registry, new ResourceLocation("c", path));
    }

    public static TagKey<Block> forgeBlockTag(String path) {
        return forgeTag(Registry.BLOCK, path);
    }

    public static TagKey<Item> forgeItemTag(String path) {
        return forgeTag(Registry.ITEM, path);
    }

    public static TagKey<Fluid> forgeFluidTag(String path) {
        return forgeTag(Registry.FLUID, path);
    }

    public enum NameSpace {
        MOD(ClockWorkMod.MOD_ID, false, true),
        FORGE("c")
        ;

        public final String id;
        public final boolean optionalDefault;
        public final boolean alwaysDatagenDefault;

        NameSpace(String id) {
            this(id, true, false);
        }

        NameSpace(String id, boolean optionalDefault, boolean alwaysDatagenDefault) {
            this.id = id;
            this.optionalDefault = optionalDefault;
            this.alwaysDatagenDefault = alwaysDatagenDefault;
        }
    }

    public enum AllFluidTags {
        STALE(MOD, "fuel/stale"),
        PLAIN(MOD, "fuel/plain"),
        SWEET(MOD, "fuel/sweet"),
        GOURMET(MOD, "fuel/gourmet"),
        EXTRA(MOD, "fuel/extra");

        public final TagKey<Fluid> tag;

        public final boolean alwaysDatagen;

        AllFluidTags() {
            this(MOD);
        }

        AllFluidTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }
        AllFluidTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }
        AllFluidTags(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }
        AllFluidTags(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optionalTag(Registry.FLUID, id);
            this.alwaysDatagen = alwaysDatagen;
        }
        @SuppressWarnings("deprecation")
        public boolean matches(Fluid fluid) {
            return fluid.is(tag);
        }
        public boolean matches(FluidState state) {
            return state.is(tag);
        }

        private static void init() {
        }
    }

    public enum AllBlockTags {
        BALLOON_BLOCK;
        public final TagKey<Block> tag;
        public final boolean alwaysDatagen;

        AllBlockTags() {
            this(MOD);
        }

        AllBlockTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        AllBlockTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        AllBlockTags(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        AllBlockTags(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optionalTag(Registry.BLOCK, id);
            this.alwaysDatagen = alwaysDatagen;
        }

        @SuppressWarnings("deprecation")
        public boolean matches(Block block) {
            return block.builtInRegistryHolder()
                    .is(tag);
        }

        public boolean matches(BlockState state) {
            return state.is(tag);
        }

        private static void init() {
        }
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



    public static void init() {
        AllBlockTags.init();
//        AllItemTags.init();
//        AllFluidTags.init();
    }
}
