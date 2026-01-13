package org.valkyrienskies.clockwork.content.curiosities.meteor

import net.minecraft.client.multiplayer.ClientLevel

object MeteorManager {

    private val meteorStates: HashMap<Long, MeteorRenderer.MeteorVfxState> = HashMap()


    fun tick(level: ClientLevel) {

    }

    fun getState(shipId: Long): MeteorRenderer.MeteorVfxState? {
        return meteorStates[shipId]
    }
}
