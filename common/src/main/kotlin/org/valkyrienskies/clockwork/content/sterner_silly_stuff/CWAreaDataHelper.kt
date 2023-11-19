package org.valkyrienskies.clockwork.content.sterner_silly_stuff

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import kotlin.collections.HashSet


class CWAreaDataHelper {

    companion object {

        fun load(nbt: CompoundTag?): SelectedAreaToolkit {
            val toolKit = SelectedAreaToolkit()
            if (nbt != null) {
                toolKit.overwriteFrom(getMapper().readValue<SelectedAreaToolkit>(nbt.getByteArray("SelectedData")))
            }
            return toolKit
        }

        private fun getMapper(): ObjectMapper {
            return VSJacksonUtil.defaultMapper
        }

        fun save(compoundTag: CompoundTag, area: SelectedAreaToolkit): CompoundTag {
            compoundTag.putByteArray("SelectedData", getMapper().writeValueAsBytes(area))
            return compoundTag
        }
    }
}
