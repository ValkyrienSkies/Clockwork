package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class InfuserProgress : SavedData() {
    private val activated = hashSetOf<UUID>()
    fun activate(player: UUID): Boolean = activated.add(player).also { if (it) setDirty() }

    override fun save(tag: CompoundTag): CompoundTag = tag.also {
        it.put("players", ListTag().also { list ->
            activated.forEach { id -> list.add(CompoundTag().also { entry -> entry.putUUID("id", id) }) }
        })
    }

    companion object {
        fun load(tag: CompoundTag) = InfuserProgress().also { data ->
            tag.getList("players", Tag.TAG_COMPOUND.toInt()).forEach {
                val entry = it as CompoundTag
                if (entry.hasUUID("id")) data.activated.add(entry.getUUID("id"))
            }
        }
        fun get(level: ServerLevel): InfuserProgress = level.server.overworld().dataStorage.computeIfAbsent(
            ::load, ::InfuserProgress, "clockwork_infuser_progress")
    }
}
