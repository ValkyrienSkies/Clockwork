package org.valkyrienskies.clockwork.util.render

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.mixin.client.SpriteContentsAccessor

/** One bounded atlas, shared by all surface passes, so a varied ship does not cause per-block draws. */
object SurfaceEffectAtlas {
    val LOCATION = ResourceLocation("vs_clockwork", "dynamic/tool_surface_effects")
    private const val PIXELS = 32
    private const val TILE = PIXELS + 2 // Duplicate edge texels prevent neighbouring sprites bleeding in.
    private const val SIDE = 32
    private const val SIZE = SIDE * TILE
    private var texture: DynamicTexture? = null
    private val slots = mutableMapOf<ResourceLocation, Slot>()
    private val fallback = Slot(1, 1, 1, 1)

    class Slot(private val x: Int, private val y: Int, private val width: Int, private val height: Int) {
        fun u(fraction: Float, frozen: Boolean): Float =
            (x + fraction.coerceIn(0f, 1f) * width + if (frozen) SIZE else 0) / (SIZE * 2f)
        fun v(fraction: Float): Float = (y + fraction.coerceIn(0f, 1f) * height) / SIZE.toFloat()
    }

    fun clear() {
        if (texture != null) Minecraft.getInstance().textureManager.release(LOCATION)
        texture = null
        slots.clear()
    }

    fun get(sprite: TextureAtlasSprite): Slot {
        val contents = sprite.contents()
        slots[contents.name()]?.let { return it }
        val atlas = texture ?: DynamicTexture(SIZE * 2, SIZE, true).also {
            // Reserve a neutral fallback tile. Memory stays bounded even in very large modpacks.
            it.pixels!!.fillRect(0, 0, TILE, TILE, -1)
            it.pixels!!.fillRect(SIZE, 0, TILE, TILE, 0xFFB0B0B0.toInt())
            it.upload()
            Minecraft.getInstance().textureManager.register(LOCATION, it)
            texture = it
        }
        if (slots.size >= SIDE * SIDE - 1) return fallback
        val source = (contents as SpriteContentsAccessor).`clockwork$getOriginalImage`()
        val width = contents.width().coerceAtMost(PIXELS)
        val height = contents.height().coerceAtMost(PIXELS)
        val index = slots.size + 1
        val x = index % SIDE * TILE
        val y = index / SIDE * TILE
        // Freeze an animation frame as well as its colors. No GPU readback or image loading per frame.
        val columns = source.width / contents.width()
        val frames = columns * (source.height / contents.height())
        val frame = contents.uniqueFrames.findFirst().orElse(0) % frames
        val frameX = frame % columns * contents.width()
        val frameY = frame / columns * contents.height()
        val pixels = atlas.pixels!!
        for (dy in -1..height) for (dx in -1..width) {
            val sx = dx.coerceIn(0, width - 1) * contents.width() / width
            val sy = dy.coerceIn(0, height - 1) * contents.height() / height
            val pixel = source.getPixelRGBA(frameX + sx, frameY + sy)
            pixels.setPixelRGBA(x + dx + 1, y + dy + 1, (pixel and -0x1000000) or 0xFFFFFF)
            pixels.setPixelRGBA(SIZE + x + dx + 1, y + dy + 1, SurfaceEffectColor.greyPixel(pixel))
        }
        atlas.bind()
        pixels.upload(0, x, y, x, y, width + 2, height + 2, false, false)
        pixels.upload(0, SIZE + x, y, SIZE + x, y, width + 2, height + 2, false, false)
        return Slot(x + 1, y + 1, width, height).also { slots[contents.name()] = it }
    }
}
