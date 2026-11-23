package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiUnits;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderHudPayloads.Member;
import dev.hoi.protocol.commander.CommanderShipPayloads.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class CommanderShipRows {
    static final double TOP = 61, PITCH = 20.5;
    private static String force = "", token = "";
    private static Force data;
    private CommanderShipRows() {}
    static void track(String id) {
        if (id.equals(force)) return;
        if (!force.isEmpty()) CommanderShipHud.send(new Request(token, Action.CLOSE_LIST, force));
        force = id; data = null;
        if (id.isEmpty()) return;
        token = UUID.randomUUID().toString();
        CommanderShipHud.send(new Request(token, Action.LIST, id));
    }
    static void accept(Force next) { if (next.token().equals(token) && next.force().equals(force)) data = next; }
    static void reset() { force = ""; data = null; }
    static List<Row> rows(String id) { return data == null || !id.equals(force) ? List.of() : data.ships(); }
    static void draw(GuiGraphicsExtractor g, UiUnits u, List<CommanderPanels.Hit> out, Member member, Row ship, int x, float y, int color, Runnable select) {
        var frame = new UiMenuStyle.Frame(x, y, u.scale() * .5f);
        frame.art(g, "commander/commands/naval_unit_ship_entry/frame_0", 0, 0, 493, 47);
        int top = (int)Math.floor(y);
        g.pose().pushMatrix();
        g.pose().translate(0, y - top);
        CommanderPanels.verticalProgress(g, "navieslist_bar_org", x + u.length(3.5), top + u.size(3), u.size(3), u.size(14), ship.organization());
        CommanderPanels.verticalProgress(g, "navieslist_bar_str", x + u.length(7.5), top + u.size(3), u.size(3), u.size(14), ship.strength());
        CommanderPanels.verticalProgress(g, "unit_xp_bar", x + u.length(72), top + u.size(3), u.size(3), u.size(14), ship.progress());
        g.pose().popMatrix();
        int[] size = UiAssets.imageSize(ship.icon());
        double iconY = 25 - size[1] / 2.0;
        frame.art(g, ship.icon(), 25, iconY);
        frame.art(g, "commander/commands/unit_level/frame_" + ship.level(), 109, 8, 27, 27);
        String activity = member.data() == null || member.data().navy() == null ? "" : member.data().navy().activityIcon();
        if (!activity.isEmpty()) frame.art(g, "commander/commands/" + activity + "/frame_0", 154, 11, 24, 24);
        UiAssets.drawTinted(g, member.icon(), frame.ix(450), frame.iy(8), u.size(16.5), u.size(16), color);
        text(g, frame, ship.name(), 3);
        text(g, frame, ship.type(), 21);
        out.add(hit(frame, 23, 2, 84, 43, data.label("CLICK_NAVY_SEE_DETAILS"), () -> CommanderShipHud.open(ship.id()), "ui.click", "ship_details/" + ship.id()));
        out.add(hit(frame, 5, 4, 9, 34, ship.organizationTooltip(), select, "", "ship_organization/" + ship.id()));
        out.add(hit(frame, 14, 4, 9, 34, ship.strengthTooltip(), select, "", "ship_strength/" + ship.id()));
        out.add(hit(frame, 0, 0, 493, 41, ship.tooltip(), select, (button, twice) -> {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) select.run();
            else if (button == InputConstants.MOUSE_BUTTON_RIGHT) CommanderShipHud.send(new Request(UUID.randomUUID().toString(), Action.FOCUS, ship.id()));
        }, "ui.fleet.select", "ship_row/" + ship.id()));
    }
    private static void text(GuiGraphicsExtractor g, UiMenuStyle.Frame frame, String value, double y) {
        var font = Minecraft.getInstance().font;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.officerText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(184), UiText.centeredY(font, line, frame.y(y), frame.size(18), scale), frame.size(262), scale, UiMenuStyle.TEXT, false, -1);
    }
    private static CommanderPanels.Hit hit(UiMenuStyle.Frame frame, double x, double y, double w, double h, String tip, Runnable action, String sound, String icon) {
        return hit(frame, x, y, w, h, tip, action, null, sound, icon);
    }
    private static CommanderPanels.Hit hit(UiMenuStyle.Frame frame, double x, double y, double w, double h, String tip, Runnable action, java.util.function.BiConsumer<Integer, Boolean> gesture, String sound, String icon) {
        int left = frame.ix(x), top = frame.iy(y);
        return new CommanderPanels.Hit(left, top, frame.ix(x + w) - left, frame.iy(y + h) - top, tip, action, gesture, sound, icon);
    }
}
