package dev.hoi.client.screen.dialog;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiText;
import dev.hoi.protocol.dialog.DialogView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.function.BiConsumer;

final class DialogLeaderEvent extends DialogScreen {
    private final BiConsumer<DialogView, String> send;
    private int left, top, pane, panelHeight, middleHeight, optionsHeight, bodyTop, bodyBottom, scroll, total, page, capacity;
    private double scale;
    private boolean pending;
    private boolean dragging;
    private int offsetX, offsetY;
    DialogLeaderEvent(DialogView view, BiConsumer<DialogView, String> send) { super(view); this.send = send; }
    @Override public int panelLeft() { return left; }
    @Override public int panelTop() { return top; }
    @Override public int panelWidth() { return pane; }
    @Override public int panelHeight() { return panelHeight; }
    @Override int bodyViewportHeight() { return bodyBottom - bodyTop; }
    @Override public int scrollOffset() { return scroll; }
    private int units(double value) { return (int)Math.round(value * scale); }
    int[] bounds(double x, double y, double w, double h) { return new int[]{left + units(x), top + units(y), units(w), units(h)}; }
    String portraitPath() { return UiAssets.imageHeight(view().image(), 156) > 0 ? view().image() : "politics/empty/leader"; }
    @Override public void update(DialogView next) {
        if (!view().token().equals(next.token()) || next.revision() < view().revision()) return;
        if (next.revision() > view().revision()) pending = false;
        super.update(next);
    }
    @Override protected void choose(String choice) {
        if (pending || !choice.isEmpty() && view().choices().stream().noneMatch(c -> c.id().equals(choice))) return;
        pending = true;
        children().stream().filter(UiMenuButton.class::isInstance).map(UiMenuButton.class::cast).filter(b -> !b.getMessage().getString().equals("최소화")).forEach(b -> b.active = false);
        send.accept(view(), choice);
    }
    @Override protected void init() {
        resetChoiceTooltips();
        double screenScale = Math.min(width / 1280.0, height / 720.0);
        pane = Math.min(width - 16, Math.max(240, (int)Math.round(605 * .5 * screenScale)));
        scale = pane / 605.0;
        total = dev.hoi.client.ui.tooltip.Tooltips.split(font,bodyText(), Math.max(1, (int)(units(350) / textScale()))).size() * 12;
        int buttonHeight = Math.max(units(48), view().choices().stream().mapToInt(c -> UiMenuButton.labelHeight(font, c.label(), units(352), textScale())).max().orElse(0));
        boolean wrapped = buttonHeight > units(48);
        int slotHeight = wrapped ? buttonHeight + 2 : units(47);
        int available = Math.max(1, height - 16 - units(121 + 68));
        capacity = Math.clamp((available - units(197.2 + 10) - 24) / Math.max(1, slotHeight), 1, 24);
        int pages = Math.max(1, (view().choices().size() + capacity - 1) / capacity);
        page = Math.clamp(page, 0, pages - 1);
        int shown = Math.min(capacity, Math.max(0, view().choices().size() - page * capacity));
        optionsHeight = (wrapped ? units(10) + shown * slotHeight : units(10 + shown * 47)) + (pages > 1 ? 24 : 0);
        middleHeight = Math.min(Math.max(units(235), total), Math.max(1, available - optionsHeight));
        panelHeight = units(121 + 68) + middleHeight + optionsHeight;
        left = Math.clamp(width / 2 - units(282) + offsetX, 8, Math.max(8, width - pane - 8));
        top = Math.clamp(height / 2 - units(310) + offsetY, 8, Math.max(8, height - panelHeight - 8));
        dragging = false;
        bodyTop = top + units(121);
        bodyBottom = bodyTop + middleHeight;
        scroll = Math.clamp(scroll, 0, Math.max(0, total - bodyViewportHeight()));
        minimizeButton(left + units(10), top + units(45), units(33));
        for (int i = 0; i < shown; i++) {
            var choice = view().choices().get(page * capacity + i);
            var button = new UiMenuButton(choice.label(), left + units(115), bodyBottom + (wrapped ? units(10) + i * slotHeight : units(10 + i * 47)), units(352), buttonHeight, () -> choose(choice.id())).background("events/leader/choice").textShadow(false).wrapLabel();
            button.textScale(textScale());
            button.active = !pending;
            choiceTooltip(button, choice);
            addRenderableWidget(button);
        }
        if (pages > 1) {
            int y = bodyBottom + optionsHeight - 22;
            int controlWidth = Math.min(80, (units(352) - 4) / 2);
            var previous = new UiMenuButton("이전 선택지", left + units(115), y, controlWidth, 20, () -> { page--; rebuildWidgets(); });
            previous.active = !pending && page > 0;
            addRenderableWidget(previous);
            var next = new UiMenuButton("다음 선택지", left + units(467) - controlWidth, y, controlWidth, 20, () -> { page++; rebuildWidgets(); });
            next.active = !pending && page + 1 < pages;
            addRenderableWidget(next);
        }
    }
    private void sprite(GuiGraphicsExtractor g, String path, double x, double y, double w, double h) {
        UiAssets.draw(g, "events/leader/" + path, left + units(x), top + units(y), units(w), units(h));
    }
    private void tile(GuiGraphicsExtractor g, String path, int y, int height) {
        if (height <= 0) return;
        var id = Identifier.parse("hoi:textures/gui/events/leader/" + path + ".png");
        int edge = Math.min(height / 2, units(16));
        g.blit(id, left, y, left + pane, y + edge, 0, 1, 0, .2f);
        int tile = Math.max(1, units(48));
        for (int offset = edge; offset < height - edge; offset += tile) {
            int part = Math.min(tile, height - edge - offset);
            g.blit(id, left, y + offset, left + pane, y + offset + part, 0, 1, .2f, .2f + .6f * part / tile);
        }
        g.blit(id, left, y + height - edge, left + pane, y + height, 0, 1, .8f, 1);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!stacked) g.fill(0, 0, width, height, 0x70000000);
        sprite(g, "top", 0, 0, 605, 123);
        tile(g, "middle", bodyTop, middleHeight);
        tile(g, "options", bodyBottom, optionsHeight);
        UiAssets.draw(g, "events/leader/end", left + units(1), bodyBottom + optionsHeight, units(605), units(68));
        int[] portrait = bounds(43, 125, 156 * .85, 210 * .85);
        UiAssets.draw(g, portraitPath(), portrait[0], portrait[1], portrait[2], portrait[3]);
        sprite(g, "frame", 35, 116, 172 * .85, 232 * .85);
        eventTitle(g, left + units(14), top + units(70), units(551), Math.max(units(32), (int)Math.ceil(font.lineHeight * eventTitleScale(units(551)))), 0xff000000, false);
        g.enableScissor(left + units(198), bodyTop, left + units(548), bodyBottom);
        int y = bodyTop - scroll;
        for (var line : dev.hoi.client.ui.tooltip.Tooltips.split(font,bodyText(), Math.max(1, (int)(units(350) / textScale())))) {
            UiText.text(g, font, line, left + units(198), y, 0xff000000, false);
            y += 12;
        }
        g.disableScissor();
        for (var child : children()) if (child instanceof UiMenuButton button) button.extractRenderState(g, mx, my, delta);
        renderChoiceTooltip(g, mx, my);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (scrollChoiceTooltip(x, y, vertical)) return true;
        if (x >= left + units(198) && x <= left + units(548) && y >= bodyTop && y <= bodyBottom) {
            scroll = Math.clamp(scroll - (int)(vertical * 32), 0, Math.max(0, total - bodyViewportHeight()));
            return true;
        }
        return false;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (super.mouseClicked(event, twice)) return true;
        dragging = event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
            && event.x() >= left && event.x() < left + pane && event.y() >= top && event.y() < bodyTop;
        return dragging;
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (!dragging || event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return super.mouseDragged(event, dx, dy);
        int nextLeft = Math.clamp(left + (int)Math.round(dx), 8, Math.max(8, width - pane - 8));
        int nextTop = Math.clamp(top + (int)Math.round(dy), 8, Math.max(8, height - panelHeight - 8));
        int moveX = nextLeft - left, moveY = nextTop - top;
        left = nextLeft; top = nextTop; bodyTop += moveY; bodyBottom += moveY;
        offsetX += moveX; offsetY += moveY;
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
            widget.setPosition(widget.getX() + moveX, widget.getY() + moveY);
        return true;
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && dragging) { dragging = false; return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int maximum = Math.max(0, total - bodyViewportHeight());
        switch (event.key()) {
            case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP -> scroll = Math.max(0, scroll - bodyViewportHeight());
            case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN -> scroll = Math.min(maximum, scroll + bodyViewportHeight());
            case com.mojang.blaze3d.platform.InputConstants.KEY_HOME -> scroll = 0;
            case com.mojang.blaze3d.platform.InputConstants.KEY_END -> scroll = maximum;
            default -> { return super.keyPressed(event); }
        }
        return true;
    }
}
