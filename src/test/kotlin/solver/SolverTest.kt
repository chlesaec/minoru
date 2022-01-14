package solver

import board.Game
import board.Move
import board.PieceData
import board.PositionPiece
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


internal class SolverTest {

    var nbe : Long = 0L
    var total : Long = 0L

    fun incremente(done : Boolean) {
        total += 1
        if (done) {
            nbe += 1
        }
    }

    fun buildStepsContainer(width : Int,
                            positionPieces : Array<PositionPiece?>,
                            index : Int,
                            limit : Int) : solver.Steps {
        if (index == limit) {
            return StepContainer()
        }
        else {
            val indexPiece = positionPieces[index]?.piece?.index
            if (indexPiece != null) {
                return CompositeContainer(
                    indexPiece,
                    Array(width) {
                        this.buildStepsContainer(width, positionPieces, index + 1, limit)
                    })
            }
        }
        return StepContainer()
    }

    fun stepsBuilderTest(game : Game) : solver.Steps {
        val kpieces : Array<PositionPiece?> = game.start.pieces
        val nbre = Math.min(kpieces.size, 30)
        val steps =  this.buildStepsContainer(game.start.width,
            kpieces,
            0, nbre)

        return solver.StepsDecorator(steps, ::incremente)
    }

    @Test
    fun run() {
        val game = PieceData().game("config/10block.dat")
        var counter : Long = 0L
        val solver = solver.multi.Solver(game, ::stepsBuilderTest)
        println("started")

        val startTime = System.nanoTime()
        val moves: List<Move>? = solver.run()
        val duration = (System.nanoTime() - startTime) / 1_000_000L
        val board = game.start.copy()
        moves?.forEach { board.applyMove(it) }
        Assertions.assertTrue( board.match(game.target))
        println("duration = ${duration}")
        println("nbe = ${nbe}")
        println("total = ${total}")
    }
}