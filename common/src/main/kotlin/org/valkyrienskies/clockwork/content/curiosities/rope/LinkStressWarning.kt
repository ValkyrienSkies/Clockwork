package org.valkyrienskies.clockwork.content.curiosities.rope

internal object LinkStressWarning {
    const val START = 0.6f
    const val RELEASE = 0.5f

    fun volume(stress: Float): Float {
        if (!stress.isFinite() || stress < RELEASE) return 0f
        val progress = ((stress - START) / (1f - START)).coerceIn(0f, 1f)
        return 0.04f + 0.96f * progress * progress
    }
}
