package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.utility.Components
import com.simibubi.create.foundation.utility.LangBuilder
import com.simibubi.create.foundation.utility.LangNumberFormat
import io.github.fabricators_of_create.porting_lib.util.FluidStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import java.util.*

object ClockworkLang {

    @JvmStatic
    fun translateDirect(key: String, vararg args: Any?): MutableComponent {
        return Components.translatable(ClockworkMod.MOD_ID + "." + key, *resolveBuilders(arrayOf(args)))
    }

    @JvmStatic
    fun translateDirect(key: String): MutableComponent {
        return Components.translatable("${ClockworkMod.MOD_ID}.$key")
    }

    @JvmStatic
    fun asId(name: String): String {
        return name.lowercase(Locale.ROOT)
    }

    @JvmStatic
    fun nonPluralId(name: String): String {
        val asId = asId(name)
        return if (asId.endsWith("s")) asId.substring(0, asId.length - 1) else asId
    }

    @JvmStatic
    fun translatedOptions(prefix: String?, vararg keys: String): List<Component> {
        val result: MutableList<Component> = ArrayList(keys.size)
        for (key in keys) {
            result.add(translate("${prefix.orEmpty()}.$key").component())
        }
        return result
    }

    fun builder(): LangBuilder {
        return LangBuilder(ClockworkMod.MOD_ID)
    }

    fun builder(namespace: String): LangBuilder {
        return LangBuilder(namespace)
    }

    fun blockName(state: BlockState): LangBuilder {
        return builder().add(state.block.name)
    }

    fun itemName(stack: ItemStack): LangBuilder {
        return builder().add(stack.hoverName.copy())
    }

    fun fluidName(stack: FluidStack): LangBuilder {
        return builder().add(stack.displayName.copy())
    }

    fun number(d: Double): LangBuilder {
        return builder().text(LangNumberFormat.format(d))
    }

    fun translate(langKey: String, vararg args: Any): LangBuilder {
        return builder().translate(langKey, args)
    }

    fun text(text: String): LangBuilder {
        return builder().text(text)
    }

    fun resolveBuilders(args: Array<Any?>): Array<Any?> {
        for (i in args.indices) {
            if (args[i] is LangBuilder) {
                val cb = (args[i] as LangBuilder)
                args[i] = cb.component()
            }
        }
        return args
    }
}
