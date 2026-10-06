
package com.example.hex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Draws a Hex board on a {@link Canvas}.
 *
 * <p>The view's intended role also includes cell input; game rules stay in a separate class.
 */
public final class HexBoardView extends View {
    private static final float SQRT_THREE = (float) Math.sqrt(3.0);

    private final HexGame game = new HexGame();

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hexPath = new Path();

    private float radius;
    private float startX;
    private float startY;

    private final int emptyColor;
    private final int redColor;
    private final int blueColor;
    private final int lineColor;

    /**
     * Creates a board view inflated from XML.
     *
     * @param context Android context used to resolve resources
     * @param attributes XML attributes supplied by the layout inflater
     */
    public HexBoardView(Context context, @Nullable AttributeSet attributes) {
        super(context, attributes);
        emptyColor = ContextCompat.getColor(context, R.color.hex_empty);
        redColor = ContextCompat.getColor(context, R.color.hex_red);
        blueColor = ContextCompat.getColor(context, R.color.hex_blue);
        lineColor = ContextCompat.getColor(context, R.color.hex_line);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        sidePaint.setStyle(Paint.Style.STROKE);
        sidePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        calculateGeometry();
        drawGoalSides(canvas);

        strokePaint.setColor(lineColor);
        strokePaint.setStrokeWidth(dp(1.5f));
        for (int row = 0; row < game.getSize(); row++) {
            for (int column = 0; column < game.getSize(); column++) {
                float centerX = centerX(row, column);
                float centerY = centerY(row);
                makeHexagon(centerX, centerY);
                fillPaint.setColor(emptyColor);
                fillPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(hexPath, fillPaint);
                canvas.drawPath(hexPath, strokePaint);
            }
        }
    }

    private void drawGoalSides(Canvas canvas) {
        int last = game.getSize() - 1;
        sidePaint.setStrokeWidth(Math.max(dp(4), radius * 0.18f));

        sidePaint.setColor(redColor);
        canvas.drawLine(centerX(0, 0), centerY(0) - radius * 1.18f,
                centerX(0, last), centerY(0) - radius * 1.18f, sidePaint);
        canvas.drawLine(centerX(last, 0), centerY(last) + radius * 1.18f,
                centerX(last, last),
                centerY(last) + radius * 1.18f, sidePaint);

        sidePaint.setColor(blueColor);
        canvas.drawLine(centerX(0, 0) - radius, centerY(0),
                centerX(last, 0) - radius, centerY(last), sidePaint);
        canvas.drawLine(centerX(0, last) + radius, centerY(0),
                centerX(last, last) + radius,
                centerY(last), sidePaint);
    }

    /** Fits and centers the board geometry inside the current view size. */
    private void calculateGeometry() {
        float inset = dp(14);
        float availableWidth = Math.max(1, getWidth() - 2 * inset);
        float availableHeight = Math.max(1, getHeight() - 2 * inset);
        float widthInHexagons = 1.5f * (game.getSize() - 1) + 1;
        float heightInRadii = 1.5f * (game.getSize() - 1) + 2;
        radius = Math.min(availableWidth / (SQRT_THREE * widthInHexagons),
                availableHeight / (heightInRadii + 0.5f));

        float boardWidth = SQRT_THREE * radius * widthInHexagons;
        float boardHeight = radius * heightInRadii;
        float left = (getWidth() - boardWidth) / 2.0f;
        float top = (getHeight() - boardHeight) / 2.0f;
        startX = left + SQRT_THREE * radius / 2.0f;
        startY = top + radius;
    }

    private void makeHexagon(float centerX, float centerY) {
        hexPath.reset();
        for (int corner = 0; corner < 6; corner++) {
            double angle = Math.toRadians(-90 + 60 * corner);
            float x = centerX + radius * (float) Math.cos(angle);
            float y = centerY + radius * (float) Math.sin(angle);
            if (corner == 0) {
                hexPath.moveTo(x, y);
            } else {
                hexPath.lineTo(x, y);
            }
        }
        hexPath.close();
    }

    private float centerX(int row, int column) {
        return startX + SQRT_THREE * radius * (column + row * 0.5f);
    }

    private float centerY(int row) {
        return startY + radius * 1.5f * row;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}