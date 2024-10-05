package org.valkyrienskies.clockwork.effekseer.client.internal

import net.minecraft.client.player.LocalPlayer

interface EffekFpvRenderer {
    fun `vsclockwork$renderFpvEffek`(partial: Float, player: LocalPlayer?)
}