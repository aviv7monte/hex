package com.example.hex;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import com.example.hex.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    // A result belongs only to the board position that requested it.

    private List<ModelCatalog.Level> computerLevels = Collections.emptyList();
    private ModelCatalog.Level selectedLevel;
    private int boardSize = 7; // Matches the first (7×7) board-size option.+    private int modelRequest;
    /** Creates the game screen and connects its controls to the current game. */
    private int modelRequest;

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
        setupBoardSizes();
        setupComputerLevels();
        render();

    }
    /** Builds a board with the size selected in the first dropdown. */
    private void setupBoardSizes() {
        int[] sizes = {7, 11};
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.board_sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.boardSizeSpinner.setAdapter(adapter);
        binding.boardSizeSpinner.setSelection(0, false);
        binding.boardSizeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if (boardSize != sizes[position]) {
                            boardSize = sizes[position];
                            refreshComputerLevels();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Keep the current board size.
                    }
                });
    }
    /** Reads the Java-shaped entries from the asset catalog. */
    private void setupComputerLevels() {
        try {
            computerLevels = ModelCatalog.load(getApplicationContext());
        } catch (Exception exception) {
            computerLevels = Collections.emptyList();
        }
        refreshComputerLevels();
    }
    /** Shows only computer models whose input shape matches the selected board. */
    private void refreshComputerLevels() {
        List<ModelCatalog.Level> matchingLevels = new ArrayList<>();
        for (ModelCatalog.Level level : computerLevels) {
            if (level.boardSize == boardSize) matchingLevels.add(level);
        }

        // Prefer iteration 2,620 for the default 7×7 board when it is available.
        int defaultPosition = 0;
        for (int i = 0; i < matchingLevels.size(); i++) {
            if ("trained-002620".equals(matchingLevels.get(i).id)) {
                defaultPosition = i;
                break;
            }
        }

        ArrayAdapter<ModelCatalog.Level> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, matchingLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.computerLevelSpinner.setOnItemSelectedListener(null);
        binding.computerLevelSpinner.setAdapter(adapter);
        binding.computerLevelSpinner.setEnabled(!matchingLevels.isEmpty());
        binding.computerLevelSpinner.setSelection(defaultPosition, false);
        binding.computerLevelSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        switchComputerLevel(
                                (ModelCatalog.Level) parent.getItemAtPosition(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Keep the current selection.
                    }
                });

        switchComputerLevel(matchingLevels.isEmpty() ? null : matchingLevels.get(defaultPosition));
    }

    /** Loads the selected model and starts a new game for its board size. */
    private void switchComputerLevel(ModelCatalog.Level level) {
        if (level == selectedLevel && (modelLoading || valueModel != null)) return;

        selectedLevel = level;
        int request = ++modelRequest;
        TfliteValueModel previousModel = valueModel;
        valueModel = null;
        modelLoading = level != null;
        restartGame();

        aiExecutor.execute(() -> {
            if (previousModel != null) previousModel.close();
            if (level == null) return;

            TfliteValueModel loaded;
            try {
                loaded = new TfliteValueModel(getApplicationContext(), level.modelAsset);
            } catch (Exception exception) {
                loaded = null;
            }
            TfliteValueModel result = loaded;
            runOnUiThread(() -> {
                if (request != modelRequest) {
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
        game = new HexGame(boardSize);
        positionChanged();
        render();
    }


    /** Refreshes the visible screen from the current game state. */
    private void render() {
        binding.boardView.setGame(game);
        binding.boardView.setContentDescription(getString(R.string.board_description,
                game.getSize(), game.getSize()));
        boolean canTap = !aiThinking && !game.isOver()
                && (!vsAi || (!modelLoading && valueModel != null
                && game.getCurrentPlayer() == HexGame.RED));

        binding.boardView.setEnabled(canTap);
        if (!vsAi) {
            binding.modelText.setText(R.string.model_local);
        } else if (selectedLevel == null || valueModel == null && !modelLoading) {
            binding.modelText.setText(R.string.model_unavailable_help);
        } else if (modelLoading) {
            binding.modelText.setText(getString(R.string.model_loading, selectedLevel.label));
        } else {
            binding.modelText.setText(selectedLevel.description);
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