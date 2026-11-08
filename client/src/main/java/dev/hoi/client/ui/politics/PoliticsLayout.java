package dev.hoi.client.ui.politics;

public record PoliticsLayout(int pane, int top) {
    public record PoliticsBox(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private int usable() { return pane - 16; }
    public PoliticsBox leader() { int w = usable() * 28 / 100; return new PoliticsBox(8, top + 29, w, w * 258 / 172); }
    private int split() { return leader().x() + leader().width() + 6; }
    private int rowHeight() { return (leader().height() - 8) / 3; }
    private int rowY(int row) { return top + 29 + row * (rowHeight() + 4); }
    private int columnWidth() { return rowHeight(); }
    public PoliticsBox focus() { return new PoliticsBox(split() + (int)Math.round(rowHeight() * 4.0 / 107), rowY(0), (int)Math.round(rowHeight() * 359.0 / 107), rowHeight()); }
    public PoliticsBox focusImage() { return new PoliticsBox(split(), rowY(0), rowHeight(), rowHeight()); }
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
    public PoliticsBox union() { return new PoliticsBox(pane - 8 - columnWidth(), rowY(0), columnWidth(), rowHeight()); }
    public PoliticsBox ideology() { return new PoliticsBox(split(), rowY(1), columnWidth(), rowHeight()); }
    public PoliticsBox spirits() { int x = election().x(); return new PoliticsBox(x, rowY(1), pane - 8 - x, rowHeight()); }
    public PoliticsBox government() { return new PoliticsBox(split(), rowY(2), rowHeight(), rowHeight()); }
    public PoliticsBox election() { var b = government(); int x = b.x() + b.width() + 4; return new PoliticsBox(x, b.y(), parties().x() - x - 6, b.height()); }
    public int summaryY() { return leader().y() + leader().height() + 6; }
    public int summaryHeight() { return Math.max(36, usable() * 15 / 100); }
    public PoliticsBox economy() { return new PoliticsBox(8, summaryY(), summaryHeight(), summaryHeight()); }
    public PoliticsBox faction() { var b = economy(); return new PoliticsBox(b.x() + b.width() + 6, b.y(), b.width(), b.height()); }
    public PoliticsBox parties() { int x = 8 + usable() * 64 / 100; return new PoliticsBox(x, rowY(2), pane - 8 - x, summaryY() + summaryHeight() - rowY(2)); }
    public PoliticsBox partyChart() {
        int size = Math.min(summaryHeight(), parties().x() - faction().x() - faction().width() - 30);
        return new PoliticsBox(parties().x() - size - 5, summaryY() + (summaryHeight() - size) / 2, size, size);
    }
    public PoliticsBox status(int index) {
        var f = faction(); int left = f.x() + f.width(), space = partyChart().x() - left, size = Math.min(20, space / 2 - 4);
        return new PoliticsBox(partyChart().x() - 2 - (2 - index) * (size + 3), summaryY() + summaryHeight() - size - 3, size, size);
    }
    public int contentTop() { return summaryY() + summaryHeight() + 9; }
}
