package dev.hoi.client.ui;

public record UiPoliticsLayout(int pane, int top) {
    public record UiPoliticsBox(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private int usable() { return pane - 16; }
    public UiPoliticsBox leader() { int w = usable() * 28 / 100; return new UiPoliticsBox(8, top + 29, w, w * 258 / 172); }
    private int split() { return leader().x() + leader().width() + 6; }
    private int rowHeight() { return (leader().height() - 8) / 3; }
    private int rowY(int row) { return top + 29 + row * (rowHeight() + 4); }
    private int columnWidth() { return rowHeight(); }
    public UiPoliticsBox focus() { return new UiPoliticsBox(split() + (int)Math.round(rowHeight() * 4.0 / 107), rowY(0), (int)Math.round(rowHeight() * 359.0 / 107), rowHeight()); }
    public UiPoliticsBox focusImage() { return new UiPoliticsBox(split(), rowY(0), rowHeight(), rowHeight()); }
    public UiPoliticsBox focusArtwork() {
        var b = focusImage();
        return new UiPoliticsBox(b.x() - b.width() / 2 - b.width() / 3, b.y() - b.height() / 2, b.width() * 2, b.height() * 2);
    }
    public UiPoliticsBox focusTitle() {
        var b = focus(); double scale = b.height() / 107.0;
        return new UiPoliticsBox(b.x() + (int)Math.round(99 * scale), b.y() + (int)Math.round(12 * scale),
                (int)Math.round(258 * scale), (int)Math.round(83 * scale));
    }
    public UiPoliticsBox focusProgress() {
        var b = focus(); double scale = b.height() / 107.0;
        return new UiPoliticsBox(b.x() + (int)Math.round(115 * scale), b.y() + (int)Math.round(70 * scale),
                (int)Math.round(212 * scale), Math.max(2, (int)Math.round(6 * scale)));
    }
    public UiPoliticsBox union() { return new UiPoliticsBox(pane - 8 - columnWidth(), rowY(0), columnWidth(), rowHeight()); }
    public UiPoliticsBox ideology() { return new UiPoliticsBox(split(), rowY(1), columnWidth(), rowHeight()); }
    public UiPoliticsBox spirits() { int x = election().x(); return new UiPoliticsBox(x, rowY(1), pane - 8 - x, rowHeight()); }
    public UiPoliticsBox government() { return new UiPoliticsBox(split(), rowY(2), rowHeight(), rowHeight()); }
    public UiPoliticsBox election() { var b = government(); int x = b.x() + b.width() + 4; return new UiPoliticsBox(x, b.y(), parties().x() - x - 6, b.height()); }
    public int summaryY() { return leader().y() + leader().height() + 6; }
    public int summaryHeight() { return Math.max(36, usable() * 15 / 100); }
    public UiPoliticsBox economy() { return new UiPoliticsBox(8, summaryY(), summaryHeight(), summaryHeight()); }
    public UiPoliticsBox faction() { var b = economy(); return new UiPoliticsBox(b.x() + b.width() + 6, b.y(), b.width(), b.height()); }
    public UiPoliticsBox parties() { int x = 8 + usable() * 64 / 100; return new UiPoliticsBox(x, rowY(2), pane - 8 - x, summaryY() + summaryHeight() - rowY(2)); }
    public UiPoliticsBox partyChart() {
        int size = Math.min(summaryHeight(), parties().x() - faction().x() - faction().width() - 30);
        return new UiPoliticsBox(parties().x() - size - 5, summaryY() + (summaryHeight() - size) / 2, size, size);
    }
    public UiPoliticsBox status(int index) {
        var f = faction(); int left = f.x() + f.width(), space = partyChart().x() - left, size = Math.min(20, space / 2 - 4);
        return new UiPoliticsBox(partyChart().x() - 2 - (2 - index) * (size + 3), summaryY() + summaryHeight() - size - 3, size, size);
    }
    public int contentTop() { return summaryY() + summaryHeight() + 9; }
}
