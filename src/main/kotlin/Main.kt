import kotlin.math.max
import kotlin.math.min

/**
 * Tic Tac Toe using minimax
 * Author: Tibor Jevicki, 13. Nov 2022.
 */
fun main() {
    val game = Game(
        TicTacToeWinResolver(),
        MinimaxAiPlayer(TicTacToeWinResolver(), Player.X)
    )

    game.play()
}

class Game(
    private val winResolver: TicTacToeWinResolver,
    private val aiPlayer: MinimaxAiPlayer
) {
    private val board = Board()
    private var activePlayer = Player.X

    fun play() {
        board.print()

        while (winResolver.resolveWinner(board) is TicTacToeWinResolver.Outcome.GameNotOver) {
            try {
                println("Player ${activePlayer}'s turn.")
                playMove(aiPlayer.calculateNextMove(board))
                activePlayer = activePlayer.otherPlayer()

                if (winResolver.resolveWinner(board) is TicTacToeWinResolver.Outcome.GameNotOver) {
                    println("Player ${activePlayer}'s turn.")
                    print("Enter move -> ")
                    val moveInput = readLine()
                    playMove(Move(activePlayer, parseInputIntoTileIndex(moveInput ?: "")))
                    activePlayer = activePlayer.otherPlayer()
                }
            } catch (e: IllegalArgumentException) {
                println("Wrong input.")
                continue
            }
        }

        print(winResolver.resolveWinner(board).forDisplay())
    }

    private fun TicTacToeWinResolver.Outcome.forDisplay(): String {
        return when (this) {
            is TicTacToeWinResolver.Outcome.Winner -> "$player wins."
            is TicTacToeWinResolver.Outcome.Tie -> "Game finished with a tie."
            else -> "Game still in progress."
        }
    }

    private fun parseInputIntoTileIndex(input: String): Int {
        val inputError = IllegalArgumentException("Wrong input")

        if (input.length != 2) throw inputError
        return when (input[0]) {
            'a' -> {
                when (input[1]) {
                    '1' -> 0
                    '2' -> 3
                    '3' -> 6
                    else -> throw inputError
                }
            }

            'b' -> {
                when (input[1]) {
                    '1' -> 1
                    '2' -> 4
                    '3' -> 7
                    else -> throw inputError
                }
            }

            'c' -> {
                when (input[1]) {
                    '1' -> 2
                    '2' -> 5
                    '3' -> 8
                    else -> throw inputError
                }
            }

            else -> throw inputError
        }
    }

    private fun playMove(move: Move) {
        println("Player ${move.player} makes the move on ${move.displayCoords()}: ")
        println()

        board.playMove(move)
        board.print()
    }
}

class Board(initialState: List<TileState>? = null) {
    private val tiles: MutableList<TileState> =
        initialState?.toMutableList()
            ?: mutableListOf<TileState>()
                .apply {
                    repeat(9) {
                        this.add(TileState(null))
                    }
                }

    val state: List<TileState>
        get() = tiles.toList()

    fun playMove(move: Move) {
        tiles.set(move.tileIndex, TileState(move.player))
    }

    fun print() {
        println("   a   b   c")
        println()

        for (row in 0..2) {
            printRow(row, row < 2)
        }

        println()
        println()
    }

    fun copy(): Board = Board(state)

    private fun printRow(row: Int, hasBottomDivider: Boolean) {
        print("${row + 1} ")

        for (i in 0..2) {
            val actualIndex = i + 3 * row

            print(" ${tiles[actualIndex].displayValue()} ")
            if (i<2) print("|")
        }
        println()

        if (hasBottomDivider) println("  --- --- ---")
    }

    fun TileState.displayValue() = when (this.occupiedByPlayer) {
        Player.X -> "X"
        Player.O -> "0"
        null -> " "
    }
}

class MinimaxAiPlayer(
    private val gameOutcomeResolver: TicTacToeWinResolver,
    val player: Player
) {
    fun calculateNextMove(board: Board): Move {
        // Pair of move and its minimax value
        var bestMove: Pair<Move, Int> = Move(player, 0) to MAXIMIZER_LOSE_VALUE

        board.state
            .forEachIndexed { index, tile ->
                if (!tile.isOccupied) {
                    val possibleBoard = board.copy()
                    possibleBoard.playMove(Move(player, index))

                    val minimax = minimax(possibleBoard, isMaximizing = false)

                    val newMax = Math.max(bestMove.second, minimax)
                    if (newMax > bestMove.second)
                        bestMove = Move(player, index) to newMax
                }
            }

        return bestMove.first
    }

    private fun minimax(boardPossibility: Board, isMaximizing: Boolean): Int {
        val outcome = gameOutcomeResolver.resolveWinner(boardPossibility)

        if (outcome is TicTacToeWinResolver.Outcome.Winner && outcome.player == player) return MAXIMIZER_WIN_VALUE
        else if (outcome is TicTacToeWinResolver.Outcome.Winner && outcome.player != player) return MAXIMIZER_LOSE_VALUE
        else if (outcome is TicTacToeWinResolver.Outcome.Tie) return MAXIMIZER_NEUTRAL_VALUE

        // Go further by evaluating all unoccupied tiles in this board possibility
        // If maximizing, find maximum, if minimizing, find minimum
        val hypotheticalPlayer = if (isMaximizing) player else player.otherPlayer()

        var minimax = if (isMaximizing) -1 else 1
        boardPossibility.state
            .forEachIndexed { index, tile ->
                if (!tile.isOccupied) {
                    val possibleBoard = boardPossibility.copy()
                    possibleBoard.playMove(Move(hypotheticalPlayer, index))

                    // Check if this value is the best
                    // If it is, set this return this tile as the next move
                    val nextMinimax = minimax(possibleBoard, isMaximizing = !isMaximizing)

                    minimax =
                        if (isMaximizing) max(minimax, nextMinimax)
                        else min(minimax, nextMinimax)
                }
            }

        return minimax
    }

    companion object {
        private const val MAXIMIZER_WIN_VALUE = 1
        private const val MAXIMIZER_LOSE_VALUE = -1
        private const val MAXIMIZER_NEUTRAL_VALUE = 0
    }
}

class TicTacToeWinResolver() {
    fun resolveWinner(board: Board): Outcome {
        val boardState = board.state

        // Check horizontal wins
        boardState[0].occupiedByPlayer?.let { player ->
            if (boardState[0] == boardState[1] && boardState[0] == boardState[2])
                return Outcome.Winner(player)
        }

        boardState[3].occupiedByPlayer?.let { player ->
            if (boardState[3] == boardState[4] && boardState[3] == boardState[5])
                return Outcome.Winner(player)
        }

        boardState[6].occupiedByPlayer?.let { player ->
            if (boardState[6] == boardState[7] && boardState[6] == boardState[8])
                return Outcome.Winner(player)
        }

        // Check vertical wins
        boardState[0].occupiedByPlayer?.let { player ->
            if (boardState[0] == boardState[3] && boardState[0] == boardState[6])
                return Outcome.Winner(player)
        }

        boardState[1].occupiedByPlayer?.let { player ->
            if (boardState[1] == boardState[4] && boardState[1] == boardState[7])
                return Outcome.Winner(player)
        }

        boardState[2].occupiedByPlayer?.let { player ->
            if (boardState[2] == boardState[5] && boardState[2] == boardState[8])
                return Outcome.Winner(player)
        }

        // Check diagonal
        boardState[0].occupiedByPlayer?.let { player ->
            if (boardState[0] == boardState[4] && boardState[0] == boardState[8])
                return Outcome.Winner(player)
        }

        boardState[2].occupiedByPlayer?.let { player ->
            if (boardState[2] == boardState[4] && boardState[2] == boardState[6])
                return Outcome.Winner(player)
        }

        // Check free tiles
        boardState.forEach { tile ->
            if (tile.occupiedByPlayer == null) return Outcome.GameNotOver
        }

        return Outcome.Tie
    }

    sealed class Outcome {
        data class Winner(val player: Player) : Outcome()
        object Tie : Outcome()
        object GameNotOver : Outcome()
    }
}

data class Move(val player: Player, val tileIndex: Int) {
    fun displayCoords(): String {
        val x = when (tileIndex) {
            0, 3, 6 -> "a"
            1, 4, 7 -> "b"
            2, 5, 8 -> "c"
            else -> ""
        }

        val y = when (tileIndex) {
            0, 1, 2 -> "1"
            3, 4, 5 -> "2"
            6, 7, 8 -> "3"
            else -> ""
        }

        return "$x$y"
    }
}

data class TileState(val occupiedByPlayer: Player?) {
    val isOccupied = occupiedByPlayer != null
}

enum class Player {
    X, O;

    fun otherPlayer(): Player = when (this) {
        X -> O
        O -> X
    }
}


