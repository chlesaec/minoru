package view

import board.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.stage.Stage
import solver.multi.Solver
import tornadofx.*

class RectDrawer(val size : Int) {
    fun draw(g : GraphicsContext,
             c : Color,
             startPoint : Coord) {
        g.fill = c
        g.fillRect(
            startPoint.col.toDouble(),
            startPoint.line.toDouble(),
            this.size.toDouble(),
            this.size.toDouble())
    }
}

class BoardDrawer(val blockSize : Int,
                  val board : Board,
                  val leftpoint : Coord) {

    private fun color(c : Char) : Color {
        return when (c) {
            '1' -> Color.BLUE
            '2' -> Color.RED
            '3' -> Color.GREEN
            '4' -> Color.YELLOW
            '5' -> Color.PURPLE
            '6' -> Color.CORAL
            '7' -> Color.BROWN
            '8' -> Color.BEIGE
            '9' -> Color.CYAN
            'A' -> Color.GOLD
            'x' -> Color.BISQUE
            'y' -> Color.PINK
            else -> Color.PINK
        }
    }

    fun getBlockCoord(line : Double, col : Double) : Coord? {
        if (line.toInt() < this.leftpoint.line
            || col.toInt() < this.leftpoint.col) {
            return null
        }
        val endCol : Int = this.board.width * this.blockSize + this.leftpoint.col
        val endLine : Int = this.board.height * this.blockSize + this.leftpoint.line
        if (line.toInt() > endLine || col.toInt() > endCol) {
            return null
        }
        val columnBoard : Int = (col.toInt() - leftpoint.col) / this.blockSize
        val lineBoard : Int = (line.toInt() - leftpoint.line) / this.blockSize
        return Coord(lineBoard, columnBoard)
    }

    fun draw(g: GraphicsContext) {
        val d = RectDrawer(blockSize)
        for (i in 0 until board.height) {
            for (j in 0 until board.width) {
                val block = board.get(Coord(i, j))
                val color: Color = when (block) {
                    freeBlok -> {
                        Color.BLACK
                    }
                    unreachableBloc -> {
                        Color.GREY
                    }
                    is PieceBlock -> {
                        this.color(block.piece.ident)
                    }
                }
                val rectCoord = Coord(i*blockSize, j*blockSize)
                d.draw(g, color,
                    rectCoord + this.leftpoint)
            }
        }
    }
}

class PieceDragger(val startX : Double,
                   val startY : Double,
                   val block : PieceBlock,
                   val drawer : BoardDrawer,
                   val game : Game,
                   val canvas : Canvas,
                   val g : GraphicsContext) {

    var lastDirection : Direction? = null

    val dm = EventHandler<MouseEvent>() {
        v : MouseEvent -> this.dragMoved(v)
    }
    val ed : EventHandler<MouseEvent> = EventHandler<MouseEvent>() {
        v : MouseEvent ->  this.endDrag(v)
    }

    init {
        this.canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this.dm)
        this.canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, this.ed)
    }

    private fun dragMoved(evt : MouseEvent) {
        this.posNode(evt.x, evt.y)
    }

    private fun endDrag(evt : MouseEvent) {
        this.posNode(evt.x, evt.y)
        this.canvas.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this.dm)
        this.canvas.removeEventFilter(MouseEvent.MOUSE_RELEASED, this.ed)

        val gr : GraphicsContext? = g
        if (gr != null) {
            if (this.game.isWinner(this.drawer.board)) {
                gr.fillText("Congratulation,\nYou Win", 200.0, 160.0, 200.0)
            }
            else {
                gr.clearRect(200.0, 140.0, 100.0, 50.0)
            }
        }
    }

    private fun getBoard() : Board = this.drawer.board

    private fun posNode(x : Double, y : Double) {
        val directToCancel = this.lastDirection
        val direction = findDirection(Coord(this.startY, this.startX), Coord(y, x))
        if (directToCancel != null && directToCancel != direction) {
            this.getBoard().cancelMove(Move(this.block.piece.index, directToCancel))
            this.drawer.draw(g)
        }

        if (direction == null) {
            this.lastDirection = null;
        }
        else if (directToCancel != direction &&
            this.getBoard().canMove(Move(block.piece.index, direction))) {
            if (this.game.applyMove(Move(this.block.piece.index, direction))) {
                this.drawer.draw(g)
                this.lastDirection = direction
            }
        }
    }
}

class SolutionWindow(val game : Game) : Fragment() {

    var current : Board = game.start.copy()

    val drawer = BoardDrawer(
        18,
        this.current,
        Coord(40.0, 10.0)
    )

    val moves : List<Move>?

    var currentMove : Int = 0

    var g : GraphicsContext? = null

    init {
        this.moves = Solver(this.game).run()
    }

    fun go(direction : Int) {
        if (moves == null) {
            return;
        }
        if (direction > 0 && this.currentMove < this.moves.size) {
            val m = this.moves[this.currentMove]
            if (m != null) {
                this.current.applyMove(m)
                this.currentMove++
            }
        }
        if (direction < 0 && this.currentMove > 0) {
            val m = this.moves[this.currentMove]
            if (m != null) {
                this.current.cancelMove(m)
                this.currentMove--
            }
        }
    }

    fun drawCurrent() {
        val currentG = this.g
        if (currentG != null) {
            drawer.draw(currentG)
        }
    }

    override val root = borderpane {
        center = canvas {
            this.width = 500.0
            this.height = 400.0
            this.minWidth(500.0)
            this.minHeight(400.0)
            this.maxWidth(500.0)
            this.maxHeight(500.0)
            this.graphicsContext2D.clearRect(0.0, 0.0, this.width, this.height)
            this@SolutionWindow.g = this.graphicsContext2D
            this@SolutionWindow.drawCurrent()
        }
        bottom = hbox {
            button("<<") {
                action {
                    this@SolutionWindow.go(-1)
                    this@SolutionWindow.drawCurrent()
                }
            }
            button(">>") {
                action {
                    this@SolutionWindow.go(1)
                    this@SolutionWindow.drawCurrent()
                }
            }
        }
    }
}

class MyView() : View() {

    private var game : Game

    private var workBoard : BoardDrawer

    var gc : GraphicsContext? = null

    init {
        this.game = PieceData().game("config/2heart.dat")

        this.workBoard = BoardDrawer(30,
            this.game.current,
            Coord(200,100));
    }

    private fun load(file : String) {
        this.game = PieceData().game(file)

        this.workBoard = BoardDrawer(30,
            this.game.current,
            Coord(200,100));
        this.draw()
    }


    val event =
        EventHandler<ActionEvent> { action : ActionEvent ->
            val source = action.source
            if (source is MenuItem) {
                val fic = when (source.text) {
                    "heart" -> "config/2heart.dat"
                    //"to 10" -> "config/1to10.dat"
                    "domino" -> "config/4domino.dat"
                    "puzz" -> "config/9puzz.dat"
                    "block" -> "config/10block.dat"
                    "123" -> "config/123.dat"
                    else -> ""
                }
                if (fic.isNotEmpty()) {
                    this@MyView.load(fic)
                }
            }

        }
    override val root = borderpane {
        top = hbox {
            menubar {
                menu("Load") {
                    item("heart").onAction = this@MyView.event
                    //item("to 10").onAction = this@MyView.event
                    item("domino").onAction = this@MyView.event
                    item("puzz").onAction = this@MyView.event
                    item("block").onAction = this@MyView.event
                    item("123").onAction = this@MyView.event
                }
            }
        }
        center = canvas {
            this.width = 900.0
            this.height = 600.0
            this.minWidth(900.0)
            this.minHeight(600.0)
            this.graphicsContext2D.clearRect(0.0, 0.0, this.width, this.height)
            this@MyView.gc = this.graphicsContext2D
            this@MyView.draw()
            this.addEventFilter(MouseEvent.MOUSE_PRESSED,
                this@MyView::startDrag)
        }

        right = vbox {
            button("back") {
                action {
                    this@MyView.game.cancelLastMove()
                    val gc = this@MyView.gc
                    if (gc != null) {
                        this@MyView.workBoard.draw(gc)
                    }
                }
            }
            button("reset") {
                action {
                    this@MyView.game.restart()
                    val gc = this@MyView.gc
                    if (gc != null) {
                        this@MyView.workBoard.draw(gc)
                    }
                }
            }
            button("show") {
                action {
                    SolutionWindow(this@MyView.game)
                        .openWindow(owner = null)
                }
            }
        }
    }


    fun draw() {
        val g : GraphicsContext? = this.gc
        if (g != null) {
            g.clearRect(0.0, 0.0, 900.0, 600.0)
            val drawerStart = BoardDrawer(
                13,
                this.game.start,
                Coord(10, 10)
            );
            drawerStart.draw(g)
            val drawerEnd = BoardDrawer(
                13,
                this.game.target,
                Coord(10, 380)
            );
            drawerEnd.draw(g)
            workBoard.draw(g)
        }
    }

    private fun startDrag(evt: MouseEvent) {
        val blockCoord : Coord? = this.workBoard.getBlockCoord(evt.y, evt.x)
        if (blockCoord != null) {

            val block : Block = this.game.current.get(blockCoord)
            if (block is PieceBlock) {
                val center = root.center
                val g = this.gc
                if (center is Canvas && g != null) {
                    PieceDragger(evt.x,
                        evt.y,
                        block,
                        this.workBoard,
                        this.game,
                        center,
                        g)
                }
            }
        }
    }

}

class MyApp() : App(MyView::class) {

    override fun start(stage: Stage) {
        with(stage) {
            minWidth = 900.0
            minHeight = 600.0
            super.start(this)
        }
    }
}

fun main(args: Array<String>) {
    launch<MyApp>(args)
}