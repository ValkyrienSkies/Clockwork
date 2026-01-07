package org.valkyrienskies.clockwork.content.curiosities.meteor

object SphereMesh {
    data class Mesh(
        val positions: FloatArray, // xyz
        val normals: FloatArray,   // xyz
        val uvs: FloatArray,       // uv
        val indices: IntArray
    )

    val mesh: Mesh = buildUvSphere(longSegs = 16, latSegs = 10)

    fun buildUvSphere(longSegs: Int, latSegs: Int): Mesh {
        require(longSegs >= 3)
        require(latSegs >= 2)

        val cols = longSegs + 1
        val rows = latSegs + 1
        val vertCount = cols * rows

        val pos = FloatArray(vertCount * 3)
        val nrm = FloatArray(vertCount * 3)
        val uv  = FloatArray(vertCount * 2)

        var vtx = 0
        for (iy in 0..latSegs) {
            val t = iy.toDouble() / latSegs.toDouble()   // 0..1
            val theta = Math.PI * t
            val sinT = Math.sin(theta)
            val cosT = Math.cos(theta)

            for (ix in 0..longSegs) {
                val s = ix.toDouble() / longSegs.toDouble() // 0..1
                val phi = 2.0 * Math.PI * s
                val sinP = Math.sin(phi)
                val cosP = Math.cos(phi)

                val x = (sinT * cosP).toFloat()
                val y = (cosT).toFloat()
                val z = (sinT * sinP).toFloat()

                pos[vtx*3+0] = x; pos[vtx*3+1] = y; pos[vtx*3+2] = z
                nrm[vtx*3+0] = x; nrm[vtx*3+1] = y; nrm[vtx*3+2] = z

                // Standard UV sphere mapping
                uv[vtx*2+0] = s.toFloat()
                uv[vtx*2+1] = (1.0 - t).toFloat()

                vtx++
            }
        }

        val triCount = longSegs * latSegs * 2
        val idx = IntArray(triCount * 3)
        var ii = 0
        for (iy in 0 until latSegs) {
            for (ix in 0 until longSegs) {
                val a = iy * cols + ix
                val b = a + 1
                val c = a + cols
                val d = c + 1
                idx[ii++] = a; idx[ii++] = c; idx[ii++] = b
                idx[ii++] = b; idx[ii++] = c; idx[ii++] = d
            }
        }

        return Mesh(pos, nrm, uv, idx)
    }
}

