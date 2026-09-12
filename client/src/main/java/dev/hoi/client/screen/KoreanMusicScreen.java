package dev.hoi.client.screen;

import dev.hoi.client.audio.KoreanMusic;
import dev.hoi.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.List;
import dev.hoi.client.audio.PlaylistPlayback;

public final class KoreanMusicScreen extends Screen {
    private final Screen parent;
    private int x, y, w, h, offset, visible;
    private List<PlaylistPlayback.Track> tracks;
    public KoreanMusicScreen(Screen parent) { super(Component.literal("한국 음악")); this.parent = parent instanceof DialogStackScreen stack ? stack.backdrop() : parent; }
    @Override protected void init() {
        w = Math.min(460, width - 12); h = Math.min(450, height - 12);
        x = (width - w) / 2; y = (height - h) / 2;
        visible = Math.max(1, (h - 99) / 25);
        var playback = KoreanMusic.PLAYBACK; var items = playback.tracks(); tracks = items;
        offset = Math.clamp(offset, 0, Math.max(0, items.size() - visible));
        addRenderableWidget(new PanelButton("×", x + w - 25, y + 3, 22, 20, this::onClose));
        for (int i = offset; i < Math.min(items.size(), offset + visible); i++) {
            int index = i; var track = items.get(i);
            var button = new PanelButton(track.title(), x + 10, y + 51 + (i - offset) * 25, w - 53, 23, () -> KoreanMusic.play(index));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(track.title())));
            addRenderableWidget(button);
        }
        int controlWidth = (w - 32) / 4;
        var prev = addRenderableWidget(new PanelButton("이전 곡", x + 10, y + h - 32, controlWidth, 23, () -> { if (!items.isEmpty()) KoreanMusic.play(Math.floorMod(playback.selected() - 1, items.size())); }));
        var start = addRenderableWidget(new PanelButton("재생", x + 14 + controlWidth, y + h - 32, controlWidth, 23, () -> { if (!items.isEmpty()) KoreanMusic.play(playback.selected()); }));
        addRenderableWidget(new PanelButton("정지", x + 18 + controlWidth * 2, y + h - 32, controlWidth, 23, KoreanMusic::stop));
        var next = addRenderableWidget(new PanelButton("다음 곡", x + 22 + controlWidth * 3, y + h - 32, controlWidth, 23, () -> { if (!items.isEmpty()) KoreanMusic.play((playback.selected() + 1) % items.size()); }));
        prev.active = start.active = next.active = !items.isEmpty();
    }
    @Override public void tick() { if (tracks != KoreanMusic.PLAYBACK.tracks()) rebuildWidgets(); }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mx >= x && mx < x + w && my >= y + 45 && my < y + h - 37) {
            offset = Math.clamp(offset - (int)Math.signum(vertical) * 3, 0, Math.max(0, KoreanMusic.PLAYBACK.tracks().size() - visible));
            rebuildWidgets(); return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuStyle.panel(g, x, y, w, h);
        g.text(font, "한국 음악", x + 12, y + 9, HoiMenuStyle.TEXT);
        var p = KoreanMusic.PLAYBACK; var items = p.tracks();
        String status = items.isEmpty() ? "한국 음악 리소스팩이 필요합니다." : p.requested() ? "재생 중: " + items.get(p.selected()).title() : "곡을 선택하면 순서대로 반복 재생합니다.";
        g.text(font, font.plainSubstrByWidth(status, w - 24), x + 12, y + 34, HoiMenuStyle.MUTED);
        super.extractRenderState(g, mx, my, delta);
        for (int i = offset; i < Math.min(items.size(), offset + visible); i++) {
            int row = y + 51 + (i - offset) * 25;
            long seconds = (long)items.get(i).seconds();
            g.text(font, String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60), x + w - 38, row + 8, HoiMenuStyle.MUTED);
            if (i == p.selected() && p.requested()) g.fill(x + 11, row + 2, x + 13, row + 21, 0xFF77A65F);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
