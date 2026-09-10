package dev.hoi.client.ui;

public record PoliticsLayout(int pane, int top) {
    public record Box(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private int usable() { return pane - 16; }
    public Box leader() { int w = usable() * 28 / 100; return new Box(8, top + 29, w, w * 258 / 172); }
    private int split() { return leader().x() + leader().width() + 6; }
    private int rowHeight() { return (leader().height() - 8) / 3; }
    private int rowY(int row) { return top + 29 + row * (rowHeight() + 4); }
    private int columnWidth() { return rowHeight(); }
    public Box focus() { return new Box(split(), rowY(0), (int)Math.round(rowHeight() * 359.0 / 107), rowHeight()); }
    public Box focusImage() { var b = focus(); return new Box(b.x(), b.y(), Math.min(b.height(), b.width() / 3), b.height()); }
    public Box focusTitle() {
        var b = focus(); double scale = b.height() / 107.0;
        return new Box(b.x() + (int)Math.round(99 * scale), b.y() + (int)Math.round(12 * scale),
                (int)Math.round(258 * scale), (int)Math.round(83 * scale));
    }
    public Box union() { return new Box(pane - 8 - columnWidth(), rowY(0), columnWidth(), rowHeight()); }
    public Box ideology() { return new Box(split(), rowY(1), columnWidth(), rowHeight()); }
    public Box spirits() { return new Box(split() + columnWidth() + 6, rowY(1), pane - 14 - split() - columnWidth(), rowHeight()); }
    public Box government() { return new Box(split(), rowY(2), rowHeight(), rowHeight()); }
    public Box election() { var b = government(); int x = b.x() + b.width() + 4; return new Box(x, b.y(), parties().x() - x - 6, b.height()); }
    public int summaryY() { return leader().y() + leader().height() + 6; }
    public int summaryHeight() { return Math.max(36, usable() * 15 / 100); }
    public Box economy() { return new Box(8, summaryY(), usable() * 16 / 100, summaryHeight()); }
    public Box faction() { var b = economy(); return new Box(b.x() + b.width() + 6, b.y(), b.width(), b.height()); }
    public Box parties() { int x = 8 + usable() * 64 / 100; return new Box(x, rowY(2), pane - 8 - x, summaryY() + summaryHeight() - rowY(2)); }
    public Box partyChart() {
        int size = Math.min(summaryHeight(), parties().x() - faction().x() - faction().width() - 30);
        return new Box(parties().x() - size - 5, summaryY() + (summaryHeight() - size) / 2, size, size);
    }
    public Box status(int index) {
        var f = faction(); int left = f.x() + f.width(), space = partyChart().x() - left, size = Math.min(20, space / 2 - 4);
        return new Box(partyChart().x() - 2 - (2 - index) * (size + 3), summaryY() + summaryHeight() - size - 3, size, size);
    }
    public int contentTop() { return summaryY() + summaryHeight() + 9; }
}
