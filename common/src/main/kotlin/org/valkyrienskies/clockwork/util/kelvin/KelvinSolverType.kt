package org.valkyrienskies.clockwork.util.kelvin

import org.valkyrienskies.kelvin.api.KelvinSolver
import org.valkyrienskies.kelvin.impl.solvers.ClassicSolver
import org.valkyrienskies.kelvin.impl.solvers.JacobiSimplifiedSolver
import org.valkyrienskies.kelvin.impl.solvers.JacobiSolver

enum class KelvinSolverType {
    CLASSIC,     JACOBI_SIMPLIFIED,     JACOBI;

    fun getSolver(): KelvinSolver {
        return when (this) {
            CLASSIC -> ClassicSolver()
            JACOBI_SIMPLIFIED -> JacobiSimplifiedSolver()
            JACOBI -> JacobiSolver()
        }
    }
}