package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CommanderNavalResultWindow extends CommanderBattleWindow {
    static final int WIDTH = 708, HEIGHT = 532, PANEL = 290;
    private static final String ART = "commander/battle/result/";
    private static final double[] PANEL_X = {-280, 698};
    private final Scroll[] lost = {new Scroll(112, 34), new Scroll(576, 34)}, details = {new Scroll(-280 + PANEL - 22, 36), new Scroll(698 + PANEL - 22, 36)};
    private Result result;
    private boolean detailed;
    CommanderNavalResultWindow(String id) { super(Kind.RESULT, id, WIDTH, HEIGHT, -354, -266); }
    Result result() { return result; }
    @Override boolean accept(Snapshot snapshot) {
        var next = snapshot.result();
        if (!next.token().equals(token())) return true;
        result = next;
        return next.error().isEmpty();
    }
    @Override boolean contains(double x, double y) {
        if (super.contains(x, y)) return true;
        if (frame != null && detailed) for (double panel : PANEL_X) if (gui(new Box(panel, 30, PANEL, HEIGHT - 5)).contains(x, y)) return true;
        return false;
    }
    @Override void render(GuiGraphicsExtractor g, Font font) {
        frame.art(g, ART + "bg", 0, 0);
        if (result == null || !result.error().isEmpty()) { closeButton(g, "menu/close", 553, 11, 32, 33, ""); return; }
        UiMenuStyle.title(g, font, frame, result.title(), 135, 26, 415, 0, UiMenuStyle.TEXT);
        closeButton(g, "menu/close", 553, 11, 32, 33, result.label("CLOSE"));
        outlined(g, font, result.outcome(), 142, 72, 420, 0, true);
        side(g, font, result.attacker(), true);
        side(g, font, result.defender(), false);
        frame.art(g, ART + "details/frame_" + (detailed ? 1 : 0), 122, 469);
        hit(122, 469, 46, 46, result.label("DETAILS"), "ui.click", () -> detailed = !detailed);
        frame.art(g, "raids/gfx_button_reset_123x34/frame_0", 294, 474);
        outlined(g, font, result.label("BUTTON_DISMISS"), 294, 491, 123, 0);
        hit(294, 474, 123, 34, result.label("BUTTON_DISMISS"), "ui.close", () -> { send(Action.DISMISS); CommanderBattleHud.close(this); });
        if (detailed) { panel(g, font, result.attacker().details(), 0); panel(g, font, result.defender().details(), 1); }
    }
    private void side(GuiGraphicsExtractor g, Font font, ResultSide side, boolean attacker) {
        flag(g, side.country(), attacker ? 43 : 624, 126, side.countryTooltip());
        frame.art(g, "map_detail/flag_frame", attacker ? 38 : 619, 122);
        if (!side.portrait().isEmpty()) {
            frame.art(g, side.portrait(), attacker ? 28 : 609, 161, 70.2, 94.5);
            frame.art(g, "commander/battle/naval/leader_frame", attacker ? 23 : 604, 154);
            hit(attacker ? 23 : 604, 154, 81, 107, side.leaderTooltip());
            traits(g, side.traits(), attacker ? 24 : 605, 263, 19, 30, 4, false);
        }
        for (int i = 0; i < side.survivors().size(); i++) survivor(g, font, side.survivors().get(i), attacker ? 140 + 34 * i : 567 - 34 * (side.survivors().size() - i));
        var scroll = lost[attacker ? 0 : 1];
        scroll.begin(g, new Box(attacker ? 130 : 356, 148, 226, 316), 34 * side.lost().size());
        for (int i = 0; i < side.lost().size(); i++) lost(g, font, side.lost().get(i), 154 + 34 * i - scroll.offset, attacker);
        scroll.end(g);
    }
    private void survivor(GuiGraphicsExtractor g, Font font, Survivor survivor, double x) {
        outlined(g, font, Integer.toString(survivor.count()), x - 9, 89, 50, 0);
        frame.art(g, survivor.image(), x, 98);
        int flags = survivor.flags().size();
        for (int i = 0; i < Math.min(2, flags); i++) smallFlag(g, survivor.flags().get(i), x + (flags == 1 ? 10 : i == 0 ? 4 : 16), 110, 10, 7);
        hit(x, 80, 34, 40, survivor.tooltip());
    }
    private void lost(GuiGraphicsExtractor g, Font font, Lost lost, double y, boolean attacker) {
        int[] size = UiAssets.imageSize(lost.image());
        double center = attacker ? 196 : 508;
        frame.art(g, lost.image(), center - size[0] / 2.0, y + 10 - size[1] / 2.0);
        hit(center - size[0] / 2.0, y, size[0], 20, lost.tooltip());
        for (int i = 0; i < lost.losses().size(); i++) {
            var loss = lost.losses().get(i);
            double x = attacker ? 250 + 25 * i : 438 - 25 * i;
            CountryFlags.draw(g, HudMenuBar.flagTexture(loss.flag()), "smallest", frame.x(x), frame.y(y), frame.size(23), frame.size(16));
            outlined(g, font, Integer.toString(loss.count()), x - 8, y + 24, 39, 0);
            hit(x, y, 25, 33, loss.tooltip());
        }
    }
    private void panel(GuiGraphicsExtractor g, Font font, String text, int index) {
        double x = PANEL_X[index];
        nine(g, "commander/commands/mini_tooltip/frame_0", x, 30, PANEL, HEIGHT - 5, 12);
        float scale = UiText.scale(font);
        var lines = Tooltips.split(font, Tooltips.panelText(text), Math.max(1, (int)(frame.size(256) / scale)));
        int step = line() + 2;
        var scroll = details[index];
        scroll.begin(g, new Box(x + 8, 40, 272, HEIGHT - 25), step * lines.size());
        for (int i = 0; i < lines.size(); i++) UiText.marquee(g, font, lines.get(i), frame.x(x + 10), frame.y(40 + step * i - scroll.offset), frame.size(260), scale, UiMenuStyle.TEXT, false, -1);
        scroll.end(g);
    }
}
