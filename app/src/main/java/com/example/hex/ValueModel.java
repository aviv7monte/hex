package com.example.hex;

/** Gives one position value from the point of view of the player to move. */
public interface ValueModel {
    /** Evaluates one board encoding with the fixed shape 7x7x3. */
    float evaluate(float[] encodedBoard);
}