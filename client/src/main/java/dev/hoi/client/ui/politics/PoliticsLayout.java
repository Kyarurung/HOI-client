package dev.hoi.client.ui.politics;

public record PoliticsLayout(int pane, int top, float left) {
    public static final int WIDTH = 726, ART_X = 2, ART_Y = 40, ART_WIDTH = 719, ART_HEIGHT = 394, IDEAS_Y = 435, CATEGORY_Y = 465, CATEGORY_PITCH = 110;
    public PoliticsLayout(int pane, int top) { this(pane, top, 0); }
    public record PoliticsBox(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    public double scale() { return pane / (double)WIDTH; }
    public float x(double x) { return left + (float)(x * scale()); }
    public float y(double y) { return top + (float)(y * scale()); }
    public PoliticsBox box(double x, double y, double width, double height) {
        int left = Math.round(x(x)), upper = Math.round(y(y));
        return new PoliticsBox(left, upper, Math.round(x(x + width)) - left, Math.round(y(y + height)) - upper);
    }
    public PoliticsBox leader() { return box(6, 49, 172, 258); }
    public PoliticsBox portrait() { return box(13, 55, 156, 210); }
    public PoliticsBox leaderName() { return box(15, 275, 155, 20); }
    public PoliticsBox focus() { return box(187, 52, 359, 107); }
    public PoliticsBox focusImage() { return box(197, 65, 80, 80); }
    public PoliticsBox focusArtwork() { return box(187, 55, 100, 100); }
    public PoliticsBox focusTitle() { return box(280, 64, 258, 83); }
    public PoliticsBox focusName() { return box(307, 92, 200, 20); }
    public PoliticsBox focusProgress() { return box(302, 122, 212, 6); }
    public PoliticsBox union() { return box(573, 51, 121, 114); }
    public PoliticsBox ideology() { return box(193, 174, 66, 68); }
    public PoliticsBox spirits() { return box(281, 165, 428, 90); }
    public PoliticsBox subideology() { return box(192, 257, 175, 24); }
    public PoliticsBox government() { return box(178, 337, 68, 68); }
    public PoliticsBox governmentName() { return box(253, 344, 125, 30); }
    public PoliticsBox election() { return box(192, 294, 140, 22); }
    public PoliticsBox parties() { return box(500, 251, 205, 173); }
    public PoliticsBox economy() { return box(15, 322, 68, 68); }
    public PoliticsBox economyName() { return box(9, 398, 175, 34); }
    public PoliticsBox faction() { return box(105, 325, 68, 68); }
    public PoliticsBox partyChart() { return box(398, 325.5, 90, 90); }
    public PoliticsBox balance() { return box(379, 253, 129, 57); }
    public PoliticsBox status(int index) { return box(index == 0 ? 315 : 350, 398, 34, 33); }
    public PoliticsBox rules() { return box(625, 7, 38, 42); }
    public int categoryHeader() { return (int)Math.round(52 * scale()); }
    public int contentTop() { return Math.round(y(IDEAS_Y)) + 2; }
}
