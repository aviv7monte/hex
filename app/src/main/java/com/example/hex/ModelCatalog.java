package com.example.hex;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Reads the bundled computer-player list into Java objects. */
public final class ModelCatalog {
    public static final String CATALOG_ASSET = "model_catalog.json";

    private ModelCatalog() {
    }

    /** Loads every level listed in the asset catalog. */
    public static List<Level> load(Context context) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                context.getAssets().open(CATALOG_ASSET), StandardCharsets.UTF_8)) {
            Level[] levels = new Gson().fromJson(reader, Level[].class);
            return Arrays.asList(levels);
        }
    }

    /** Fields map directly from one catalog object. */
    public static final class Level {
        public String id;
        public String label;
        public String description;
        public String modelAsset;
        public int boardSize;

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}