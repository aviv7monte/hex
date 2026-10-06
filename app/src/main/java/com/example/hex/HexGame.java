package com.example.hex;

import java.util.ArrayDeque;

import java.util.Arrays;

/**
 * Holds the board dimensions, position, and rules for a Hex game.
 *
 * <p>Chapter 1 uses the size; following chapters add the board position and rules.
 */
public final class HexGame {
    /** Default board size for the Hex series. */
    public static final int DEFAULT_SIZE = 7;

    /** Cell value used for an unoccupied cell. */
    public static final int EMPTY = 0;
    /** Player value for Red, whose goal is to connect top to bottom. */
    public static final int RED = 1;
    /** Player value for Blue, whose goal is to connect left to right. */
    public static final int BLUE = 2;


    private static final int[][] NEIGHBORS = {
            {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}
    };

    private final int size;

    private final int[] cells;
    private int currentPlayer;

    private int winner = EMPTY;

    /** Creates the default 7×7 board. */
    public HexGame() {
        this(DEFAULT_SIZE);
    }

    /** Creates a square board with the given number of rows and columns. */
    public HexGame(int size) {

        this.size = size;
        cells = new int[size * size];
        currentPlayer = RED;
    }

    /** @return the number of rows and columns on this board */
    public int getSize() {
        return size;
    }


/**
 * Places the current player's stone and advances the turn when the move is legal.
 *
 * <p>TWIN-ID: HEX.APPLY_MOVE
 *
 * @param row zero-based board row
 * @param column zero-based board column
 * @return {@code true} if the move was played; {@code false} if the rules reject it,
 *         leaving the board and turn unchanged
 */
public boolean play(int row, int column) {
    if (isOutside(row, column)  || winner != EMPTY) {
        return false;
    }
    int index = index(row, column);
    if (cells[index] != EMPTY) {
        return false;
    }

    cells[index] = currentPlayer;

    if (hasConnection(currentPlayer)) {
                    winner = currentPlayer;
               }
            // Advance the turn even after the winning move.
    currentPlayer = otherPlayer(currentPlayer);
    return true;
}

    /**
     * Detects a win by searching the player's connected stones between both goal edges.
     * <p>TWIN-ID: HEX.WIN_CHECK
     *
     * This check reads the board without placing a stone or changing the game state.
     *
     * @param player {@link #RED} (top to bottom) or {@link #BLUE} (left to right)
     * @return {@code true} if the player's stones connect their two goal edges
     */
    public boolean hasConnection(int player) {

        // Mark cells when queued so each stone is examined at most once.
        boolean[] visited = new boolean[cells.length];
        // Breadth-first search: cells waiting to be examined.
        ArrayDeque<Integer> frontier = new ArrayDeque<>();
        // Seed every stone on the player's starting edge.
        for (int i = 0; i < size; i++) {
            // Red starts on row 0; Blue starts on column 0.
            int row = player == RED ? 0 : i;
            int column = player == RED ? i : 0;
            int start = index(row, column);
            // Empty and opposing cells cannot begin this player's path.
            if (cells[start] == player) {
                visited[start] = true;
                frontier.add(start);
            }
        }

        // Expand the connected region until it reaches the goal or runs out.
        while (!frontier.isEmpty()) {
            int position = frontier.removeFirst();
            // Convert the one-dimensional cell index back to board coordinates.
            int row = position / size;
            int column = position % size;
            // The opposite edge completes Red's vertical or Blue's horizontal path.
            if ((player == RED && row == size - 1)
                    || (player == BLUE && column == size - 1)) {
                return true;
            }
            // Follow only the six neighboring cells on the Hex grid.
            for (int[] offset : NEIGHBORS) {
                int nextRow = row + offset[0];
                int nextColumn = column + offset[1];
                // Ignore coordinates outside the board before computing an index.
                if (isOutside(nextRow, nextColumn)) {
                    continue;
                }
                int next = index(nextRow, nextColumn);
                // An unvisited stone of this color extends the connected path.
                if (!visited[next] && cells[next] == player) {
                    visited[next] = true;
                    frontier.addLast(next);
                }
            }
        }
        // Every reachable stone was checked without finding the goal edge.
        return false;
    }

    /** @return the winning player, or {@link #EMPTY} while no player has won */
    public int getWinner() {
        return winner;
    }

    /** @return {@code true} after either player has completed a connection */
    public boolean isOver() {
        return winner != EMPTY;
    }


/**
 * Returns the value stored at one board coordinate.
 *
 * @param row zero-based board row
 * @param column zero-based board column
 * @return {@link #EMPTY}, {@link #RED}, or {@link #BLUE}
 * @throws IndexOutOfBoundsException if the coordinate is outside the board
 */
public int getCell(int row, int column) {
    if (isOutside(row, column)) {
        throw new IndexOutOfBoundsException("Cell is outside the board");
    }
    return cells[index(row, column)];
}

/**
 * Copies all board cells in row-major order.
 *
 * @return an independent row-major array of board cells
 */
public int[] getCells() {
    return Arrays.copyOf(cells, cells.length);
}

/** @return the player that will make the next move */
public int getCurrentPlayer() {
    return currentPlayer;
}

/** RED is 1 and BLUE is 2, so subtracting either from 3 gives the opponent. */
public static int otherPlayer(int player) {
    return 3 - player;
}

private int index(int row, int column) {
    return row * size + column;
}

private boolean isOutside(int row, int column) {
    return row < 0 || row >= size || column < 0 || column >= size;
}
}
