package dev.hoi.client.ui;

public record UiUnits(int width, int height) {
    public static final int REFERENCE_WIDTH = 1280;
    public static final int REFERENCE_HEIGHT = 720;

    public UiUnits {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Viewport dimensions must be positive");
    }

    public float scale() {
        return Math.min(width / (float) REFERENCE_WIDTH, height / (float) REFERENCE_HEIGHT);
    }

    public float length(double gui2) {
        return (float) (gui2 * scale());
    }

    public int size(double gui2) {
        return (int) Math.ceil(length(gui2));
    }
}
