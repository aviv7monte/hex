package com.example.hex;

/**
 * Holds the board dimensions, position, and rules for a Hex game.
 *
 * <p>Chapter 1 uses the size; following chapters add the board position and rules.
 */
public final class HexGame {
    /** Default board size for the Hex series. */
    public static final int DEFAULT_SIZE = 7;

    private final int size;

    /** Creates the default 7×7 board. */
    public HexGame() {
        this(DEFAULT_SIZE);
    }

    /** Creates a square board with the given number of rows and columns. */
    public HexGame(int size) {
        this.size = size;
    }

    /** @return the number of rows and columns on this board */
    public int getSize() {
        return size;
    }}

