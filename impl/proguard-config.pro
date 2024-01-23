#noinspection ShrinkerUnresolvedReference
-ignorewarnings
-dontoptimize
-dontshrink

-keep class kotlin.Metadata { *; }
-keep class
kotlin.**,
org.valkyrienskies.core.impl.program.**,
org.valkyrienskies.core.impl.util.**,
org.valkyrienskies.core.impl.config.**,
org.valkyrienskies.core.impl.networking.**,
org.valkyrienskies.core.impl.hooks.**,
org.valkyrienskies.core.impl.datastructures.**,
# serialization
org.valkyrienskies.core.impl.game.ships.serialization.**,
org.valkyrienskies.core.impl.game.ChunkClaimImpl,
org.valkyrienskies.core.impl.game.ChunkAllocator,
org.valkyrienskies.core.impl.chunk_tracking.ShipActiveChunksSet,
# for LOD
org.valkyrienskies.core.impl.collision.Lod1SolidShapeUtils,
org.valkyrienskies.core.impl.game.BlockTypeImpl,
# for config files
com.github.imifou.jsonschema.module.addon.annotation.*,
# for ship assembly hack + physics entity
org.valkyrienskies.core.impl.game.ships.ShipData,
org.valkyrienskies.core.impl.game.ships.ShipTransformImpl,
# for physics entity
org.valkyrienskies.core.impl.game.ships.*,
org.valkyrienskies.core.impl.game.phys_entities.**,
org.valkyrienskies.core.impl.game.ships.ShipTransformImpl$Companion,
org.valkyrienskies.core.impl.game.ShipTeleportDataImpl,
org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl,
org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld,
org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld
{ *; }


-keepattributes
InnerClasses,
Signature,
RuntimeVisibleAnnotations,
RuntimeVisibleParameterAnnotations,
RuntimeVisibleTypeAnnotations,
EnclosingMethod,
AnnotationDefault,
MethodParameters

-keepparameternames


-dontnote
org.joml.**,
org.apache.**,
io.netty.**,
kotlin.reflect.**,
kotlin.coroutines.**,
kotlinx.coroutines.**,
com.google.common.**,
com.fasterxml.jackson.**,
com.github.victools.**

-dontwarn
org.joml.**,
org.apache.**,
io.netty.**,
kotlin.reflect.**,
kotlin.coroutines.**,
kotlinx.coroutines.**,
com.google.common.**,
com.fasterxml.jackson.**,
com.github.victools.**

-repackageclasses org.valkyrienskies.core.impl.shadow

# https://stackoverflow.com/questions/33189249/how-to-tell-proguard-to-keep-enum-constants-and-fields
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}