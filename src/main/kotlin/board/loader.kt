package board

class PieceBuilder(val ident : Char, val index : Int) {

    val pos = ArrayList<Coord>()

    fun add(line : Int, col : Int) {
        this.pos.add(Coord(line, col));
    }

    private fun origin() : Coord {
        return Coord(
            this.pos.map{ c : Coord -> c.line }.min() ?: 0,
            this.pos.map{ c : Coord -> c.col }.min() ?: 0
        )
    }

    fun build() : PositionPiece {
        val origin : Coord = this.origin()

        val size = Coord(
            (this.pos.map{ c : Coord -> c.line - origin.line}.max() ?: 0) + 1,
            (this.pos.map{ c : Coord -> c.col - origin.col}.max() ?: 0) + 1
        )
        val block = Array(size.line)
            { Array(size.col){ false} }

        for (p in this.pos) {
            block[p.line - origin.line][p.col - origin.col] = true
        }

        val piece = Piece(this.ident, this.index, size.line, size.col, block)
        return PositionPiece(origin, piece)
    }
}

open class BoardBuilder(val height : Int,
                        val width : Int,
                        val indexes : HashMap<Char, Int> = HashMap<Char, Int>()) {

    val block = Array(this.height)
        { Array(this.width) { false} }

    val pieces = arrayListOf<PieceBuilder?>()

    fun integrate(line : Int, col : Int, c : Char) {
        if (c == '.') {
            this.block[line][col] = true
        }
        else if (c != '#') {
            this.block[line][col] = true
            indexes.compute(c) {
                    k: Char, v: Int? ->
                if (v == null) {
                    val index = this.pieces.size
                    this.pieces.add(PieceBuilder(c, index))
                    pieces[index]?.add(line, col)
                    index
                }
                else {
                    while (v >= this.pieces.size) {
                        this.pieces.add(null)
                    }
                    if (pieces[v] == null) {
                        pieces[v] = PieceBuilder(c, v)
                    }
                    pieces[v]?.add(line, col)
                    v
                }
            }
        }
    }

    fun build() : Board {
        return Board(this.height,
              this.width,
              this.block,
              this.indexes,
              this.pieces.map { it?.build() }.toTypedArray())
    }
}

class PieceData {
    var startBordBuilder : BoardBuilder? = null
    var endBoardBuilder : BoardBuilder? = null

    var inInitial = false
    var inTarget = false

    var lineNumber = 0

    fun addLine(line : String, indexes : Map<Char, Int>?) {
        if (line.startsWith("size")) {
            val posBeforeHeigth = line.lastIndexOf(' ')
            val heigth = line.substring(posBeforeHeigth).trim().toInt()
            val width = line.substring(5, posBeforeHeigth).trim().toInt()
            this.startBordBuilder = BoardBuilder(heigth, width)
        }
        else if ("initial" == line) {
            this.inInitial = true
            this.inTarget = false
            lineNumber = 0
        }
        else if ("target" == line) {
            this.inInitial = false
            this.inTarget = true
            lineNumber = 0
            this.endBoardBuilder = BoardBuilder(this.startBordBuilder!!.height,
                this.startBordBuilder!!.width,
                this.startBordBuilder!!.indexes)
        }
        else if (this.inInitial) {
            if (line == ";") {
                this.inInitial = false
            }
            else {
                val bb = this.startBordBuilder
                if (bb != null) {
                    this.lineBordDesc(line, lineNumber, bb)
                    this.lineNumber++
                }
            }
        }
        else if (this.inTarget) {
            if (line == ";") {
                this.inTarget = false
            }
            else {
                val bb = this.endBoardBuilder
                if (bb != null) {
                    this.lineBordDesc(line, lineNumber, bb)
                    this.lineNumber++
                }
            }
        }
    }

    private fun lineBordDesc(line : String,
                             lineNumber: Int,
                             builder : BoardBuilder) {
        var col = 0

        for (c : Char in line) {
            builder.integrate(lineNumber, col, c)
            col++
        }
    }

    fun game(fileName : String) : Game {
        val resource = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        resource.use { resource ->
            resource.bufferedReader().lines().forEach {
                this.addLine(it, this.startBordBuilder?.indexes)
            }

            val start: Board = this.startBordBuilder!!.build()
            val end: Board = this.endBoardBuilder!!.build()

            return Game(start, end)
        }
    }
}
