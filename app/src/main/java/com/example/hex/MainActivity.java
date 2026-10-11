package com.example.hex;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hex.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Connects board taps to the independent game state. */

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private HexGame game;

    private TfliteValueModel valueModel;
    private ExecutorService aiExecutor;
    private boolean vsAi = true;
    private boolean modelLoading;
    private boolean aiThinking;
    // A result belongs only to the board position that requested it.
    private volatile int positionRevision;

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

        aiExecutor = Executors.newSingleThreadExecutor();
        game = new HexGame();
        binding.modeAi.setChecked(vsAi);
        binding.modeHuman.setChecked(!vsAi);
        binding.boardView.setGame(game);
        binding.boardView.setOnCellClickListener(this::onCellClicked);
        binding.restartButton.setOnClickListener(view -> restartGame());
        binding.modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean requestedAi = checkedId == R.id.modeAi;
            if (requestedAi != vsAi) {
                vsAi = requestedAi;
                restartGame();
            }
        });
        loadModel();
        render();

    }
    /** Loads the single 7x7 model while the screen remains responsive. */
    private void loadModel() {
        modelLoading = true;
        aiExecutor.execute(() -> {
            TfliteValueModel loaded = null;
            try {
                loaded = new TfliteValueModel(getApplicationContext());
            } catch (Exception ignored) {
                // render() displays that the computer is unavailable.
            }
            TfliteValueModel result = loaded;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    if (result != null) result.close();
                    return;
                }
                valueModel = result;
                modelLoading = false;
                render();
            });
        });
    }


    /**
     * Plays a legal move from a board tap and updates the screen.
     *
     * @param row zero-based row of the tapped cell
     * @param column zero-based column of the tapped cell
     */
    private void onCellClicked(int row, int column) {
        if (game.play(row, column)) {
            positionChanged();
            render();
            if (vsAi && !game.isOver()) requestComputerMove();
        }
    }

    /** Evaluates a copied position on the background worker. */
    private void requestComputerMove() {
        aiThinking = true;
        render();
        int revision = positionRevision;
        HexGame position = game.copy();
        TfliteValueModel model = valueModel;

        aiExecutor.execute(() -> {
            try {
                HexGame.Move move = new HexAi(model).chooseMove(position);
                runOnUiThread(() -> finishComputerMove(revision, model, move, false));
            } catch (RuntimeException exception) {
                runOnUiThread(() -> finishComputerMove(revision, model, null, true));
            }
        });
    }

    /** Applies a result only while it still belongs to the displayed board. */
    private void finishComputerMove(int revision, TfliteValueModel model,
                                    HexGame.Move move, boolean failed) {
        if (revision != positionRevision) return;
        aiThinking = false;
        if (failed) {
            valueModel = null;
            // Queue cleanup after inference has finished on the same worker.
            aiExecutor.execute(model::close);
        } else {
            game.play(move.row, move.column);
            positionChanged();
        }
        render();
    }

    /** A changed board makes earlier background results obsolete. */
    private void positionChanged() {
        positionRevision++;
        aiThinking = false;
    }


    /** Resets the current game and refreshes the screen. */
    private void restartGame() {
        game = new HexGame();
        positionChanged();
        render();
    }


    /** Refreshes the visible screen from the current game state. */
    private void render() {
        binding.boardView.setGame(game);

        boolean canTap = !aiThinking && !game.isOver()
                && (!vsAi || (!modelLoading && valueModel != null
                && game.getCurrentPlayer() == HexGame.RED));

        binding.boardView.setEnabled(canTap);
        if (!vsAi) {
            binding.modelText.setText(R.string.model_local);
        } else if (modelLoading) {
            binding.modelText.setText(R.string.model_loading);
        } else if (valueModel == null) {
            binding.modelText.setText(R.string.model_unavailable_help);
        } else {
            binding.modelText.setText(R.string.model_ready);
        }
        if (game.getWinner() == HexGame.RED) {
            binding.statusText.setText(R.string.status_red_wins);
        } else if (game.getWinner() == HexGame.BLUE) {
            binding.statusText.setText(R.string.status_blue_wins);
        } else if (vsAi && modelLoading) {
            binding.statusText.setText(R.string.status_model_loading);
        } else if (vsAi && valueModel == null) {
            binding.statusText.setText(R.string.ai_unavailable);
        } else if (aiThinking) {
            binding.statusText.setText(R.string.status_ai_thinking);
        } else if (vsAi) {
            binding.statusText.setText(R.string.status_your_turn);
        } else {
            binding.statusText.setText(game.getCurrentPlayer() == HexGame.RED
                    ? R.string.status_red_turn : R.string.status_blue_turn);
        }
    }
    /** Invalidates pending results and releases the model after background work. */
    @Override
    protected void onDestroy() {
        positionChanged();
        TfliteValueModel model = valueModel;
        if (model != null) aiExecutor.execute(model::close);
        aiExecutor.shutdown();
        super.onDestroy();
    }
}