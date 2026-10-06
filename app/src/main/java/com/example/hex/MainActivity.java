package com.example.hex;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hex.databinding.ActivityMainBinding;

/** Connects board taps to the independent game state. */

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private HexGame game;

    /** Creates the game screen and connects its controls to the current game. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });


        game = new HexGame();
        binding.boardView.setGame(game);
        binding.boardView.setOnCellClickListener(this::onCellClicked);
        binding.restartButton.setOnClickListener(view -> restartGame());
        render();

    }


    /**
     * Plays a legal move from a board tap and updates the screen.
     *
     * @param row zero-based row of the tapped cell
     * @param column zero-based column of the tapped cell
     */
    private void onCellClicked(int row, int column) {
        if (game.play(row, column)) {
            render();
        }
    }


    /** Resets the current game and refreshes the screen. */
    private void restartGame() {
        game = new HexGame();
        render();
    }


    /** Refreshes the visible screen from the current game state. */
    private void render() {
        binding.boardView.setGame(game);
        binding.boardView.setEnabled(!game.isOver());
        binding.modelText.setText(R.string.model_local);
        if (game.getWinner() == HexGame.RED) {
            binding.statusText.setText(R.string.status_red_wins);
        } else if (game.getWinner() == HexGame.BLUE) {
            binding.statusText.setText(R.string.status_blue_wins);
        } else {
            binding.statusText.setText(game.getCurrentPlayer() == HexGame.RED
                    ? R.string.status_red_turn : R.string.status_blue_turn);
        }
    }
}