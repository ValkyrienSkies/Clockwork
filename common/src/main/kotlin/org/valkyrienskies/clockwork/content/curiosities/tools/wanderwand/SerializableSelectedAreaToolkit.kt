package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import org.joml.primitives.AABBd
@Deprecated("This class is deprecated and will be removed in the future. Do not use it.")
data class SerializableSelectedAreaToolkit(val selectedAreas: HashSet<AABBd>, val selectionClusters: HashSet<Set<AABBd>>)
