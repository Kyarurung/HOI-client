package dev.hoi.client.screen.dialog;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.dialog.DialogView;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DialogLeaseNotification extends DialogScreen {
    private final dev.hoi.client.ui.UiDiplomacyAssets assets = new dev.hoi.client.ui.UiDiplomacyAssets();
    private int left, top, scroll, maximum;
    private double scale;
    private boolean pending;
    private boolean dragging;
    public DialogLeaseNotification(DialogView view, BiConsumer<DialogView, String> send) { super(view, send); }
    public static boolean matches(DialogView view) { return view.presentation().equals("lend_lease_request") || view.presentation().equals("lend_lease_reply"); }
    private int s(int value) { return (int)Math.round(value * scale); }
    @Override public int panelLeft() { return left; }
    @Override public int panelTop() { return top; }
    @Override public int panelWidth() { return s(350); }
    @Override public int panelHeight() { return s(220); }
    @Override public void update(DialogView next) {
        if (next.token().equals(view().token()) && next.revision() > view().revision()) pending = false;
        super.update(next);
    }
    @Override protected void init() {
        scale = Math.max(.1, Math.min(.5, Math.min((width - 8) / 350.0, (height - 8) / 464.0)));
        left = width - s(349);
        top = height - s(464);
        for (var choice : view().choices()) {
            var button = new UiMenuButton(choice.label(), left + s(115), top + s(160), s(123), s(34), () -> choose(choice.id())).background("diplomacy/dialog/footer_button/frame_0");
            button.active = !pending;
            addRenderableWidget(button);
        }
    }
    @Override protected void choose(String choice) {
        if (pending || view().choices().stream().noneMatch(value -> value.id().equals(choice))) return;
        pending = true;
        for (var child : children()) if (child instanceof UiMenuButton button) button.active = false;
        super.choose(choice);
    }
    @Override public void onClose() {}
    @Override public void removed() { assets.close(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        assets.tile(g, "diplomacy/lease/popup_background", left, top, s(350), s(220), 64, 64, scale);
        UiText.centered(g, font, view().title(), left + s(70), top + s(26), s(210), s(44), UiMenuStyle.TEXT);
        assets.smallFlag(g, view().flag(), left + s(22), top + s(23), s(41), s(26));
        UiAssets.draw(g, "map_detail/flag_frame", left + s(18), top + s(19), s(52), s(38));
        if (dev.hoi.client.ClientCampaignHud.visible()) {
            assets.smallFlag(g, "country/" + dev.hoi.client.ClientCampaignHud.country().toLowerCase(java.util.Locale.ROOT) + "/flag", left + s(287), top + s(23), s(41), s(26));
            UiAssets.draw(g, "map_detail/flag_frame", left + s(282), top + s(19), s(52), s(38));
        }
        int x = left + s(21), y = top + s(76), w = s(308), h = s(110);
        var lines = Tooltips.split(font, Tooltips.officerText(view().body()), Math.max(1, (int)(w / UiText.scale(font))));
        int pitch = UiText.MIN_HEIGHT + 1;
        int visible = Math.min(h, s(160 - 76)) / pitch * pitch;
        maximum = Math.max(0, lines.size() * pitch - visible);
        scroll = Math.clamp(scroll, 0, maximum);
        g.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = i * pitch - scroll;
            if (lineY < 0 || lineY + UiText.MIN_HEIGHT > visible) continue;
            UiText.text(g, font, lines.get(i), x + Math.round((w - font.width(lines.get(i)) * UiText.scale(font)) / 2), y + lineY, UiMenuStyle.TEXT);
        }
        g.disableScissor();
        if (maximum > 0) {
            int barX = left + s(329), size = s(16), trackTop = y + size, travel = h - size * 3;
            String path = "commander/commands/";
            g.pose().pushMatrix(); g.pose().translate((size - s(12)) / 2f, 0);
            assets.tile(g, path + "scroll_track/frame_0", barX, trackTop, s(12), h - size * 2, 4, 4, scale);
            g.pose().popMatrix();
            UiAssets.draw(g, path + "scroll_up/frame_0", barX, y, size, size);
            UiAssets.draw(g, path + "scroll_down/frame_0", barX, y + h - size, size, size);
            UiAssets.draw(g, path + "scroll_drager/frame_0", barX, trackTop + (int)Math.round(travel * scroll / (double)maximum), size, size);
        }
        g.nextStratum();
        superWidgets(g, mx, my, delta);
    }
    private void superWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, mx, my, delta);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int)(vertical * (UiText.MIN_HEIGHT + 1) * 3), 0, maximum);
        return true;
    }
    private void moveScroll(double y) {
        int pitch = UiText.MIN_HEIGHT + 1;
        double position = (y - top - s(76 + 16) - s(8)) / Math.max(1.0, s(110) - s(16) * 3);
        scroll = Math.clamp((int)Math.round(position * maximum / pitch) * pitch, 0, maximum);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (maximum > 0 && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                && event.x() >= left + s(329) && event.x() < left + s(345)
                && event.y() >= top + s(76) && event.y() < top + s(186)) {
            if (event.y() < top + s(92)) scroll = Math.max(0, scroll - UiText.MIN_HEIGHT - 1);
            else if (event.y() >= top + s(170)) scroll = Math.min(maximum, scroll + UiText.MIN_HEIGHT + 1);
            else { dragging = true; moveScroll(event.y()); }
            return true;
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { moveScroll(event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }
}
