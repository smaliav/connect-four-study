package connectfour

import java.lang.StringBuilder
import java.util.*
import kotlin.math.min

const val GAME_NAME = "Connect Four"
const val LINE_TARGET = 4
const val MIN_ROWS = 5
const val MIN_COLUMNS = 5
const val MAX_ROWS = 9
const val MAX_COLUMNS = 9
const val BOARD_SIZE_INPUT_REGEX = """^\s*\d+\s*[xX]\s*\d+\s*$"""
const val FIRST_PLAYER_NAME_PROMPT = "First player's name:"
const val SECOND_PLAYER_NAME_PROMPT = "Second player's name:"
const val BOARD_DIMENSIONS_PROMPT = """Set the board dimensions (Rows x Columns)
Press Enter for default (%d x %d)"""
const val INVALID_INPUT = "Invalid input"
const val INVALID_ROWS = "Board rows should be from $MIN_ROWS to $MAX_ROWS"
const val INVALID_COLUMNS = "Board columns should be from $MIN_COLUMNS to $MAX_COLUMNS"
const val INCORRECT_COLUMN = "Incorrect column number"
const val VERT_LINE = "║"
const val LEFT_BOTTOM_CORNER = "╚"
const val HOR_LINE = "═"
const val BOTTOM_T_SHAPE = "╩"
const val RIGHT_BOTTOM_CORNER = "╝"
const val END_GAME_COMMAND = "end"
const val END_GAME = "Game over!"
const val PLAYER1_SIGN = "o"
const val PLAYER2_SIGN = "*"
const val DRAW = "It is a draw"
const val GAME_TYPE_PROMPT = """Do you want to play single or multiple games?
For a single game, input 1 or press Enter
Input a number of games:"""

class Game(
    private var player1: Player,
    private var player2: Player,
    rows: Int,
    cols: Int,
    private val scanner: Scanner,
) {
    companion object {
        const val DEFAULT_GAMES_COUNT = 1
    }

    private var isOn = true
    private val board = Board(rows, cols)
    private var curPlayer = player1

    fun start(alternate: Boolean): GameRes {
        var res = GameRes.QUIT

        if (alternate) curPlayer = player2

        println(board.toString())

        while (isOn) {
            do {
                println("$curPlayer's turn:")
                val input = scanner.next()
                val isValid = validateColumnInput(input)

                if (isValid) {
                    val col = input.toInt() - 1

                    if (col in 0 until board.cols) {
                        val successfulTurn = board.makeTurn(col, curPlayer.turnType)

                        if (successfulTurn) {
                            println(board.toString())

                            when (board.checkState()) {
                                Board.BoardState.WIN -> {
                                    println("Player $curPlayer won")
                                    res = when (curPlayer) {
                                        player1 -> GameRes.PLAYER1_WON
                                        else -> GameRes.PLAYER2_WON
                                    }
                                    isOn = false
                                }
                                Board.BoardState.DRAW -> {
                                    println(DRAW)
                                    res = GameRes.DRAW
                                    isOn = false
                                }
                                Board.BoardState.TURN_MADE -> {
                                    curPlayer = if (curPlayer == player1) player2 else player1
                                }
                            }
                        } else {
                            println("Column ${col + 1} is full")
                        }
                    } else {
                        println("The column number is out of range (1 - ${board.cols})")
                    }
                } else if (input == END_GAME_COMMAND) {
                    res = GameRes.QUIT
                    isOn = false
                } else {
                    println(INCORRECT_COLUMN)
                }

            } while (!isValid && isOn)
        }

        return res
    }

    private fun validateColumnInput(input: String) = input.matches("""\d+""".toRegex())
}

class Turn(val row: Int = -1, val col: Int = -1, val type: TurnType = TurnType.PLAYER1)

enum class TurnType(val sign: String) {
    PLAYER1(PLAYER1_SIGN), PLAYER2(PLAYER2_SIGN)
}

class Board(val rows: Int, val cols: Int) {

    private var lastTurn = Turn()
    private val field = MutableList(rows) {
        MutableList(cols) { EMPTY_CELL }
    }

    companion object {
        const val EMPTY_CELL = " "
        const val DEFAULT_ROWS = 6
        const val DEFAULT_COLS = 7
    }

    enum class BoardState {
        WIN, DRAW, TURN_MADE
    }

    fun makeTurn(col: Int, turnType: TurnType): Boolean {
        if (field[0][col] != EMPTY_CELL) return false

        var targetRow = field.lastIndex
        while (field[targetRow][col] != EMPTY_CELL) targetRow--

        field[targetRow][col] = when (turnType) {
            TurnType.PLAYER1 -> PLAYER1_SIGN
            TurnType.PLAYER2 -> PLAYER2_SIGN
        }

        lastTurn = Turn(targetRow, col, turnType)
        return true
    }

    fun checkState(): BoardState {
        val isWinner = checkWinner()
        val isDraw = checkDraw()

        return when {
            isWinner -> BoardState.WIN
            isDraw -> BoardState.DRAW
            else -> BoardState.TURN_MADE
        }
    }

    private fun checkWinner(): Boolean {
        var line = ""

        // Check vertical, |
        for (row in 0 until rows) {
            line += field[row][lastTurn.col]
        }
        if (checkWiningLine(line)) return true

        // Check horizontal, -
        line = field[lastTurn.row].joinToString("")
        if (checkWiningLine(line)) return true

        // Check diagonal, \
        line = ""
        var cellsUp = min(lastTurn.row, lastTurn.col)
        var cellsDown = min(cols - 1 - lastTurn.col, rows - 1 - lastTurn.row)
        var maxIters = cellsUp + cellsDown
        for (i in 0..maxIters) {
            line += field[lastTurn.row - cellsUp + i][lastTurn.col - cellsUp + i]
        }
        if (checkWiningLine(line)) return true

        // Check diagonal, /
        line = ""
        cellsDown = min(lastTurn.col, rows - 1 - lastTurn.row)
        cellsUp = min(lastTurn.row, cols - 1 - lastTurn.col)
        maxIters = cellsUp + cellsDown
        for (i in 0..maxIters) {
            line += field[lastTurn.row + cellsDown - i][lastTurn.col - cellsDown + i]
        }
        if (checkWiningLine(line)) return true

        return false
    }

    private fun checkWiningLine(line: String) = line.indexOf(lastTurn.type.sign.repeat(LINE_TARGET)) != -1

    private fun checkDraw() = field.first().count { it == EMPTY_CELL } == 0

    override fun toString(): String {
        val res = StringBuilder()

        // Header
        res.append((1..cols).joinToString(" ", " ", " ")).append("\n")
        // Vertical lines
        for (i in 0 until rows) {
            res.append(field[i].joinToString(VERT_LINE, VERT_LINE, VERT_LINE + "\n"))
        }
        // Footer
        res.append(HOR_LINE.repeat(cols).toList()
            .joinToString(BOTTOM_T_SHAPE, LEFT_BOTTOM_CORNER, RIGHT_BOTTOM_CORNER))

        return res.toString()
    }

}

class Player(val name: String, val turnType: TurnType) {
    override fun toString(): String {
        return name
    }
}

enum class GameRes {
    PLAYER1_WON, PLAYER2_WON, DRAW, QUIT
}

class ScoreBoard(private val player1: Player, private val player2: Player) {
    private val scores = mutableMapOf(player1 to 0, player2 to 0)
    private val history = mutableListOf<Game>()

    companion object {
        const val WIN_POINTS = 2
        const val DRAW_POINTS = 1
    }

    fun registerGame(game: Game): Int {
        history.add(game)
        return history.size
    }

    fun updateScores(gameRes: GameRes) {
        when (gameRes) {
            GameRes.PLAYER1_WON -> scores[player1] = scores.getOrDefault(player1, 0) + WIN_POINTS
            GameRes.PLAYER2_WON -> scores[player2] = scores.getOrDefault(player2, 0) + WIN_POINTS
            GameRes.DRAW -> {
                scores[player1] = scores.getOrDefault(player1, 0) + DRAW_POINTS
                scores[player2] = scores.getOrDefault(player2, 0) + DRAW_POINTS
            }
            GameRes.QUIT -> return
        }
    }

    fun printScores() {
        val res = StringBuilder()
        res.append("Score").append('\n')
        res.append("$player1: ${scores[player1]} $player2: ${scores[player2]}")
        println(res)
    }
}

fun readPlayers(scanner: Scanner): Pair<Player, Player> {
    println(FIRST_PLAYER_NAME_PROMPT)
    val player1 = scanner.nextLine()

    println(SECOND_PLAYER_NAME_PROMPT)
    val player2 = scanner.nextLine()

    return Pair(Player(player1, TurnType.PLAYER1), Player(player2, TurnType.PLAYER2))
}

fun readSize(scanner: Scanner): Pair<Int, Int> {
    var rows = Board.DEFAULT_ROWS
    var cols = Board.DEFAULT_COLS

    do {
        println(BOARD_DIMENSIONS_PROMPT.format(Board.DEFAULT_ROWS, Board.DEFAULT_COLS))
        val input = scanner.nextLine()
        var isValid = input.matches(BOARD_SIZE_INPUT_REGEX.toRegex()) || input.isEmpty()

        if (!isValid) {
            println(INVALID_INPUT)
        } else if (input.isNotEmpty()) {
            val (r, c) = """\d+""".toRegex().findAll(input).map { it.value.toInt() }.toList()

            if (r !in MIN_ROWS..MAX_ROWS) {
                println(INVALID_ROWS)
                isValid = false
            } else if (c !in MIN_COLUMNS..MAX_COLUMNS) {
                println(INVALID_COLUMNS)
                isValid = false
            } else {
                rows = r
                cols = c
            }
        }
    } while (!isValid)

    return Pair(rows, cols)
}

fun readGamesCount(scanner: Scanner): Int {
    var count = Game.DEFAULT_GAMES_COUNT

    do {
        println(GAME_TYPE_PROMPT)
        val input = scanner.nextLine()
        var isValid = input.matches("""\d+""".toRegex()) || input.isEmpty()

        if (!isValid) {
            println(INVALID_INPUT)
        } else if (input.isNotEmpty()) {
            val cnt = """\d+""".toRegex().findAll(input).map { it.value.toInt() }.first()

            if (cnt <= 0) {
                println(INVALID_INPUT)
                isValid = false
            } else {
                count = cnt
            }
        }
    } while (!isValid)

    return count
}

fun main() {
    println(GAME_NAME)

    val scanner = Scanner(System.`in`)
    val (player1, player2) = readPlayers(scanner)
    val (rows, cols) = readSize(scanner)
    val gamesCount = readGamesCount(scanner)

    val scoreBoard = ScoreBoard(player1, player2)

    println("$player1 VS $player2")
    println("$rows X $cols board")
    if (gamesCount > 1) {
        println("Total $gamesCount games")
    } else {
        println("Single Game")
    }

    do {
        val game = Game(player1, player2, rows, cols, scanner)
        val gameNum = scoreBoard.registerGame(game)

        if (gamesCount > 1) println("Game #$gameNum")
        val alternate = gameNum % 2 == 0
        val gameRes = game.start(alternate)

        if (gamesCount > 1 && gameRes != GameRes.QUIT) {
            scoreBoard.updateScores(gameRes)
            scoreBoard.printScores()
        }
    } while (gameRes != GameRes.QUIT && gameNum < gamesCount)

    println(END_GAME)
}
