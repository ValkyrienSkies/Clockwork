package org.valkyrienskies.clockwork.content.curiosities.spring

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks

internal object SpringClipboard {
    private const val KEY = "vs_clockwork:Spring"

    fun isClipboard(stack: ItemStack) = AllBlocks.CLIPBOARD.isIn(stack)

    fun hasSettings(stack: ItemStack) = stack.getTagElement(KEY) != null

    fun copy(stack: ItemStack, link: WandLinks.Link) {
        stack.orCreateTag.put(KEY, snapshot(link))
        ClipboardOverrides.switchTo(ClipboardOverrides.ClipboardType.WRITTEN, stack)
    }

    fun snapshot(link: WandLinks.Link) = link.settings.save().also { it.putDouble("length", link.length) }

    fun paste(stack: ItemStack, link: WandLinks.Link, includeLength: Boolean): Boolean {
        val data = stack.getTagElement(KEY) ?: return false
        apply(data, link, includeLength)
        return true
    }

    fun apply(data: CompoundTag, link: WandLinks.Link, includeLength: Boolean) {
        link.settings = SpringSettings.load(data)
        if (includeLength && data.contains("length", Tag.TAG_ANY_NUMERIC.toInt())) {
            val length = data.getDouble("length")
            if (length.isFinite()) link.length = length.coerceIn(0.5, 128.0)
        }
    }
}
