package com.example.hex;

/** Chooses a move by evaluating every legal one-move successor. */
public final class HexAi {
    private final ValueModel model;

    /** Creates a chooser that evaluates each candidate position with the supplied model. */
    public HexAi(ValueModel model) {
        this.model = model;
    }

    /**
     * Compares copied positions so the visible game remains unchanged.
     *
     * @param position position to evaluate without modifying it
     * @return the best legal move, or null if no legal move remains
     */
    public HexGame.Move chooseMove(HexGame position) {
        HexGame.Move bestMove = null;
        float bestValue = Float.NEGATIVE_INFINITY;

        for (HexGame.Move move : position.legalMoves()) {
            // Play this candidate on its own copy of the current position.
            HexGame successor = position.copy();
            successor.play(move.row, move.column);

            // A move that connects the sides wins immediately.
            if (successor.isOver()) return move;

            // The model sees the next player, so reverse its score for this player.
            float value = -model.evaluate(successor.encodeForCurrentPlayer());
            if (value > bestValue) {
                bestMove = move;
                bestValue = value;
            }
        }
        return bestMove;
    }
}