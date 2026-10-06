package com.example.hex;


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



    private final int size;

    private final int[] cells;
    private int currentPlayer;

    /** Creates the default 7×7 board. */
    public HexGame() {
        this(DEFAULT_SIZE);
    }

    /** Creates a square board with the given number of rows and columns. */
    public HexGame(int size) {


        this.size = size;
        cells = new int[size * size];
        currentPlayer = RED;}

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
    if (isOutside(row, column)) {
        return false;
    }
    int index = index(row, column);
    if (cells[index] != EMPTY) {
        return false;
    }
    cells[index] = currentPlayer;
    currentPlayer = otherPlayer(currentPlayer);
    return true;
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
