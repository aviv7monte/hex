package com.example.hex;

import android.content.Context;

import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.LiteRtException;
import com.google.ai.edge.litert.TensorBuffer;

import java.util.List;

/** Runs the 7x7 value model with LiteRT. */
public final class TfliteValueModel implements ValueModel, AutoCloseable {
    /** Compiled once when the computer player is selected. */
    private final CompiledModel compiledModel;
    /** Reused for every candidate position. */
    private final List<TensorBuffer> inputBuffers;
    /** Receives the predicted value. */
    private final List<TensorBuffer> outputBuffers;

    /** Load the model and create buffers for its first signature. */
    public TfliteValueModel(Context context) throws LiteRtException {
        compiledModel = CompiledModel.create(context.getAssets(),
                "hex_value_v1.tflite", CompiledModel.Options.getCPU());
        inputBuffers = compiledModel.createInputBuffers(0);
        outputBuffers = compiledModel.createOutputBuffers(0);
    }

    @Override
    public float evaluate(float[] encodedBoard) {
        try {
            // Copy the encoded board into the model's input.
            inputBuffers.get(0).writeFloat(encodedBoard);
            // Run the model using its first signature.
            compiledModel.run(inputBuffers, outputBuffers, 0);
            // Read the predicted value.
            return outputBuffers.get(0).readFloat()[0];
        } catch (LiteRtException exception) {
            throw new IllegalStateException("Could not evaluate the model", exception);
        }
    }

    @Override
    public void close() {
        // Release buffers before the compiled model that created them.
        for (TensorBuffer buffer : inputBuffers) buffer.close();
        for (TensorBuffer buffer : outputBuffers) buffer.close();
        compiledModel.close();
    }
}