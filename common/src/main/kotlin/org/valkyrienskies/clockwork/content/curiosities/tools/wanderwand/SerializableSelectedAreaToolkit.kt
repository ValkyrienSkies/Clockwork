package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import org.joml.primitives.AABBd

data class SerializableSelectedAreaToolkit(val selectedAreas: HashSet<AABBd>,
                                           val selectionClusters: HashSet<Set<AABBd>>)
