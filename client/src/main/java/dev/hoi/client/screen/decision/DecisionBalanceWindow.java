package dev.hoi.client.screen.decision;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.politics.PoliticsLayout;
import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.decision.DecisionPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class DecisionBalanceWindow {
    public static final int WIDTH = 550, HEIGHT = 600;
    static final int BAR_X = 95, BAR_Y = 125, BAR_WIDTH = 360, ROW_X = 13, LIST_TOP = 215, LIST_BOTTOM = 587, ROW_PITCH = 40;
    public static Consumer<DecisionPayloads.Request> requestObserver;
    private record DecisionBalanceHit(String tooltip, int x, int y, int width, int height, String sound, Runnable action) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private final DecisionText text = new DecisionText();
    private final List<DecisionBalanceHit> hits = new ArrayList<>();
    private DecisionSnapshot snapshot = new DecisionSnapshot(null);
    private Map<String,String> assets = Map.of();
    private DecisionPreferences preferences;
    private String token = UUID.randomUUID().toString(), category = "";
    private long pendingRevision = -1, marqueeTicks;
    private int ticks, scroll;
    public static UiMenuStyle.Frame frame(PoliticsLayout layout, int screenWidth) {
        float scale = (float)layout.scale();
        return new UiMenuStyle.Frame(Math.max(0, Math.min(layout.x(717), screenWidth - WIDTH * scale)), layout.y(2), scale);
    }
    public String category() { return category; }
    public List<DecisionPayloads.Row> rows() { return snapshot.rows(); }
    public void open(String nextCategory, net.minecraft.server.packs.resources.ResourceManager resources) {
        category = nextCategory == null ? "" : nextCategory; token = UUID.randomUUID().toString(); snapshot = new DecisionSnapshot(null);
        pendingRevision = -1; scroll = 0; ticks = 0; text.clear();
        var root = resources == null ? null : text.load(resources);
        try { assets = root == null ? Map.of() : DecisionPanel.sprites(root); } catch (Exception invalid) { assets = Map.of(); }
        preferences = DecisionPreferences.current();
        if (!category.isEmpty()) request(DecisionPayloads.Action.OPEN, "category:" + category, 0);
    }
    public void close() { category = ""; snapshot = new DecisionSnapshot(null); hits.clear(); }
    public boolean update(DecisionPayloads.Response packet) {
        if (category.isEmpty() || !packet.token().equals(token)) return false;
        if (packet.json().isEmpty()) { snapshot = new DecisionSnapshot(null); return true; }
        DecisionPayloads.View next;
        try { next = packet.view(); } catch (IllegalArgumentException invalid) { return true; }
        var result = snapshot.accept(next);
        if (result == DecisionSnapshot.Acceptance.STAGED) request(DecisionPayloads.Action.REFRESH, "", snapshot.nextPage());
        else if (result == DecisionSnapshot.Acceptance.APPLIED) { if (next.revision() > pendingRevision) pendingRevision = -1; text.clear(); }
        else if (result == DecisionSnapshot.Acceptance.REVOKED) snapshot = new DecisionSnapshot(null);
        return true;
    }
    private void request(DecisionPayloads.Action action, String id, int page) {
        var authority = snapshot.authority();
        if (action == DecisionPayloads.Action.TAKE) {
            if (pendingRevision >= 0 || snapshot.loading() || authority == null) return;
            pendingRevision = authority.revision();
        }
        var packet = new DecisionPayloads.Request(action, token, authority == null ? "" : authority.session(), authority == null ? 0 : authority.revision(), id, page);
        if (requestObserver != null) requestObserver.accept(packet);
        else if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(DecisionPayloads.Request.TYPE)) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);
        ticks = 0;
    }
    public void tick() {
        marqueeTicks++;
        if (category.isEmpty()) return;
        if (++ticks >= (snapshot.loading() ? 20 : 80)) {
            boolean open = snapshot.authority() == null;
            request(open ? DecisionPayloads.Action.OPEN : DecisionPayloads.Action.REFRESH, open ? "category:" + category : "", open ? 0 : snapshot.nextPage());
        }
    }
    private boolean tracking(DecisionPayloads.Row row) {
        var view = snapshot.view();
        return preferences != null && view != null && preferences.tracking(DecisionPreferences.server(), view.country(), row.id());
    }
    public void draw(GuiGraphicsExtractor g, Font font, UiMenuStyle.Frame at, MenuView.Balance balance, String barTooltip, int mx, int my) {
        hits.clear();
        g.nextStratum();
        at.tile(g, "commander/commands/tiled_plain_bg/frame_0", 0, 0, WIDTH, HEIGHT, 64, UiMenuStyle.TILE);
        at.art(g, "politics/bop/background", 5, 44);
        UiMenuStyle.title(g, font, at, "Balance of Power", 37, 16, 300, -1, UiMenuStyle.TEXT);
        cell(g, font, at, text.component(balance.name()), 120, 70, 300, true);
        at.art(g, balance.leftIcon(), 15, 84);
        at.art(g, balance.rightIcon(), 458, 84);
        double value = Math.clamp(balance.value(), -1, 1), center = BAR_X + BAR_WIDTH / 2.0, needle = BAR_X + (value + 1) / 2 * BAR_WIDTH;
        at.art(g, "bop/bar_bg", BAR_X, BAR_Y);
        g.enableScissor(at.ix(Math.min(center, needle)), at.iy(BAR_Y), at.ix(Math.max(center, needle)), at.iy(BAR_Y + 16));
        at.art(g, value < 0 ? "bop/bar_orange_light" : "bop/bar_blue_light", BAR_X, BAR_Y);
        g.disableScissor();
        at.art(g, "bop/bar_frame", BAR_X - 4, BAR_Y - 3);
        var ranges = balance.ranges();
        MenuView.Entry active = null;
        for (int i = 0; i < ranges.size(); i++) {
            double min = ranges.get(i).progress(), max = i + 1 < ranges.size() ? ranges.get(i + 1).progress() : 1;
            boolean current = value >= min && (value < max || i + 1 == ranges.size());
            if (current) active = ranges.get(i);
            double split = BAR_X + (min + 1) / 2 * BAR_WIDTH, bulb = BAR_X + ((min + max) / 2 + 1) / 2 * BAR_WIDTH;
            if (i > 0) at.art(g, "politics/bop/splitter", split - 6, BAR_Y - 4);
            at.art(g, "politics/bop/indicator/frame_" + (current ? 1 : 0), bulb - 14, BAR_Y - 21);
            hits.add(new DecisionBalanceHit(ranges.get(i).name(), at.ix(bulb - 14), at.iy(BAR_Y - 21), at.len(24), at.len(20), "", null));
        }
        at.art(g, "politics/bop/needle", needle - 12, BAR_Y - 6);
        hits.add(new DecisionBalanceHit(barTooltip, at.ix(BAR_X - 4), at.iy(BAR_Y - 3), at.len(BAR_WIDTH + 8), at.len(24), "", null));
        if (active != null) cell(g, font, at, text.component(active.name()), 120, 170, 300, true);
        drawRows(g, font, at);
    }
    public String tooltip(double mx, double my) {
        DecisionBalanceHit hovered = null;
        for (var hit : hits) if (hit.contains(mx, my) && !hit.tooltip().isEmpty() && (hovered == null || hit.width() < hovered.width())) hovered = hit;
        return hovered == null ? "" : hovered.tooltip();
    }
    public void drawTooltip(GuiGraphicsExtractor g, Font font, int mx, int my) {
        String value = tooltip(mx, my);
        if (!value.isEmpty()) Tooltips.drawSourceScrollable(g, font, value, mx, my, g.guiWidth(), g.guiHeight(), 0);
    }
    private void drawRows(GuiGraphicsExtractor g, Font font, UiMenuStyle.Frame at) {
        var rows = snapshot.rows();
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() * ROW_PITCH - (LIST_BOTTOM - LIST_TOP)));
        int top = at.iy(LIST_TOP), bottom = at.iy(LIST_BOTTOM);
        g.enableScissor(at.ix(ROW_X), top, at.ix(ROW_X + 512), bottom);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); int y = LIST_TOP + i * ROW_PITCH - scroll;
            if (y + ROW_PITCH < LIST_TOP || y > LIST_BOTTOM) continue;
            drawRow(g, font, at, row, y);
            boolean active = row.enabled() && pendingRevision < 0 && !snapshot.loading();
            int rowTop = Math.max(top, at.iy(y)), rowBottom = Math.min(bottom, at.iy(y + 39));
            if (rowBottom <= rowTop) continue;
            if (row.remainingHours() <= 0) {
                hits.add(new DecisionBalanceHit(UiCommandLabels.text(tracking(row) ? "DECISION_KEEP_NOTIFIED" : "DECISION_NOT_KEEP_NOTIFIED", ""), at.ix(ROW_X + 437), at.iy(y + 7), at.len(23), at.len(23), "ui.decision.tab",
                        () -> { var view = snapshot.view(); if (preferences != null && view != null) preferences.tracking(DecisionPreferences.server(), view.country(), row.id(), !tracking(row)); }));
                hits.add(new DecisionBalanceHit(DecisionScreen.tooltip(text, row), at.ix(ROW_X + 465), at.iy(y + 6), at.len(40), at.len(28), active ? "ui.decision.take" : "", active ? () -> request(DecisionPayloads.Action.TAKE, row.id(), 0) : null));
            }
            hits.add(new DecisionBalanceHit(DecisionScreen.tooltip(text, row), at.ix(ROW_X), rowTop, at.len(row.remainingHours() > 0 ? 512 : 437), rowBottom - rowTop, active ? "ui.decision.take" : "", active ? () -> request(DecisionPayloads.Action.TAKE, row.id(), 0) : null));
        }
        g.disableScissor();
    }
    private void drawRow(GuiGraphicsExtractor g, Font font, UiMenuStyle.Frame at, DecisionPayloads.Row row, int y) {
        boolean counting = row.remainingHours() > 0;
        at.art(g, "decision/ui/row/frame_" + (row.phase().equals("ACTIVE") ? 0 : 1), ROW_X, y);
        String image = DecisionScreen.asset(assets, row.icon(), "icon");
        if (UiAssets.imageHeight(image, 20) <= 0) image = DecisionScreen.asset(assets, "GFX_decision_unknown", "icon");
        int[] size = UiAssets.imageSize(image);
        double nameX = ROW_X + 55;
        if (size[0] > 0 && size[1] > 0) {
            double x = ROW_X + 30 - size[0] / 2.0; at.art(g, image, x, y + 19 - size[1] / 2.0); nameX = Math.max(nameX, x + size[0] + 2);
        }
        double right = ROW_X + 432, costWidth = 0;
        int costColor = Boolean.FALSE.equals(row.costMet()) ? Tooltips.BAD : Tooltips.GOLD;
        if (counting) {
            String days = (row.remainingHours() + 23) / 24 + "일";
            int[] timer = UiAssets.imageSize("decision/timer");
            at.art(g, "decision/timer", ROW_X + 492 - timer[0] * .95 / 2, y + 15 - timer[1] * .95 / 2, timer[0] * .95, timer[1] * .95);
            var sequence = Component.literal(days).withStyle(style -> style.withColor(Tooltips.GOLD)).getVisualOrderText();
            text.draw(g, font, sequence, at.ix(ROW_X + 475) - font.width(days), Math.round(UiText.centeredY(font, sequence, at.y(y + 19.5) - font.lineHeight / 2f, font.lineHeight, 1)), 0xFFFFFFFF, false);
            right = ROW_X + 475 - font.width(days) / at.scale();
        } else {
            var cost = Component.empty();
            for (var line : text.rows(DecisionScreen.rowCost(row))) TooltipKeywordIcons.decorateLines(line, false).visit((style, part) -> {
                part.codePoints().forEach(c -> cost.append(Component.literal(new String(Character.toChars(c))).withStyle(Character.isDigit(c) || ".,%+-".indexOf(c) >= 0 ? style.withColor(costColor) : style)));
                return Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            var sequence = cost.getVisualOrderText();
            text.draw(g, font, sequence, at.ix(right) - font.width(cost), Math.round(UiText.centeredY(font, sequence, at.y(y + 18) - font.lineHeight / 2f, font.lineHeight + 1, 1)), 0xFFFFFFFF, false);
            costWidth = font.width(cost) / at.scale();
            at.art(g, "decision/ui/track_decision/frame_" + (tracking(row) ? 1 : 0), ROW_X + 437, y + 7);
            at.art(g, "decision/ui/select/frame_" + (row.enabled() && pendingRevision < 0 && !snapshot.loading() ? 1 : 0), ROW_X + 465, y + 6);
        }
        double nameRight = Math.min(nameX + 330, (counting ? right : right - costWidth) - 6);
        cell(g, font, at, TooltipKeywordIcons.decorateLines(text.component(DecisionScreen.rowName(row)), false), nameX, y + 16.5, nameRight - nameX, false);
    }
    private void cell(GuiGraphicsExtractor g, Font font, UiMenuStyle.Frame at, Component value, double x, double center, double width, boolean centered) {
        if (width <= 0) return;
        int left = at.ix(x), right = at.ix(x + width), top = Math.round(at.y(center) - font.lineHeight / 2f);
        text.scrollingCell(g, font, value, left, top, Math.max(1, right - left), font.lineHeight, 0xFFFFFFFF, centered, marqueeTicks * 50);
    }
    public boolean click(double mx, double my) {
        DecisionBalanceHit chosen = null;
        for (var hit : hits) if (hit.action() != null && hit.contains(mx, my) && (chosen == null || hit.width() < chosen.width())) chosen = hit;
        if (chosen == null) return false;
        if (!chosen.sound().isEmpty()) AudioUiSounds.play(chosen.sound());
        chosen.action().run();
        return true;
    }
    public boolean contains(UiMenuStyle.Frame at, double mx, double my) {
        return mx >= at.x() && mx < at.x(WIDTH) && my >= at.y() && my < at.y(HEIGHT);
    }
    public boolean scroll(UiMenuStyle.Frame at, double mx, double my, double amount) {
        if (!contains(at, mx, my) || my < at.y(LIST_TOP)) return false;
        scroll = Math.max(0, scroll - (int)Math.round(amount * ROW_PITCH));
        return true;
    }
}
