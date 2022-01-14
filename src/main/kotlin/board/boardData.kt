package board

import java.util.*

sealed class Block
object freeBlok : Block()
object unreachableBloc : Block()
class  PieceBlock(val piece : Piece) : Block()

data class Coord(val line : Int, val col : Int) :
    Comparable<Coord> {

    constructor(x : Double, y : Double) : this(x.toInt(), y.toInt())

    inline operator fun plus(increment: Coord): Coord {
        return Coord(this.line + increment.line, this.col + increment.col)
    }

    inline fun inside(heigth : Int, width : Int) : Boolean {
        return this.line >= 0 && this.line < heigth
                && this.col >= 0 && this.col < width
    }

    fun findDirection(to : Coord, minD : Double) : Direction? {
        val deltaX = to.col - this.col
        val deltaY = to.line - this.line
        val direction : Direction? = if (Math.abs(deltaX) > Math.abs(deltaY)) {
            //horizontal move
            if (deltaX > minD) {
                Direction.RIGHT
            }
            else if (deltaX < -minD) {
                Direction.LEFT
            }
            else {
                null
            }
        }
        else {
            if (deltaY > minD) {
                Direction.DOWN
            }
            else if (deltaY < -minD) {
                Direction.UP
            }
            else {
                null
            }
        }

        return direction
    }

    override fun compareTo(other: Coord): Int {
        return compareValuesBy(this, other,
            { it.line },
            { it.col })
    }
}

open class Frame(val height : Int,
                 val width : Int,
                 val blocks : Array<Array<Boolean>>) {
    private fun isBlock(position : Coord) : Boolean {
        return position.inside(this.height, this.width)
                && this.blocks[position.line][position.col];
    }

    fun <T> forEachBlock(start : T, merge : (T, T) -> T, f : (Coord) -> T) : T {
        var current = start
        for (i in 0 until height) {
            for (j in 0 until width) {
                val c = Coord(i, j)
                if (this.isBlock(c)) {
                    val next = f(c)
                    current = merge(current, next)
                }
            }
        }
        return current
    }
}

open class Piece(val ident : Char,
                 val index : Int,
                 height : Int,
                 width : Int,
                 blocks : Array<Array<Boolean>>) : Frame(height, width, blocks) {
}

class PositionPiece(var position: Coord,
                    val piece : Piece) {
    private fun inFrame(pos : Coord) : Boolean {
        return (this.position.col <= pos.col && (this.position.col + piece.width) > pos.col)
                && (this.position.line <= pos.line && (this.position.line + piece.height) > pos.line)
    }

    fun inPiece(pos : Coord) : Boolean {
        var res : Boolean = this.inFrame(pos)
        if (res) {
            res = piece.forEachBlock<Boolean>(false, Boolean::or)
            { c : Coord ->  pos == c + position }
        }
        return res;
    }

    fun match(other : PositionPiece) : Boolean {
        return this.piece.ident == other.piece.ident
                && this.position == other.position
    }
}

enum class Direction(val delta : Coord, val opposite : () -> Direction) {
    UP(Coord(-1, 0), { DOWN }),
    DOWN(Coord(1, 0), { UP }),
    LEFT(Coord(0, -1), { RIGHT }),
    RIGHT(Coord(0, 1), { LEFT });

    fun apply(startPos : Coord) : Coord {
        return this.delta + startPos
    }
}

fun findDirection(start : Coord, to : Coord) : Direction {
    val deltaX = to.col - start.col
    val deltaY = to.line - start.line
    val direction : Direction = if (Math.abs(deltaX) > Math.abs(deltaY)) {
        //horizontal move
        if (deltaX > 0) {
            Direction.RIGHT
        }
        else {
            Direction.LEFT
        }
    }
    else {
        if (deltaY > 0) {
            Direction.DOWN
        }
        else {
            Direction.UP
        }
    }

    return direction
}

data class Move(val indexPiece : Int, val dir : Direction) {
    fun opposite() : Move {
        return Move(indexPiece, dir.opposite())
    }
}

class Board(height : Int,
            width : Int,
            blocks : Array<Array<Boolean>>,
            val indexes : Map<Char, Int>,
            val pieces : Array<PositionPiece?>)
    : Frame(height, width, blocks) {

    fun match(target : Board) : Boolean {
        var match = this.height == target.height
                && this.width == target.width
        var i = 0
        while (match && i < this.pieces.size) {
            val tg = if (i < target.pieces.size)
                target.pieces[i]
                else null
            val actual = this.pieces[i]
            match = tg == null ||
                    (actual != null && tg.match(actual))
            i += 1
        }
        return match
    }

    fun allPossibleMoves() : List<Move> {
        return this.pieces
            .filterNotNull()
            .flatMap {
                p : PositionPiece ->
                Direction.values().map {
                    Move(p.piece.index, it)
                }
            }
            .filter {
                    m : Move -> this.canMove(m)
            }
    }

    fun canMove(m : Move) : Boolean {
        val piecePos :  PositionPiece? = pieces[m.indexPiece]
        if (piecePos == null) {
            return false
        }
        val posDest  : Coord = m.dir.apply(piecePos.position)
        var canMove : Boolean = posDest.inside(height, width)
                && piecePos.piece.forEachBlock(true, Boolean::and)
        { c : Coord ->
            val destCoord : Coord = m.dir.apply(c + piecePos.position)
            var canMoveBlock : Boolean = destCoord.inside(height, width)
            if (canMoveBlock) {
                val block : Block = this.get(destCoord)

                canMoveBlock = block is freeBlok
                        || (block is PieceBlock && block.piece.ident == piecePos.piece.ident)
            }
            canMoveBlock
        }
        return canMove
    }

    fun applyMove(m : Move) : Boolean {
        if (this.canMove(m)) {
            val piece : PositionPiece? = pieces[m.indexPiece]
            if (piece != null) {
                val posDest = m.dir.apply(piece.position)
                piece.position = posDest
                return true
            }
        }
        return false
    }

    fun cancelMove(m : Move) : Boolean {
        return this.applyMove(m.opposite())
    }

    fun copy() : Board {
        return Board(this.height,
            this.width,
            this.blocks,
            this.indexes,
            Array(this.pieces.size) {
                val p = this.pieces[it]
                if (p == null) {
                    null
                }
                else {
                    PositionPiece(p.position, p.piece)
                }
            }
        )
    }

    fun copy(newPos : (Int) -> Coord?) : Board {
        val newPosPieces = Array<PositionPiece?>(this.pieces.size) {
            val piece = this.pieces[it]?.piece
            val coord = newPos(it)
            if (piece == null || coord == null) {
                null
            }
            else {
                PositionPiece(coord, piece)
            }
        }

        return Board(this.height, this.width, this.blocks, this.indexes, newPosPieces)
    }

    fun get(c : Coord) : Block {
        if (this.blocks[c.line][c.col]) {
            val p = this.pieces.filterNotNull().filter {  it.inPiece(c) }.firstOrNull()
            if (p != null) {
                return PieceBlock(p.piece)
            }
            return freeBlok
        }
        return unreachableBloc
    }
}

class Game(val start : Board,
           val target : Board) {
    val moves : Stack<Move> = Stack()

    val current : Board = start.copy()

    fun restart() {
        var move = this.cancelLastMove()
        while (move != null) {
            move = this.cancelLastMove()
        }
    }

    fun isWinner(board: Board) : Boolean {
        return board.match(this.target)
    }

    fun applyMove(m : Move) : Boolean {
        val ok = this.current.applyMove(m)
        if (ok) {
            this.moves.add(m)
        }
        return ok;
    }

    fun cancelLastMove() : Move? {
        if (!this.moves.empty()) {
            val m : Move = this.moves.pop();
            this.current.cancelMove(m)
            return m
        }
        return null
    }
}