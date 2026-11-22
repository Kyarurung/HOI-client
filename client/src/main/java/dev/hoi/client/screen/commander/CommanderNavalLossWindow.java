package dev.hoi.client.screen.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.map.commander.CommanderCombatLogHud;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.country.CountryFlags;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CommanderNavalLossWindow {
    static final int X = 540, WIDTH = 495, LIST = 130, BATTLE_WIDTH = 283, ENTRY_WIDTH = 460, CHOICE = 40, MENU = 270;
    private enum Kind { HEADER, SECTION, HULL, ACCIDENT, SHIP }
    private record Item(Kind kind, String text, Section section, Hull hull, Accident accident, Ship ship, int y, int h) {}
    record Box(double x, double y, double w, double h) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    private final String token = UUID.randomUUID().toString();
    private final Set<String> expanded = new HashSet<>();
    private Losses losses;
    private UiMenuStyle.Frame frame;
    private int height, scroll, menuScroll;
    private String target = "", country = "", faction = "", menu = "";
    private boolean dragging, menuDragging;
    void open() { AudioUiSounds.play("ui.pop_up"); request(); }
    void request() { CommanderCombatLogHud.send(new Request(token, Action.NAVY, target, 0, 0, List.of(country, faction))); }
    void close() { CommanderCombatLogHud.send(new Request(token, Action.CLOSE_NAVY, "", 0, 0, List.of())); }
    boolean accept(Losses next) {
        if (!next.token().equals(token)) return true;
        if (next.error().isEmpty() && !next.target().equals(target)) return true;
        losses = next; scroll = Math.clamp(scroll, 0, maxScroll());
        return next.error().isEmpty();
    }
    Losses losses() { return losses; }
    String target() { return target; }
    UiMenuStyle.Frame frame() { return frame; }
    double bottom() { return frame.y(height); }
    int listBottom() { return LIST + 20 + listHeight(); }
    int scroll() { return scroll; }
    Set<String> expanded() { return expanded; }
    void expand(String section) { expanded.add(section); }
    void layout(UiMenuStyle.Frame overview, int width, int screenHeight) {
        float scale = overview.scale(), top = overview.y(3);
        height = Math.max(LIST + 120, (int)((screenHeight - top) / scale) - 165);
        frame = new UiMenuStyle.Frame(Math.max(0, Math.min(X * scale, width - WIDTH * scale)), top, scale);
    }
    boolean contains(double x, double y) { return frame != null && gui(new Box(0, 0, WIDTH, height)).contains(x, y); }
    private Box gui(Box box) { return new Box(frame.x(box.x()), frame.y(box.y()), frame.size(box.w()), frame.size(box.h())); }
    private String label(String key) { return losses == null ? "" : losses.label(key); }
    private boolean shown() { return losses != null && losses.error().isEmpty(); }
    private boolean shipList() { return !target.isEmpty(); }
    private int listHeight() { return height - LIST - 33; }
    private List<Item> items() {
        var result = new ArrayList<Item>(); int y = 0;
        if (shipList()) { for (var ship : losses.ships()) { result.add(new Item(Kind.SHIP, "", null, null, null, ship, y, 90)); y += 90; } return result; }
        for (boolean battles : new boolean[]{true, false}) {
            var sections = battles ? losses.battles() : losses.periods();
            if (sections.isEmpty() && battles) continue;
            result.add(new Item(Kind.HEADER, label(battles ? "NOTEWORTHY_BATTLES" : "BATTLES_OVER_TIME"), null, null, null, null, y, 24)); y += 24;
            for (var section : sections) {
                result.add(new Item(Kind.SECTION, section.title(), section, null, null, null, y, 30)); y += 30;
                if (!expanded.contains(section.id())) continue;
                for (var hull : section.hulls()) { result.add(new Item(Kind.HULL, hull.name(), section, hull, null, null, y, 69)); y += 69; }
                for (var accident : section.accidents()) { result.add(new Item(Kind.ACCIDENT, accident.name(), section, null, accident, null, y, 69)); y += 69; }
            }
            if (battles) y += 5;
        }
        return result;
    }
    private int total() { var items = items(); return items.isEmpty() ? 0 : items.getLast().y() + items.getLast().h(); }
    private int maxScroll() { return shown() ? Math.max(0, total() - listHeight()) : 0; }
    private Box box(Item item) { return new Box(0, LIST + 20 + item.y() - scroll, WIDTH - 18, item.h()); }
    private UiScrollBar bar() { return new UiScrollBar(frame.ix(WIDTH - 22), frame.iy(LIST + 20), frame.iy(LIST + 20 + listHeight()), Math.round(frame.size(listHeight())), Math.round(frame.size(total()))); }
    List<Choice> choices() {
        var result = new ArrayList<Choice>(); boolean countries = menu.equals("country");
        result.add(new Choice("", label(countries ? "ALL_COUNTRIES" : "ALL_FACTIONS"))); result.addAll(countries ? losses.countries() : losses.factions());
        return result;
    }
    private Box menuBox() { return new Box(menu.equals("country") ? 10 : 250, 115, 230, Math.min(MENU, choices().size() * CHOICE + 14)); }
    private int menuMaximum() { return Math.max(0, (int)(choices().size() * CHOICE + 14 - menuBox().h())); }
    private UiScrollBar menuBar() { var box = menuBox(); return new UiScrollBar(frame.ix(box.x() + box.w() - 20), frame.iy(box.y() + 7), frame.iy(box.y() + box.h() - 7), Math.round(frame.size(box.h() - 14)), Math.round(frame.size(choices().size() * CHOICE))); }
    private static Box closeBox() { return new Box(WIDTH - 43, 10, 32, 33); }
    private static Box backBox() { return new Box(335, 53, 123, 34); }
    private static Box dropdown(boolean countries) { return new Box(countries ? 10 : 250, 93, 230, 30); }
    void draw(GuiGraphicsExtractor g, Font font, int mx, int my) {
        if (frame == null) return;
        g.nextStratum();
        frame.tile(g, "commander/commands/tiled_plain_bg/frame_0", 0, 0, WIDTH, height, 64, 192);
        UiMenuStyle.title(g, font, frame, label("NAVAL_LOSSES"), 40, 27, 390, 0, UiMenuStyle.TEXT);
        frame.art(g, "menu/close", closeBox().x(), closeBox().y());
        frame.art(g, "commander/losses/strip", 0, 47);
        if (!shown()) return;
        frame.art(g, "commander/losses/lost/frame_0", 15, 58); text(g, font, Integer.toString(losses.lost()), 65, 60, 55, 22, -1);
        frame.art(g, "commander/losses/won/frame_0", 130, 61); text(g, font, Integer.toString(losses.sunk()), 180, 60, 55, 22, -1);
        if (shipList()) { frame.art(g, "commander/commands/button_123x34/frame_0", backBox().x(), backBox().y(), 123, 34); text(g, font, label("BACK"), backBox().x(), backBox().y(), 123, 34, 0); }
        else for (boolean countries : new boolean[]{true, false}) {
            var box = dropdown(countries); String id = countries ? country : faction;
            nine(g, "commander/commands/generic_background/frame_0", box.x(), box.y(), (int)box.w(), 30, 8);
            frame.art(g, "commander/commands/expand_button/frame_0", box.x() + 205, box.y() + 6);
            String name = (countries ? losses.countries() : losses.factions()).stream().filter(c -> c.id().equals(id)).findFirst().map(Choice::name).orElse(label(countries ? "ALL_COUNTRIES" : "ALL_FACTIONS"));
            text(g, font, name, box.x() + 10, box.y(), 190, 30, -1);
        }
        frame.tile(g, "intel/source/gfx_tiled_window2_1b_border/frame_0", 0, LIST, WIDTH, height - LIST, 64, 192);
        g.enableScissor(frame.ix(4), frame.iy(LIST + 14), frame.ix(WIDTH - 4), frame.iy(LIST + 20 + listHeight()));
        for (var item : items()) if (item.y() + item.h() >= scroll && item.y() <= scroll + listHeight()) item(g, font, item);
        g.disableScissor();
        bar().draw(g, Math.round(frame.size(scroll)));
        if (!menu.isEmpty()) menu(g, font, mx, my);
    }
    private void item(GuiGraphicsExtractor g, Font font, Item item) {
        double y = box(item).y();
        switch (item.kind()) {
            case HEADER -> { frame.art(g, "commander/losses/header", 10, y - 4); text(g, font, item.text(), 19, y, 160, 24, -1); }
            case SECTION -> {
                int icon = item.section().accidents().isEmpty() ? 0 : 1;
                frame.art(g, losses.battles().contains(item.section()) ? "commander/losses/battle" : "commander/losses/period", 13, y);
                text(g, font, item.text(), 18, y, 162, 30, -1);
                frame.art(g, "commander/losses/lost/frame_" + icon, 184, y + 8, 30, 14.4); text(g, font, Integer.toString(item.section().lost()), 214, y, 28, 30, -1);
                frame.art(g, "commander/losses/won/frame_" + icon, 242, y + 9, 30, 12.6); text(g, font, Integer.toString(item.section().sunk()), 270, y, 26, 30, -1);
            }
            case HULL -> {
                nine(g, "commander/commands/mini_tooltip/frame_0", 12, y, ENTRY_WIDTH, 69, 12);
                picture(g, item.hull().image(), 214, y + 42, .7, 210, 60);
                text(g, font, item.text(), 24, y + 3, 220, 20, -1);
                flag(g, losses.viewer(), 29, y + 33);
                text(g, font, item.hull().lost() == 0 ? "0" : "§R" + item.hull().lost() + "§!", 41, y + 27, 45, 20, -1);
                for (int i = 0; i < item.hull().kills().size(); i++) {
                    var kill = item.hull().kills().get(i); double line = y + 6 + i * 14, w = font.width(kill.count()) * UiText.scale(font) / frame.scale();
                    if (!kill.country().isEmpty()) flag(g, kill.country(), 427 - w, line + 6);
                    text(g, font, kill.count(), 394, line, 45, 20, 1);
                }
            }
            case ACCIDENT -> {
                frame.art(g, "commander/losses/ship_bg", 11, y);
                picture(g, item.accident().image(), 164, y + 42, .7, 150, 60);
                text(g, font, item.text(), 24, y + 3, 220, 20, -1);
                frame.art(g, "commander/losses/lost/frame_1", 8, y + 28, 40, 19.2); text(g, font, item.accident().trainingSunk(), 50, y + 25, 45, 20, -1); text(g, font, item.accident().trainingDamage(), 239, y + 25, 45, 20, 1);
                frame.art(g, "commander/losses/won/frame_1", 8, y + 48, 40, 16.8); text(g, font, item.accident().minesSunk(), 50, y + 45, 45, 20, -1); text(g, font, item.accident().minesDamage(), 239, y + 45, 45, 20, 1);
            }
            case SHIP -> {
                nine(g, "commander/commands/mini_bg/frame_0", 12, y, ENTRY_WIDTH, 90, 12);
                shield(g, item.ship().owner(), 24, y + 25);
                text(g, font, item.ship().name(), 27, y + 3, 266, 20, -1);
                picture(g, item.ship().image(), 234, y + 50, 1, 260, 60);
                frame.art(g, "commander/commands/killed_units_icon/frame_0", 404, y + 26);
                shield(g, item.ship().killer(), 424, y + 25);
            }
        }
    }
    private void menu(GuiGraphicsExtractor g, Font font, int mx, int my) {
        var box = menuBox(); var choices = choices();
        g.nextStratum();
        frame.tile(g, "commander/commands/tiled_window_1b_thin_border/frame_0", box.x(), box.y(), (int)box.w(), (int)box.h(), 64, 192);
        g.enableScissor(frame.ix(box.x()), frame.iy(box.y() + 7), frame.ix(box.x() + box.w()), frame.iy(box.y() + box.h() - 7));
        for (int i = 0; i < choices.size(); i++) {
            double y = box.y() + 7 + i * CHOICE - menuScroll;
            if (y + CHOICE < box.y() || y > box.y() + box.h()) continue;
            nine(g, "commander/commands/mini_tooltip/frame_0", box.x() + 5, y, 200, CHOICE, 12);
            boolean flag = menu.equals("country") && !choices.get(i).id().isEmpty();
            if (flag) flag(g, choices.get(i).id(), box.x() + 14, y + 13);
            text(g, font, choices.get(i).name(), box.x() + (flag ? 38 : 14), y, flag ? 160 : 184, CHOICE, -1);
        }
        g.disableScissor();
        menuBar().draw(g, Math.round(frame.size(menuScroll)));
    }
    private void flag(GuiGraphicsExtractor g, String tag, double x, double y) { frame.art(g, HudMenuBar.flagTexture(tag), x, y, 10, 7); }
    private void shield(GuiGraphicsExtractor g, String tag, double x, double y) { CountryFlags.draw(g, HudMenuBar.flagTexture(tag), "small", frame.x(x + 3), frame.y(y + 2), frame.size(20), frame.size(17)); }
    private void picture(GuiGraphicsExtractor g, String image, double cx, double cy, double scale, double maxWidth, double maxHeight) {
        int[] size = UiAssets.imageSize(image);
        if (size[0] <= 0 || size[1] <= 0) return;
        double fit = Math.min(scale, Math.min(maxWidth / size[0], maxHeight / size[1])), w = size[0] * fit, h = size[1] * fit;
        frame.art(g, image, cx - w / 2, cy - h / 2, w, h);
    }
    private void nine(GuiGraphicsExtractor g, String path, double x, double y, int w, int h, int border) {
        g.pose().pushMatrix();
        g.pose().translate(frame.x(x), frame.y(y));
        g.pose().scale(frame.scale());
        UiAssets.nineSlice(g, path, 0, 0, w, h, border, 1);
        g.pose().popMatrix();
    }
    private void text(GuiGraphicsExtractor g, Font font, String value, double x, double y, double w, double h, int align) {
        if (value == null || value.isEmpty()) return;
        var line = net.minecraft.locale.Language.getInstance().getVisualOrder(Tooltips.panelText(value));
        float scale = UiText.scale(font);
        UiText.marquee(g, font, line, frame.x(x), UiText.centeredY(font, line, frame.y(y), frame.size(h), scale), frame.size(w), scale, UiMenuStyle.TEXT, false, align);
    }
    String tooltip(double mx, double my) { return TooltipDelay.visible(source(mx, my)); }
    String source(double mx, double my) {
        if (frame == null || !contains(mx, my)) return "";
        if (gui(closeBox()).contains(mx, my)) return label("CLOSE");
        if (!shown() || !menu.isEmpty() && gui(menuBox()).contains(mx, my)) return "";
        if (gui(new Box(15, 56, 105, 26)).contains(mx, my)) return label("TOTAL_SHIPS_LOST");
        if (gui(new Box(130, 56, 105, 26)).contains(mx, my)) return label("TOTAL_SHIPS_SUNK");
        if (shipList()) { if (gui(backBox()).contains(mx, my)) return label("BACK"); }
        else for (boolean countries : new boolean[]{true, false}) if (gui(dropdown(countries)).contains(mx, my)) return label(countries ? "SHIPS_LOST_FILTER_COUNTRY" : "SHIPS_LOST_FILTER_FACTION");
        if (!gui(new Box(0, LIST + 14, WIDTH - 22, listHeight() + 6)).contains(mx, my)) return "";
        for (var item : items()) {
            if (!gui(box(item)).contains(mx, my)) continue;
            double y = box(item).y();
            return switch (item.kind()) {
                case HULL -> item.hull().tooltip();
                case ACCIDENT -> item.accident().tooltip();
                case SHIP -> item.ship().tooltip();
                case SECTION -> gui(new Box(184, y, 58, 30)).contains(mx, my) ? label("OUR_LOSSES") : gui(new Box(242, y, 54, 30)).contains(mx, my) ? label("OUR_KILLS") : "";
                case HEADER -> "";
            };
        }
        return "";
    }
    boolean mouseClicked(MouseButtonEvent event, Runnable closer) {
        double mx = event.x(), my = event.y();
        if (!contains(mx, my) && (menu.isEmpty() || !gui(menuBox()).contains(mx, my))) { menu = ""; return false; }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (!menu.isEmpty()) {
            boolean inside = gui(menuBox()).contains(mx, my);
            if (inside && menuMaximum() > 0 && menuBar().contains(mx, my)) { if (menuBar().arrow(my)) menuScroll = Math.clamp(menuScroll + (my < menuBar().top() + 8 ? -CHOICE : CHOICE), 0, menuMaximum()); else { menuDragging = true; menuDrag(my); } return true; }
            if (inside) {
                int index = (int)Math.floor((my - frame.y(menuBox().y() + 7)) / frame.scale() + menuScroll) / CHOICE;
                if (index >= 0 && index < choices().size()) { AudioUiSounds.play("ui.click"); String id = choices().get(index).id(); if (menu.equals("country")) country = id; else faction = id; menu = ""; scroll = 0; request(); }
                return true;
            }
            menu = "";
        }
        if (gui(closeBox()).contains(mx, my)) { AudioUiSounds.play("ui.close"); closer.run(); return true; }
        if (!shown()) return true;
        if (shipList()) { if (gui(backBox()).contains(mx, my)) { AudioUiSounds.play("ui.click"); target = ""; scroll = 0; request(); return true; } }
        else for (boolean countries : new boolean[]{true, false}) if (gui(dropdown(countries)).contains(mx, my)) { AudioUiSounds.play("ui.click"); menu = countries ? "country" : "faction"; menuScroll = 0; return true; }
        if (maxScroll() > 0 && bar().contains(mx, my)) {
            if (bar().arrow(my)) scroll = Math.clamp(scroll + (my < frame.y(LIST + 20) + 8 ? -40 : 40), 0, maxScroll());
            else { dragging = true; drag(my); }
            return true;
        }
        if (!gui(new Box(0, LIST + 14, WIDTH - 22, listHeight() + 6)).contains(mx, my)) return true;
        for (var item : items()) {
            if (!gui(box(item)).contains(mx, my)) continue;
            if (item.kind() == Kind.SECTION && gui(new Box(13, box(item).y(), BATTLE_WIDTH, 30)).contains(mx, my)) { AudioUiSounds.play("ui.click"); if (!expanded.remove(item.section().id())) expanded.add(item.section().id()); scroll = Math.clamp(scroll, 0, maxScroll()); }
            else if (item.kind() == Kind.HULL && item.hull().tooltip().contains(dev.hoi.protocol.ProtocolTooltip.DELAYED)) { AudioUiSounds.play("ui.click"); target = item.section().id() + "/" + item.hull().key(); scroll = 0; request(); }
            return true;
        }
        return true;
    }
    private void drag(double my) { scroll = Math.clamp(Math.round(bar().offset(my) / frame.scale()), 0, maxScroll()); }
    private void menuDrag(double my) { menuScroll = Math.clamp(Math.round(menuBar().offset(my) / frame.scale()), 0, menuMaximum()); }
    boolean mouseDragged(MouseButtonEvent event) {
        if (menuDragging) { menuDrag(event.y()); return true; }
        if (!dragging) return false;
        drag(event.y());
        return true;
    }
    boolean mouseReleased() { boolean held = dragging || menuDragging; dragging = menuDragging = false; return held; }
    boolean mouseScrolled(double x, double y, double vertical) {
        if (!menu.isEmpty() && gui(menuBox()).contains(x, y)) { menuScroll = Math.clamp(menuScroll - (int)Math.round(vertical * CHOICE), 0, menuMaximum()); return true; }
        if (!contains(x, y)) return false;
        scroll = Math.clamp(scroll - (int)Math.round(vertical * 40), 0, maxScroll());
        return true;
    }
    boolean closeMenu() { if (menu.isEmpty()) return false; menu = ""; return true; }
}
