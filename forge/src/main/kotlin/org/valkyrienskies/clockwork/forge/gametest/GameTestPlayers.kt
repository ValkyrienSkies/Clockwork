package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.mod.common.ValkyrienSkiesMod

internal fun ServerLevel.removeTestPlayer(player: ServerPlayer) {
    server.playerList.remove(player)
    val releaseAt = server.tickCount + 2
    ValkyrienSkiesMod.api.tickEndEvent.on { _, listener ->
        if (player.server.tickCount >= releaseAt || player.server.isStopped) listener.unregister()
    }
}
