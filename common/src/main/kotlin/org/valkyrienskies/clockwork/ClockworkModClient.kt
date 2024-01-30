package org.valkyrienskies.clockwork;

import com.simibubi.create.foundation.outliner.Outliner

object ClockworkModClient {
    @JvmStatic
    val OUTLINER: Outliner = Outliner()


    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    fun initClient() {
        ClockworkPonderScenes.init()
    }

}
