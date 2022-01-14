package solver.multi

import board.Game
import board.Move
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import solver.Step
import solver.Steps
import solver.stepsBuilder
import kotlin.coroutines.coroutineContext


class Solver(val game : Game, val builder : (Game) -> Steps = ::stepsBuilder) {
    private val steps : Steps = builder(game)

    private val stack = Channel<Step>(400)

    var winStep : Step? = null

    private suspend fun explore(prec: Step? = null) {
        val board = if (prec != null) {
            game.start.copy{ prec.positionPieces[it] }
        }
        else {
            this.game.start
        }

        if (board.match(this.game.target)) {
            this.winStep = prec
            this.stack.close()
            return
        }

        val moves  : Iterable<Move> = board.allPossibleMoves()

        moves.map { if (prec == null) Step.create(board, it) else Step.create(prec, it) }
                .filter { this.steps.add(it) } // add to steps only if not already
                .forEach {
                    if (this.winStep == null && !this.stack.isClosedForSend) {
                        GlobalScope.launch {
                            try {
                                this@Solver.stack.send(it)
                            }
                            catch (ex : ClosedSendChannelException)  {}
                        }
                    }
                }
    }

    private suspend fun doRun() {
        this.explore()
        //this.stack.onReceive.
        for (step in this.stack) {
            if (this.winStep == null) {
                /*coroutineScope {
                    this@Solver.explore(step)
                }*/
                GlobalScope.launch {
                    this@Solver.explore(step)
                }
            }
        }
    }

    fun run() : List<Move>? {
        runBlocking  {
            this@Solver.doRun()
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