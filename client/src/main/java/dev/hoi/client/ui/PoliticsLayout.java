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
    private int columnWidth() { return usable() * 18 / 100; }
    public Box focus() { return new Box(split(), rowY(0), pane - 14 - columnWidth() - split(), rowHeight()); }
    public Box focusImage() { var b = focus(); return new Box(b.x(), b.y(), Math.min(b.height(), b.width() / 3), b.height()); }
    public Box focusTitle() { var b = focus(); int x = focusImage().x() + focusImage().width(); return new Box(x, b.y(), b.x() + b.width() - x, b.height()); }
    public Box union() { return new Box(pane - 8 - columnWidth(), rowY(0), columnWidth(), rowHeight()); }
    public Box ideology() { return new Box(split(), rowY(1), columnWidth(), rowHeight()); }
    public Box spirits() { return new Box(split() + columnWidth() + 6, rowY(1), pane - 14 - split() - columnWidth(), rowHeight()); }
    public Box government() { return new Box(split(), rowY(2), parties().x() - split() - 6, (rowHeight() - 3) / 2); }
    public Box election() { var b = government(); int y = b.y() + b.height() + 3; return new Box(b.x(), y, b.width(), rowY(2) + rowHeight() - y); }
    public int summaryY() { return leader().y() + leader().height() + 6; }
    public int summaryHeight() { return Math.max(36, usable() * 15 / 100); }
    public Box economy() { return new Box(8, summaryY(), usable() * 16 / 100, summaryHeight()); }
    public Box faction() { var b = economy(); return new Box(b.x() + b.width() + 6, b.y(), b.width(), b.height()); }
    public Box parties() { int x = 8 + usable() * 64 / 100; return new Box(x, rowY(2), pane - 8 - x, summaryY() + summaryHeight() - rowY(2)); }
    public Box partyChart() { return new Box(parties().x() - 38, summaryY(), 34, summaryHeight()); }
    public Box status(int index) {
        var f = faction(); int left = f.x() + f.width(), space = partyChart().x() - left, size = Math.min(20, space / 2 - 4);
        return new Box(partyChart().x() - 2 - (2 - index) * (size + 3), summaryY() + summaryHeight() - size - 3, size, size);
    }
    public int contentTop() { return summaryY() + summaryHeight() + 9; }
}
