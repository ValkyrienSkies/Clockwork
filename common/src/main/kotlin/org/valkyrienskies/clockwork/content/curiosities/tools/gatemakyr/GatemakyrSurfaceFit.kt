package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

object GatemakyrSurfaceFit {
    data class Rectangle(val u: Int, val v: Int, val width: Int, val height: Int)

    fun fit(maxWidth: Int, maxHeight: Int = maxWidth, supported: (Int, Int) -> Boolean): Rectangle? {
        val w = maxWidth.coerceIn(1, 32)
        val h = maxHeight.coerceIn(1, 32)
        val rows = (1 - h until h).associateWith { v ->
            if (!supported(0, v)) null else {
                var left = 0; var right = 0
                while (left > 1 - w && supported(left - 1, v)) left--
                while (right < w - 1 && supported(right + 1, v)) right++
                left..right
            }
        }
        var best: Rectangle? = null
        var bestArea = 0
        var bestOffset = Double.POSITIVE_INFINITY
        for (bottom in 1 - h..0) {
            var left = 1 - w; var right = w - 1
            for (top in bottom until bottom + h) {
                val row = rows[top] ?: break
                left = maxOf(left, row.first); right = minOf(right, row.last)
                if (top < 0) continue
                val width = minOf(w, right - left + 1)
                val height = top - bottom + 1
                val u = (-width / 2).coerceIn(left, right - width + 1)
                val centerU = u + (width - 1) / 2.0
                val centerV = bottom + (height - 1) / 2.0
                val offset = centerU * centerU + centerV * centerV
                if (width * height > bestArea || width * height == bestArea && offset < bestOffset) {
                    best = Rectangle(u, bottom, width, height)
                    bestArea = width * height
                    bestOffset = offset
                }
            }
        }
        return best
    }
}
