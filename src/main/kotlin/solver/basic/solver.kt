package solver.basic

import board.Game
import board.Move
import solver.Step
import solver.Steps
import solver.stepsBuilder
import java.util.*
import kotlin.collections.ArrayList


class Solver(val game : Game, val builder : (Game) -> Steps = ::stepsBuilder) {
    val steps : Steps = builder(game)

    val stack = ArrayDeque<Step>()

    var winStep : Step? = null

    private fun explore(prec: Step? = null) {

        val board = if (prec != null) {
            game.start.copy{ prec.positionPieces[it] }
            //createBoard(prec.positionPieces, prec.apply(game.start))
        }
        else {
            this.game.start
        }
        if (board.match(this.game.target)) {
            this.winStep = prec
            this.stack.clear()
            return
        }
        val moves  : Iterable<Move> = board.allPossibleMoves()
        moves.map {  if (prec == null) Step.create(board, it)  else Step.create(prec, it) }
            .filter { this.steps.add(it) } // add to steps only if not already
            .forEach { this.stack.addLast(it) }

    }

    fun run() : List<Move>? {
        this.explore()

        while (!this.stack.isEmpty()) {
            val step = this.stack.removeFirst()
            this.explore(step)
        }
        val ws = this.winStep
        if (ws != null) {
            val moves = ArrayList<Move>()
            ws.applyFromStart {
                step : Step -> moves.add(step.move)
            }
            return moves
        }
        else {
            return null
        }
    }

}