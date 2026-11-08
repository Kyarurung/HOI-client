package dev.hoi.client.screen.doctrine;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.dialog.DialogScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.function.BiConsumer;

public final class DoctrineNotification extends DialogScreen {
    private final BiConsumer<DialogView, String> send;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total;
    private boolean scrolling, pending;
    public DoctrineNotification(DialogView view, BiConsumer<DialogView, String> send) {
        super(view, send); this.send = send;
    }
    public static boolean matches(DialogView view) {
        return view.presentation().equals("doctrine_reward_complete") || view.presentation().equals("doctrine_milestone_complete");
    }
    public boolean reward() { return view().presentation().equals("doctrine_reward_complete"); }
    @Override public int panelTop() { return top; }
    @Override public int panelHeight() { return panelHeight; }
    @Override public int panelLeft() { return left; }
    @Override public int panelWidth() { return pane; }
    public int scrollOffset() { return scroll; }
    private UiScrollBar bar() { return new UiScrollBar(left + pane - 10, bodyTop, bodyBottom, bodyBottom - bodyTop, total); }
    @Override protected void init() {
        pane = Math.min(width - 16, reward() ? 175 : 250);
        panelHeight = Math.min(height - 16, reward() ? 110 : 160);
        left = reward() ? Math.max(8, width - pane - 43) : (width - pane) / 2;
        top = reward() ? Math.max(8, height - panelHeight - 122) : (height - panelHeight) / 2;
        bodyTop = top + (reward() ? 38 : 42);
        bodyBottom = top + panelHeight - (reward() ? 10 : 30);
        if (!reward()) {
            var confirm = new UiMenuButton(view().choices().stream().filter(c -> c.id().equals("ack")).map(DialogView.Choice::label).findFirst().orElse(""),
                    left + (pane - 62) / 2, top + panelHeight - 24, 62, 17, this::ack).background("completion/button").sound("");
            confirm.active = !pending && view().choices().stream().anyMatch(c -> c.id().equals("ack"));
            addRenderableWidget(confirm);
        }
    }
    @Override public void update(DialogView next) {
        if (!next.token().equals(view().token()) || next.revision() < view().revision()) return;
        if (next.revision() > view().revision()) pending = false;
        super.update(next);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        UiMenuStyle.panel(g, left, top, pane, panelHeight);
        if (!reward()) {
            UiAssets.draw(g, "completion/header", left + 1, top + 4, pane - 2, 43);
            UiAssets.draw(g, "completion/bottom", left + 2, top + panelHeight - 31, pane - 4, 36);
        }
        UiText.centered(g, font, Tooltips.officerText(view().title(), true), left + 12, top + (reward() ? 10 : 12), pane - 24, 20, UiMenuStyle.TEXT);
        var lines = Tooltips.sourceLines(font, view().body(), Math.max(1, pane - 29));
        int lineHeight = font.lineHeight + 2;
        total = lines.size() * lineHeight;
        scroll = Math.clamp(scroll, 0, bar().maximum());
        g.enableScissor(left + 8, bodyTop, left + pane - 12, bodyBottom);
        for (int i = 0; i < lines.size(); i++) {
            int y = bodyTop + i * lineHeight - scroll;
            if (y + lineHeight > bodyTop && y < bodyBottom) g.text(font, lines.get(i), left + (pane - font.width(lines.get(i))) / 2, y, UiMenuStyle.TEXT, false);
        }
        g.disableScissor();
        bar().draw(g, scroll);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, mx, my, delta);
    }
    private void ack() {
        if (pending || view().choices().stream().noneMatch(c -> c.id().equals("ack"))) return;
        pending = true;
        send.accept(view(), "ack");
        rebuildWidgets();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.button() == 0 && bar().contains(event.x(), event.y())) {
            scrolling = !bar().arrow(event.y());
            scroll = scrolling ? bar().offset(event.y()) : bar().step(event.y(), scroll, font.lineHeight + 2);
            return true;
        }
        if (reward() && event.x() >= left && event.x() < left + pane && event.y() >= top && event.y() < top + panelHeight) {
            if (event.button() == 1) ack();
            return true;
        }
        return event.button() != 0 || super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!scrolling) return false;
        scroll = bar().offset(event.y()); return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { boolean result = scrolling; scrolling = false; return result || super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < left || x >= left + pane || y < bodyTop || y >= bodyBottom) return false;
        scroll = Math.clamp(scroll - (int)(vertical * (font.lineHeight + 2) * 3), 0, bar().maximum()); return true;
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (!reward() && (event.key() == 257 || event.key() == 335)) { ack(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { ack(); }
}
