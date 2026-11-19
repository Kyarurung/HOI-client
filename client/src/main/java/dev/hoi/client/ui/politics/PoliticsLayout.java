package dev.hoi.client.ui.politics;

public record PoliticsLayout(int pane, int top) {
    public static final int WIDTH = 726, ART_X = 2, ART_Y = 40, ART_WIDTH = 719, ART_HEIGHT = 394, IDEAS_Y = 435;
    public record PoliticsBox(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    public double scale() { return pane / (double)WIDTH; }
    public PoliticsBox box(double x, double y, double width, double height) {
        double scale = scale();
        int left = (int)Math.round(x * scale), upper = top + (int)Math.round(y * scale);
        return new PoliticsBox(left, upper, (int)Math.round((x + width) * scale) - left, top + (int)Math.round((y + height) * scale) - upper);
    }
    public PoliticsBox leader() { var b = box(6, 49, 172, 258); return new PoliticsBox(b.x(), b.y(), b.width(), (int)Math.round(b.width() * 258.0 / 172)); }
    public PoliticsBox focus() { var b = box(187, 52, 359, 107); return new PoliticsBox(b.x(), b.y(), (int)Math.round(b.height() * 359.0 / 107), b.height()); }
    public PoliticsBox focusImage() { var b = focus(); int inset = (int)Math.round(b.height() * 4.0 / 107); return new PoliticsBox(b.x() - inset, b.y(), b.height(), b.height()); }
    public PoliticsBox focusArtwork() {
        var b = focusImage();
        return new PoliticsBox(b.x() - b.width() / 2 - b.width() / 3, b.y() - b.height() / 2, b.width() * 2, b.height() * 2);
    }
    public PoliticsBox focusTitle() {
        var b = focus(); double scale = b.height() / 107.0;
        return new PoliticsBox(b.x() + (int)Math.round(99 * scale), b.y() + (int)Math.round(12 * scale),
                (int)Math.round(258 * scale), (int)Math.round(83 * scale));
    }
    public PoliticsBox focusProgress() {
        var b = focus(); double scale = b.height() / 107.0;
        return new PoliticsBox(b.x() + (int)Math.round(115 * scale), b.y() + (int)Math.round(70 * scale),
                (int)Math.round(212 * scale), Math.max(2, (int)Math.round(6 * scale)));
    }
    public PoliticsBox union() { return box(585, 59, 98, 91); }
    public PoliticsBox ideology() { return box(180, 165, 91, 81); }
    public PoliticsBox spirits() { return box(280, 164, 430, 82); }
    public PoliticsBox government() { return box(180, 253, 77, 77); }
    public PoliticsBox election() { return box(257, 253, 122, 77); }
    public PoliticsBox parties() { return box(508, 254, 202, 169); }
    public PoliticsBox economy() { return box(13, 320, 75, 69); }
    public PoliticsBox economyName() { return box(13, 394, 162, 33); }
    public PoliticsBox faction() { return box(99, 320, 75, 69); }
    public PoliticsBox partyChart() { var b = box(398, 325, 90, 90); return new PoliticsBox(b.x(), b.y(), b.width(), b.width()); }
    public PoliticsBox balance() { return box(254, 339, 125, 59); }
    public PoliticsBox status(int index) { return box(index == 0 ? 315 : 350, 398, 34, 33); }
    public int categoryHeader() { return (int)Math.round(52 * scale()); }
    public int contentTop() { return top + (int)Math.round(IDEAS_Y * scale()) + 2; }
}
