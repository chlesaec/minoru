package solver

import board.Board
import board.Coord
import board.Game
import board.Move

class Step(val positionPieces : Array<Coord?>,
           val move : Move,
           private val precedent : Step? = null) {

    val length : Int = if (precedent == null) 1 else precedent.length + 1

    companion object {
        fun create(origin : Board, m : Move) : Step {
            val next = origin.copy()
            next.applyMove(m)
            val allKeyPos = Array(origin.pieces.size) {
                next.pieces[it]?.position
            }
            return Step(allKeyPos, m)
        }

        fun create(precedent : Step, m : Move) : Step {
            return precedent.applyMove(m)
        }
    }

    fun applyMove(m : Move) : Step {
        val currentPos : Coord? = this.positionPieces[m.indexPiece]
        return if (currentPos != null) {
            val newPos = m.dir.apply(currentPos)
            val newPositions = Array(this.positionPieces.size) {
                if (it == m.indexPiece) {
                    newPos
                } else {
                    this.positionPieces[it]
                }
            }
            Step(newPositions, m, this)
        }
        else  {
            this
        }
    }

    fun isSameResult(step : Step) : Boolean {
        return this.positionPieces == step.positionPieces
                && this.move == step.move
    }


    fun applyFromStart(f : (Step) -> Unit) {
        if (this.precedent != null) {
            this.precedent.applyFromStart(f)
        }
        f(this)
    }
}

private class Coords(val coords : Array<Coord?>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coords

        if (!coords.contentEquals(other.coords)) return false

        return true
    }

    override fun hashCode(): Int {
        return coords.contentHashCode()
    }
}

sealed class Steps {
    abstract fun add(step : Step) : Boolean
}
class StepContainer() : Steps() {
    private val steps = HashMap<Coords, Step>()

    override fun add(step: Step): Boolean {
        val coords = Coords(step.positionPieces)
        val step1 = this.steps[coords]
        if (step1 == null) {
            this.steps[coords] = step
            return true
        }
        if (step1.length > step.length) {
            steps[coords] = step
        }
        return false
    }
}
class CompositeContainer(val indexPiece : Int,
                         val nexts : Array<Steps>) : Steps() {

    override fun add(step: Step): Boolean {
        val piece = step.positionPieces[this.indexPiece]
        if (piece != null) {
            val stepsNext = nexts[piece.col]
            if (stepsNext != null) {
                return stepsNext.add(step)
            }
        }
        return false
    }
}
class StepsDecorator(val s : Steps, val f : (Boolean) -> Unit) : Steps() {
    override fun add(step : Step) : Boolean {
        val res = this.s.add(step)
        f(res)
        return res
    }
}

fun stepsBuilder(game : Game) : Steps {
    val kpieces = game.start.pieces
    val nbre = Math.min(kpieces.size, 30)

    return if (nbre < 4) {
        CompositeContainer(0,
            Array(game.start.width) { StepContainer() })
    }
    else {
        CompositeContainer(0,
            Array(game.start.width) {
                CompositeContainer(1,
                    Array(game.start.width) {
                        CompositeContainer(2,
                            Array(game.start.width) { CompositeContainer(3,
                                Array(game.start.width) { StepContainer() }) })
                    })
            })
    }
}