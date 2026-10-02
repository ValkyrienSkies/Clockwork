package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

/** Server-confirmed actions. HOLD/IDLE are snapshots for clients entering tracking range. */
enum class GravitronAction(val duration: Float) {
    IDLE(0f), DRAW(18f), GRAB(12f), HOLD(0f), RELEASE(10f),
    LAUNCH(20f), FREEZE(18f), UNFREEZE(22f), OVERLOAD(32f)
}
